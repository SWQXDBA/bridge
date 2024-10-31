package io.github.swqxdba.bridge.meta

import io.github.swqxdba.bridge.meta.basicTypes.Types
import org.springframework.beans.BeanUtils
import java.beans.PropertyDescriptor
import java.lang.RuntimeException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable


object TypeHolder {
    //确保通过字符串或者类型本身都能找到

    //之所以要用identity一个字符串是因为两个实际上相同的类型可能是不同的对象，
    //比如两个方法都返回了 List<String>，但是genericReturnType却不是同一个对象
    val identityTypeMap = mutableMapOf<String, TypeInfo>()

    val typeTypeMap = mutableMapOf<Type, TypeInfo>()

    val baseTypes = mutableSetOf<TypeInfo>()

    init {
        fun addBaseType(typeInfo: TypeInfo) {
            baseTypes.add(typeInfo)
        }
        addBaseType(Types._String)
        addBaseType(Types._Double)
        addBaseType(Types._double)
        addBaseType(Types._void)
        addBaseType(Types._Void)
        addBaseType(Types._Object)
        addBaseType(Types._int)
        addBaseType(Types._Int)
        addBaseType(Types._byte)
        addBaseType(Types._Byte)
        addBaseType(Types._long)
        addBaseType(Types._Long)
        addBaseType(Types._float)
        addBaseType(Types._Float)
        addBaseType(Types._BigDecimal)
        addBaseType(Types._boolean)
        addBaseType(Types._Boolean)
        addBaseType(Types._Double)
        addBaseType(Types._Double)
        addBaseType(Types._short)
        addBaseType(Types._Short)
        addBaseType(Types._char)
        addBaseType(Types._Char)
        addBaseType(Types.utilDate)
        addBaseType(Types.sqlDate)
        addBaseType(Types.sqlTime)
        addBaseType(Types.sqlTimestamp)
        addBaseType(Types.localDate)
        addBaseType(Types.localTime)
        addBaseType(Types.localDateTime)
        addBaseType(Types.instant)
        addBaseType(Types.zonedDateTime)


        baseTypes.forEach { addType(it) }
    }


    private fun addType(typeInfo: TypeInfo) {
        identityTypeMap[typeInfo.identity] = typeInfo
        typeTypeMap[typeInfo.javaType] = typeInfo
    }

    private fun tryGeyType(javaType: Type): TypeInfo? {
        return typeTypeMap[javaType] ?: identityTypeMap[BridgeUtil.getIdentity(javaType)]
    }

    /**
     * 注册一个类型 同时会递归注册它的成员变量的类型
     */
    fun registerType(javaType: Type): TypeInfo {
        val identity = BridgeUtil.getIdentity(javaType)

        // 检查是否已存在对应的 TypeInfo
        val existingTypeInfo = tryGeyType(javaType)
        if (existingTypeInfo != null) {
            return existingTypeInfo
        }


        val typeInfo = TypeInfo(javaType, identity)
        addType(typeInfo)
        if (javaType is Class<*>) {

            val propertyDescriptors = BeanUtils.getPropertyDescriptors(javaType)
            for (propertyDescriptor in propertyDescriptors) {
                val propertyName = propertyDescriptor.name
                val propertyType = propertyDescriptor.propertyType

                if (propertyName == "class" || propertyName == "declaringClass" || propertyType == null) {
                    continue
                }
                if (propertyDescriptor.readMethod == null) {
                    continue
                }

                val isSelfProperty =
                    run {
                        try {
                            javaType.getDeclaredMethod(propertyDescriptor.readMethod.name)
                        }catch (e:NoSuchMethodException) {
                           return@run false
                        }
                        true
                    }
                val propertyInfo = resolveProperty(propertyDescriptor)
                typeInfo.addProperty(propertyName, propertyInfo,isSelfProperty)
            }
        }
        if (javaType is ParameterizedType) {
            val rawType = javaType.rawType
            registerType(rawType)
        }
        //泛型声明 如T
        if (javaType is TypeVariable<*>) {
            println()
        }

        return typeInfo
    }

    fun resolveProperty(propertyDescriptor: PropertyDescriptor): PropertyMeta {
        //这个javaType是带泛型的Type 而不是Class
        val propertyType: Type? =
            propertyDescriptor.readMethod?.genericReturnType
                ?: propertyDescriptor.writeMethod?.genericParameterTypes!![0]
        propertyType ?: throw RuntimeException("未能解析到属性类型")
        var registerType = registerType(propertyType)
        BridgeGlobalConfig.propertyTypeConverter?.let {
            val convertedType =
                it.convert(propertyType, propertyDescriptor.readMethod, propertyDescriptor.readMethod.declaringClass)
            registerType = registerType(convertedType)
        }
        return PropertyMeta(propertyDescriptor.name, registerType,propertyDescriptor.writeMethod==null,propertyDescriptor.readMethod)
    }


}

fun Type.toTypeInfo(): TypeInfo {
    return TypeHolder.registerType(this)
}