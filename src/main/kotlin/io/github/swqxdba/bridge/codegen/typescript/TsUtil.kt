package io.github.swqxdba.bridge.codegen.typescript


import io.github.swqxdba.bridge.meta.BridgeUtil
import io.github.swqxdba.bridge.meta.TypeHolder
import io.github.swqxdba.bridge.meta.TypeInfo
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

object TsUtil {
    fun needDefineTypeFile(javaType: Type): Boolean {
        if (TypeHolder.baseTypes.contains(TypeHolder.registerType(javaType))) {
            return false
        }

        val rawType = BridgeUtil.resolveRawType(javaType)
            ?: return false//不知道是什么类型 就不用生成定义文件了

        if (rawType.isArray) {
            return false
        }
        if (Collection::class.java.isAssignableFrom(rawType)) {
            return false
        }

        if (Map::class.java.isAssignableFrom(rawType)) {
            return false
        }
        return true;
    }

    /** 递归获取需要定义的所有类型
     *
     * !!!但是不包含成员属性中需要引入的类型!!!
     *
     * 如Response<SchoolDto> 此时有Response和SchoolDto两个类型需要定义
     */
    fun getDeepNeedDefineTypes(javaType: Type): Set<Type> {
        val types = mutableSetOf<Type>()
        if (needDefineTypeFile(javaType)) {
            types.add(javaType)
        }
        if (javaType is Class<*>) {
            if (javaType.isArray) {
                types.addAll(getDeepNeedDefineTypes(javaType.componentType))
            }
        }
        if (javaType is ParameterizedType) {
            if (needDefineTypeFile(javaType.rawType)) {
                types.addAll(getDeepNeedDefineTypes(javaType.rawType))
            }
            for (actualTypeArgument in javaType.actualTypeArguments) {
                types.addAll(getDeepNeedDefineTypes(actualTypeArgument))
            }
        }
        return types

    }

    fun getRawName(javaType: Type): String? {
        return BridgeUtil.resolveRawType(javaType)?.simpleName
    }

    fun getRawName(typeInfo: TypeInfo): String? {
        return BridgeUtil.resolveRawType(typeInfo.javaType)?.simpleName
    }
}
