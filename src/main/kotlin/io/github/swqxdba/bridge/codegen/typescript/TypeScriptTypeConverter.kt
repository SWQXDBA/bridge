package io.github.swqxdba.bridge.codegen.typescript

import io.github.swqxdba.bridge.meta.*
import io.github.swqxdba.bridge.meta.basicTypes.Types

import java.lang.Exception
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

object TsTypeStrings {
    const val number = "number"

    const val void = "void"

    const val string = "string"

    const val any = "any"

    const val _object = "object"

    const val boolean = "boolean"

    const val _array = "[]"
}

object TypeScriptTypeConverter {


    /**
     * 只有当forDefine==false时有可能返回null
     * @param forDefine 是否用于定义 如类型为Response<String> 那么  true=> Response<String> false=> Response<T>
     */
    fun convert(typeInfo: TypeInfo, forDefine: Boolean): String? {

        return when (typeInfo) {
            Types._void, Types._Void -> {
                TsTypeStrings.void
            }

            Types._byte, Types._Byte, Types._char, Types._Char,
            Types._Int, Types._int, Types._short, Types._Short,
            Types._long, Types._Long, Types._double, Types._Double,
            Types._float, Types._Float -> {
                TsTypeStrings.number
            }

            Types._boolean, Types._Boolean -> {
                TsTypeStrings.boolean
            }

            Types.instant, Types.sqlDate, Types.localDate, Types.localDateTime, Types.utilDate, Types.zonedDateTime -> {
                TsTypeStrings.string
            }

            Types._String -> {
                TsTypeStrings.string
            }

            Types._BigDecimal -> {
                TsTypeStrings.string
            }

            Types._Object -> {
                TsTypeStrings._object
            }

            else -> convertOther(typeInfo, forDefine)
        }
    }

    fun convertOther(typeInfo: TypeInfo, forDefine: Boolean): String? {
        val javaType = typeInfo.javaType

        if (forDefine && !TsUtil.needDefineTypeFile(javaType)) {
            return null
        }

        if (javaType is Class<*>) {
            return if (forDefine) {
                if (javaType.typeParameters.isEmpty()) {
                    javaType.simpleName
                } else {
                    //需要一个递归
                    javaType.simpleName + "<" + javaType.typeParameters.joinToString(",") {
                        convert(TypeHolder.registerType(it), false)!! //注意用false
                    } + ">"
                }

            } else {
                if (javaType.isArray) {
                    convert(javaType.componentType.toTypeInfo(), false) + "[]"
                } else {
                    //如List 没有泛型实参 不然就不会是Class而是ParameterizedType了
                    if (typeInfo.isCollection()) {
                        return "${TsTypeStrings.any}[]"
                    } else {
                        if (javaType.typeParameters.isEmpty()) {
                            javaType.simpleName
                        } else {
                            //ts中不允许没有泛型实参 因此不能用Response 而是 Response<Any>
                            javaType.simpleName + "<${javaType.typeParameters.joinToString { TsTypeStrings.any }}>"
                        }

                    }
                }
            }
        }

        //Response<String>等 带有泛型实参的type
        if (javaType is ParameterizedType) {
            val rawType = javaType.rawType

            if (forDefine) {
                //如Response<String> 此时只需要定义Response即可 因此递归就行
                return convert(rawType.toTypeInfo(), true)
            }

            //not for forDefine
            if (rawType is Class<*>) {
                //如果是集合
                if (Collection::class.java.isAssignableFrom(rawType)) {
                    return convert(
                        TypeHolder.registerType(
                            javaType.actualTypeArguments[0]!!
                        ), false
                    ) + "[]"
                } else {
                    //其他泛型类型
                    val genericStrings =
                        //比如Response<String>
                        javaType.actualTypeArguments
                            .joinToString(",") {
                                convert(
                                    it.toTypeInfo(), false
                                )!!
                            }
                    if (Map::class.java.isAssignableFrom(rawType)) {

                        //LinkedHashMap<String,Int> => [string,number][]
                        return if (LinkedHashMap::class.java.isAssignableFrom(rawType) && TsGenGlobalConfig.resolveLinkedHashMapToArrayEntry) {
                            "[$genericStrings][]"
                        } else if (TsGenGlobalConfig.resolveMapToEs6Map) {
                            "Map" + "<$genericStrings>"
                        } else {
                            val split = genericStrings.split(",")
                            val keyType = javaType.actualTypeArguments[0]
                            if(keyType is Class<*>){
                                if(keyType.isEnum){
                                    "{[key in ${split[0]}] : ${split[1]}}"
                                }else{
                                    "{[key : ${split[0]}] : ${split[1]}}"
                                }
                            }else{
                                "{[key : ${split[0]}] : ${split[1]}}"
                            }
                        }
                    } else {
                        return rawType.simpleName + "<$genericStrings>"
                    }

                }
            } else {
                throw Exception("rowType not class!!!")
            }
        }
        //比如 <T>
        if (javaType is TypeVariable<*>) {
            return javaType.name
        }
        if(javaType is WildcardType){
            return javaType.typeName
        }
        throw Exception("not support type !!! $typeInfo")
    }


}