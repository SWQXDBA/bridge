package io.github.swqxdba.bridge.meta

import io.github.swqxdba.bridge.docreader.AnnotationFirstDocDeaderWrapper
import io.github.swqxdba.bridge.docreader.DocReader
import io.github.swqxdba.bridge.docreader.JavaDocReader
import io.github.swqxdba.bridge.docreader.KotlinDocReader
import org.apache.logging.log4j.LogManager
import java.io.File
import java.lang.Exception
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.util.function.Predicate

object CommentResolver {

    val logger by lazy { LogManager.getLogger(CommentResolver::class.java) }

    //遍历源码时的文件缓存
    private val fileCache = mutableMapOf<String, File>()

    //文件是否已经初始化
    private var fileInited = false

    init {
        //构建文件缓存
        val sourceCodeDir = BridgeGlobalConfig.sourceCodeDir
        if (sourceCodeDir != null) {
            if (!fileInited) {
                File(sourceCodeDir).walk().filter {
                    it.name.endsWith(".java") || it.name.endsWith(".kt")
                }.forEach {
                    fileCache[it.name] = it
                }
                fileInited = true
            }
        }
    }


    private fun findFile(predicate: Predicate<String>): File? {
        fileCache.keys.forEach {
            if (predicate.test(it)) {
                return fileCache[it]
            }
        }
        return null
    }

    private fun docReaderOf(filename: String): DocReader {
        val rawReader = if (filename.endsWith(".java")) {
            JavaDocReader
        } else if (filename.endsWith(".kt")) {
            return KotlinDocReader
        } else {
            throw RuntimeException("不支持的文件类型")
        }
        return AnnotationFirstDocDeaderWrapper(rawReader)

    }

    fun resolveComment(controllerMeta: ControllerMeta) {

        val comment = controllerMeta.comment
        val className = controllerMeta.controllerClass.simpleName
        val sourceCodeDir = BridgeGlobalConfig.sourceCodeDir ?: return


        //find file end with $className.java

        val file = findFile {
            it == "$className.java" || it == "$className.kt"
        } ?: return


        val docReader = docReaderOf(file.name)
        val sourceCode = file.readText()
        comment.addAll(docReader.readClassDoc(sourceCode, className, controllerMeta.controllerClass))
        for (apiMeta in controllerMeta.apiList) {
            if (BridgeGlobalConfig.enableDetailLog) {
                logger.info("为${controllerMeta.controllerClass.typeName} 解析注释")
            }
            val declaringClass = apiMeta.method.declaringClass
            val declaringClassName = declaringClass.simpleName
            val methodFile =  findFile {
                it == "$declaringClassName.java" || it == "$declaringClassName.kt"
            }
            if(methodFile!=null){
                apiMeta.comment.addAll(
                    docReader.readMethodDoc(
                        methodFile.readText(),
                        declaringClassName,
                        apiMeta.controllerMethodName,
                        apiMeta.method
                    )
                )
            }


            for (param in apiMeta.params) {
                val typeInfo = param.paramType
                resolveCommentForType(typeInfo)
                typeInfo.toRawType()?.let { resolveCommentForType(it) }
            }
            resolveCommentForType(apiMeta.returnType)
            apiMeta.returnType.toRawType()?.let { resolveCommentForType(it) }
        }


    }

    fun resolveCommentForType(typeInfo: TypeInfo) {
        BridgeGlobalConfig.sourceCodeDir ?: return

        val resolveRawType = BridgeUtil.resolveRawType(typeInfo.javaType) ?: return
        if (resolveRawType.isArray) {
            return
        }
        if (typeInfo.isSpecialSuperType() || TypeHolder.baseTypes.contains(typeInfo) || typeInfo.isCollection()) {
            return
        }
        if (typeInfo.commentResolved) {
            return
        }
        if (typeInfo.isSpecialSuperType()) {
            return
        }
        val packageName = typeInfo.javaType.javaClass.`package`?.name
        if (packageName == null || packageName.startsWith("java.")) {
            return
        }
        if (BridgeGlobalConfig.enableDetailLog) {
            logger.info("为${typeInfo.javaType.typeName} 解析注释")
        }


        //标记已经解析过
        typeInfo.commentResolved = true

        //解析父类的注释 和父类泛型中的注释
        if (typeInfo.javaType is Class<*>) {
            val genericSuperclass = typeInfo.javaType.genericSuperclass
            if (genericSuperclass != null) {
                resolveCommentForType(genericSuperclass.toTypeInfo())
                val needImportTypes = BridgeUtil.getNeedImportTypes(genericSuperclass)
                for (needImportType in needImportTypes) {
                    resolveCommentForType(needImportType.toTypeInfo())
                }
            }

        }
        //如果是泛型类型 则需要解析泛型中的注释
        if (typeInfo.javaType is ParameterizedType) {
            resolveCommentForType(typeInfo.javaType.rawType.toTypeInfo())
            for (actualTypeArgument in typeInfo.javaType.actualTypeArguments) {
                resolveCommentForType(actualTypeArgument.toTypeInfo())
            }
        }

        val declareClass = resolveRawType.declaringClass
        //处理内部类的情况
        val typeClassName = if (declareClass == null) {
            resolveRawType.simpleName
        } else {
            declareClass.simpleName
        } ?: return


        val file = findFile {
            it == ("$typeClassName.java") || it == ("$typeClassName.kt")
        } ?: return
        val docReader = docReaderOf(file.name)


        val sourceCode = file.readText()

        //class上的注释
        typeInfo.comment.addAll(
            docReader.readClassDoc(
                sourceCode,
                resolveRawType.simpleName,
                resolveRawType
            )
        )

        if (BridgeGlobalConfig.enableDetailLog) {
            logger.info("准备完成,开始为${typeInfo.javaType.typeName} 解析注释")
        }


        //给成员属性添加注释
        for (selfMember in typeInfo.selfMembers) {
            val memberMeta = selfMember.value
            val fieldTypName = BridgeUtil.resolveRawType(memberMeta.realTypeInfo.javaType)?.simpleName ?: continue
            val ownerJavaClass = typeInfo.toRawType()?.javaType as? Class<*>
            if (memberMeta.comment.isEmpty()) {
                var field: Field? = null

                try {
                    field = ownerJavaClass?.getDeclaredField(selfMember.key)
                } catch (ignore: Exception) {

                }

                memberMeta.comment.addAll(
                    docReader.readFieldDoc(
                        sourceCode,
                        resolveRawType.simpleName,
                        selfMember.key,
                        fieldTypName,
                        field
                    )
                )

                //解析getter方法上的注释
                memberMeta.comment.addAll(
                    docReader.readMethodDoc(
                        sourceCode,
                        resolveRawType.simpleName,
                        memberMeta.getter.name,
                        memberMeta.getter
                    )
                )
            }


            //给成员属性的类型添加注释
            val superTypeInfo = memberMeta.realTypeInfo.superTypeInfo() ?: continue
            if (TypeHolder.baseTypes.contains(superTypeInfo)
                || superTypeInfo.isCollection()
                || superTypeInfo.isSpecialSuperType()
            ) {
                continue
            }
            //已经解析过
            if (memberMeta.realTypeInfo.comment.isNotEmpty()) {
                continue
            }
            resolveCommentForType(superTypeInfo)
        }

    }
}