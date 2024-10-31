package io.github.swqxdba.bridge.codegen.typescript.meta

import io.github.swqxdba.bridge.codegen.typescript.TsGenGlobalConfig
import io.github.swqxdba.bridge.codegen.typescript.TsUtil
import io.github.swqxdba.bridge.codegen.typescript.TypeScriptTypeConverter
import io.github.swqxdba.bridge.docreader.DocHelper
import io.github.swqxdba.bridge.meta.BridgeUtil
import io.github.swqxdba.bridge.meta.ControllerMeta
import io.github.swqxdba.bridge.meta.TypeHolder
import io.github.swqxdba.bridge.meta.toTypeInfo
import org.apache.logging.log4j.LogManager
import java.util.PriorityQueue

/**
 * 代表一个controller
 */
class TsApiMeta(
    controllerMeta: ControllerMeta,
    val typeDirImportName: String
) {
    val clientClassName: String//用于创建文件夹

    val clientDefineString: String//用于声明客户端类型定义

    val methods: PriorityQueue<TsMethodMeta>

    var types: MutableSet<TsTypeMeta>

    val comment: String//注释

    //可能有多个类型指向同一个Class 比如Response<String>和Response<Integer>都要导入Response 所以进行去重
    val importTypes: Set<TsTypeMeta>
        get() {
            return types.distinctBy {
                it.typeRowString
            }.toSortedSet()
        }

    val logger = LogManager.getLogger(TsApiMeta::class.java)

    fun deepCollectTypes(): Set<TsTypeMeta> {
        val result = mutableSetOf<TsTypeMeta>()
        val current = mutableSetOf<TsTypeMeta>()
        result.addAll(types)
        current.addAll(types)


        //类似于层序遍历树状结构
        while (current.isNotEmpty()) {
            val currentAll = current.toList()
            current.clear()
            for (tsTypeMeta in currentAll) {
                for (type in tsTypeMeta.types) {
                    if (result.contains(type)) {
                        continue
                    } else {
                        result.add(type)
                        current.add(type)
                    }
                }
            }
        }

        logger.info("deps for ${clientClassName} is ${result.joinToString { it.typeRowString!! }}")
        //去除忽略的类型 不需要作为参数传递的类型就不需要导入
        result.removeIf {
            it.toRawTypeInfo()?.let { rawType ->
                val clazz = rawType.typeInfo.javaType as Class<*>
                TsGenGlobalConfig.ignoreParamTypes.contains(clazz) ||
                        TsGenGlobalConfig.ignoreParamParentTypes.any { superClass ->
                            superClass.isAssignableFrom(clazz)
                        }
            } ?: false
        }
        return result

    }

    init {
        logger.info("init api meta for ${controllerMeta.controllerClass.name}")

        //添加注释
        comment = DocHelper.recoverComment(controllerMeta.comment)

        clientClassName = TsUtil.getRawName(controllerMeta.controllerClass)!!
            .replace("Controller", "Api")

        clientDefineString = clientClassName
//        clientDefineString = TypeScriptTypeConverter.convert(controllerMeta.controllerClass.toTypeInfo(),true)!!
//            .replace("Controller", "Api")


        val pathOnController = controllerMeta.pathOnController
        val apiList = controllerMeta.apiList

        //类型信息
        types = apiList.flatMap { methodMeta ->
            val returnType = TsTypeMeta.create(methodMeta.returnType)
            val paramTypes = methodMeta.params.map {
                TsTypeMeta.create(it.paramType)
            }
            return@flatMap paramTypes + returnType
        }.flatMap {
            //转换成真正需要定义的
            val javaTypesToDefine = TsUtil.getDeepNeedDefineTypes(it.typeInfo.javaType)
            javaTypesToDefine.map {
                TsTypeMeta.create(it.toTypeInfo())
            }
        }.filter {type->
            val rawType = BridgeUtil.resolveRawType(type.typeInfo.javaType)?:return@filter true
            !(TsGenGlobalConfig.ignoreParamTypes.contains(rawType) ||
                    TsGenGlobalConfig.ignoreParamParentTypes.any { superClass ->
                        superClass.isAssignableFrom(rawType)
                    })

        }.toMutableSet()



        types.forEach {
            logger.info("find dependency type ${it.typeRowString} for api ${this.clientClassName}")
        }


        //构建方法信息
        methods = apiList.map { methodMeta ->
            val methodName = methodMeta.controllerMethodName
            //转成小写的http方法
            val httpMethodString = methodMeta.httpMethod.toString().lowercase()

            //合并controller和方法上的路径
            var url = BridgeUtil.resolvePath(arrayOf(pathOnController, methodMeta.pathOnMethod))

            val paramMetas = methodMeta.params.toMutableList()

            paramMetas.removeIf {
                val rawType = BridgeUtil.resolveRawType(it.paramType.javaType) ?: return@removeIf false

                return@removeIf TsGenGlobalConfig.ignoreParamTypes.contains(rawType) ||
                        TsGenGlobalConfig.ignoreParamParentTypes.any { superClass ->
                            superClass.isAssignableFrom(rawType)
                        }
            }

            val pathParams = mutableMapOf<String, String>()//路径中的变量名，方法中的变量名
            paramMetas.forEach { paramMeta ->
                paramMeta.pathParamName?.let {
                    pathParams[it] = paramMeta.paramName
                }
            }

            //处理位于路径上的参数 比如 /user/{id} 变成`/user/${id}`
            if (pathParams.isNotEmpty()) {
                pathParams.forEach { (nameOnPath, nameOnParam) ->
                    url = url.replace("{$nameOnPath}", "${'$'}{$nameOnParam}")
                }
                url = "`$url`"//有参数就用反引号作为字符串模板
            } else {
                url = "'$url'"
            }


            //存在@RequestBody注解的参数
            val contentType = methodMeta.contentType


            //请求体参数字符串 如 {id,name,age}
            var bodyParam = "null"
            if (paramMetas.isNotEmpty()) {
                //用来寻找是否有基础类型
                //比如如果是id:Long,name:String这种就可以{id,name}进行传递
                //如果是person:PersonDto这种bean类型 就只能直接传递 person本身而不是{person}
                var hasBaseType = false

                //非路径上的参数都作为body 用逗号拼接
                bodyParam = paramMetas.filter {

                    it.pathParamName == null

                }
                    .map {
                        //这个map有个副作用
                        hasBaseType = hasBaseType || TypeHolder.baseTypes.contains(it.paramType)

                        it.paramName
                    }
                    .joinToString(",")

                bodyParam = if (bodyParam.isEmpty()) {
                    "null"
                } else {
                    //这里的处理其实有问题 因为可能有多个参数，但是只有一个是@RequestBody的
                    //实际上这里默认有@RequestBody时 有且只有一个参数
                    if (contentType=="application/json" || !hasBaseType) {
                        bodyParam
                    } else {
                        "{$bodyParam}"
                    }
                }

            }


            //拼接参数字符串
            val paramsString = paramMetas.map {
                it.paramName + ":" + TypeScriptTypeConverter.convert(it.paramType, false)
            }.joinToString(",")

            return@map TsMethodMeta(
                url,
                methodName,
                httpMethodString,
                paramsString,
                TypeScriptTypeConverter.convert(methodMeta.returnType, false) ?: "",
                bodyParam,
                contentType,
                DocHelper.recoverComment(methodMeta.comment)
            )
        }.let {
            PriorityQueue(it)
        }


    }

}