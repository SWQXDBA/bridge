package io.github.swqxdba.bridge.meta

import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.lang.Exception
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.function.Predicate

enum class NameKind {

    Raw,//最外层的类型 如Response<String>的Response Data[]的Data
    Simple, //简单的类型名如Response<String> 用于声明和定义变量类型
    Class,//全限定名 如com.bridge.Response<String> 用于区分同名类型
}

object BridgeUtil {

    val logger by lazy { LogManager.getLogger(BridgeUtil::class.java) }


    /**
     * 从baseClass开始扫描包和子包下的所有controller
     */
    @JvmOverloads
    @JvmStatic
    fun scan(baseClass: Class<*>, predicate: Predicate<String>? = null): List<ControllerMeta> {
        val controllers = findAllControllers(baseClass, predicate)
        logger.info("Resolve Controller Meta Start")
        val result = controllers.map { resolve(it) }
        logger.info("Resolve Controller Meta End")
        return result
    }

    /**
     * 从baseClass开始扫描包和子包下的所有controller
     */
    @JvmOverloads
    @JvmStatic
    fun findAllControllers(baseClass: Class<*>, predicate: Predicate<String>? = null): List<Class<*>> {
        logger.info("FindAllControllers Start")
        val packageName = baseClass.`package`?.name ?: ""
        if (packageName.isEmpty()) {
            throw Exception("必须要在一个包中!")
        }
        val controllerClasses = mutableListOf<Class<*>>()


        for (className in PackageClassResolver.resolveClasses(packageName)) {

            try {
                if (predicate != null && !predicate.test(className)) {
                    continue
                }
                if (BridgeGlobalConfig.enableDetailLog) {
                    logger.info("try resolve controller for class: $className")
                }
                val clazz = Class.forName(className)

                if (!isControllerClass(clazz) || clazz.isAnnotationPresent(ApiIgnore::class.java)) {
                    continue
                }
                controllerClasses.add(clazz)
                CommentResolver.resolveCommentForType(clazz.toTypeInfo())//注释解析


            } catch (e: ClassNotFoundException) {
                // 处理类加载异常
                if (BridgeGlobalConfig.enableDetailLog) {
                    logger.error("ClassNotFoundException!!!", e)
                }

            }
        }
        logger.info("FindAllControllers End")
        return controllerClasses
    }

    private fun isControllerClass(clazz: Class<*>): Boolean {
        return clazz.isAnnotationPresent(Controller::class.java) || clazz.isAnnotationPresent(RestController::class.java)
    }


    fun resolve(controller: Class<*>): ControllerMeta {

        val requestMappingOnController: RequestMapping? = controller.getDeclaredAnnotation(RequestMapping::class.java)

        val controllerPath: Array<String> = requestMappingOnController?.value ?: emptyArray()

        val apiMetas = mutableListOf<ApiMeta>()
        for (declaredMethod in controller.declaredMethods) {
            if (declaredMethod.isAnnotationPresent(ApiIgnore::class.java)) {
                continue
            }
            for (annotation in declaredMethod.annotations) {
                val requestMappingPair = tryGetMappingAnnotation(annotation) ?: continue
                val httpMethod = requestMappingPair.second
                val paths = requestMappingPair.first
                val consume = requestMappingPair.third
                var contentType = if (consume.isEmpty()) {
                    "application/x-www-form-urlencoded"
                } else {
                    consume[0]
                }

                val paramMetaList = declaredMethod.parameters.map {
                    var parameterizedType = it.parameterizedType
                    BridgeGlobalConfig.methodParamTypeConverter?.let { converter ->
                        parameterizedType = converter.convert(it)
                    }
                    val pathVariable = it.getDeclaredAnnotation(PathVariable::class.java)
                    if (pathVariable != null) {
                        var paramOnPathName = pathVariable.value
                        if (paramOnPathName.isEmpty()) {
                            paramOnPathName = pathVariable.name
                        }
                        if (paramOnPathName.isEmpty()) {
                            paramOnPathName = it.name
                        }
                        return@map ParamMeta(it.name, parameterizedType.toTypeInfo(), paramOnPathName, false)
                    }
                    val requestBody = it.getDeclaredAnnotation(RequestBody::class.java)
                    if (requestBody != null) {
                        contentType = "application/json"
                        return@map ParamMeta(it.name, parameterizedType.toTypeInfo(), null, true)
                    }
                    return@map ParamMeta(it.name, parameterizedType.toTypeInfo(), null, false)
                }
                var returnTypeInfo: TypeInfo = declaredMethod.genericReturnType.toTypeInfo()
                BridgeGlobalConfig.methodReturnTypeConverter?.let {
                    returnTypeInfo =
                        it.convert(declaredMethod.genericReturnType, declaredMethod, controller).toTypeInfo()
                }


                apiMetas.add(
                    ApiMeta(
                        declaredMethod,
                        resolvePath(paths),
                        resolveHttpMethod(httpMethod),
                        declaredMethod.name,
                        paramMetaList,
                        returnTypeInfo,
                        contentType
                    )
                )

            }
        }


        val controllerMeta = ControllerMeta(controller.name, controller, resolvePath(controllerPath), apiMetas)
        val sourceCodeDir = BridgeGlobalConfig.sourceCodeDir
        if (sourceCodeDir != null) {
            CommentResolver.resolveComment(controllerMeta)
        }

        return controllerMeta
    }


    fun resolvePath(paths: Array<String>): String {
        var pathToString = "/" + paths.joinToString("/")
        while (pathToString.indexOf("//") >= 0) {
            pathToString = pathToString.replace("//", "/")
        }
        return pathToString
    }

    fun resolveHttpMethod(methods: Array<RequestMethod>): RequestMethod {
        if (methods.isEmpty()) {
            return RequestMethod.GET
        }
        return methods[0]
    }

    /**
     * paths:请求路径 methods:支持的http method consumes: 返回的content-type
     * @return Triple<paths,methods,consumes>
     */
    fun tryGetMappingAnnotation(annotation: Annotation): Triple<Array<String>, Array<RequestMethod>, Array<String>>? {
        if (annotation is RequestMapping) {
            return Triple(annotation.path + annotation.value, annotation.method, annotation.consumes)
        }
        //只做两层读取，看看该注解上有没有RequestMapping注解。
        val requestMapping = annotation.annotationClass.java.getAnnotation(RequestMapping::class.java) ?: return null

        var paths: Array<String> = emptyArray()
        var consumes: Array<String> = emptyArray()
        when (annotation) {
            is PostMapping -> {
                paths = annotation.path + annotation.value
                consumes = annotation.consumes
            }

            is GetMapping -> {
                paths = annotation.path + annotation.value
                consumes = annotation.consumes
            }

            is DeleteMapping -> {
                paths = annotation.path + annotation.value
                consumes = annotation.consumes
            }

            is PutMapping -> {
                paths = annotation.path + annotation.value
                consumes = annotation.consumes
            }
        }

        return Triple(requestMapping.path + requestMapping.value + paths, requestMapping.method, consumes)
    }


    private fun doGetName(javaType: Type, kind: NameKind): String {
        return when (javaType) {
            is Class<*> -> {
                when (kind) {
                    NameKind.Raw -> {
                        javaType.simpleName
                    }

                    NameKind.Simple -> {
                        if (javaType.isArray) {
                            doGetName(javaType.componentType, NameKind.Simple) + "[]"
                        } else {
                            if (javaType.typeParameters?.isEmpty() == true) {
                                javaType.simpleName
                            } else {
                                "${javaType.simpleName}<${javaType.typeParameters.map { it.name }.joinToString(",")}>"
                            }

                        }
                    }

                    NameKind.Class -> {
                        javaType.name
                    }
                }
            }

            is ParameterizedType -> {
                val rawType = javaType.rawType as? Class<*>
                    ?: throw IllegalArgumentException("Invalid raw type for ParameterizedType: $javaType")

                return when (kind) {
                    NameKind.Raw -> doGetName(rawType, NameKind.Raw)

                    NameKind.Simple, NameKind.Class -> {
                        val typeArguments = javaType.actualTypeArguments.map {
                            doGetName(it, kind)
                        }
                        val typeArgumentsString = typeArguments.joinToString(", ")

                        "${doGetName(rawType, kind)}<$typeArgumentsString>"
                    }
                }
            }

            is TypeVariable<*> -> {
                javaType.name
            }

            is WildcardType -> {
                javaType.typeName
            }


            else -> "Unsupported type: $javaType"
        }
    }

    fun getIdentity(javaType: Type): String {
        return doGetName(javaType, NameKind.Class)
    }

    fun resolveRawType(javaType: Type): Class<*>? {
        if (javaType is Class<*>) {
            return javaType
        }

        if (javaType is ParameterizedType) {
            return resolveRawType(javaType.rawType)
        }
        return null
    }

    //比如 extends Data0<Data1<Data2>> 则需要导入Data0 Data1 Data2
    fun getNeedImportTypes(type: Type): Set<Class<*>> {
        if (type is Class<*>) {
            return setOf(type)
        }
        if (type is ParameterizedType) {
            val result = mutableSetOf<Class<*>>()
            result.add(type.rawType as Class<*>)
            type.actualTypeArguments.forEach {
                result += getNeedImportTypes(it)
            }
            return result
        }
        return emptySet()
    }

}

