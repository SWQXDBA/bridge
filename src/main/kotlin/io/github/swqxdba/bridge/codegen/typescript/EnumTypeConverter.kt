package io.github.swqxdba.bridge.codegen.typescript

import io.github.swqxdba.bridge.meta.NameKind
import java.util.function.Function

interface EnumTypeConverter {
    /**
     * 解析枚举类
     * @return 枚举常量和值的映射关系
     */
    fun doConvert(clazz: Class<out Enum<*>>):Map<String,Any> ;

    /**
     * 使用枚举名称本身
     */
    object NameStringConverter:EnumTypeConverter{
        override fun doConvert(clazz: Class<out Enum<*>>): Map<String, Any> {
            val result = mutableMapOf<String, Any>()
            for (enumConstant in clazz.enumConstants) {
                result[enumConstant.name] = enumConstant.name
            }
            return result
        }
    }

    /**
     *  使用枚举顺序
     */
    object OrdinalConverter:EnumTypeConverter{
        override fun doConvert(clazz: Class<out Enum<*>>): Map<String, Any> {
            val result = mutableMapOf<String, Any>()
            for (enumConstant in clazz.enumConstants) {
                result[enumConstant.name] = enumConstant.ordinal
            }
            return result
        }
    }

    /**
     * 使用桥接的转换器转换
     */
    class TransformConverter(val transformer:Function<Enum<*>,Any>):EnumTypeConverter{
        override fun doConvert(clazz: Class<out Enum<*>>): Map<String, Any> {
            val result = mutableMapOf<String, Any>()
            for (enumConstant in clazz.enumConstants) {
                result[enumConstant.name] = transformer.apply(enumConstant)
            }
            return result
        }
    }
}

fun main() {
    EnumTypeConverter.NameStringConverter.doConvert(NameKind::class.java)
}