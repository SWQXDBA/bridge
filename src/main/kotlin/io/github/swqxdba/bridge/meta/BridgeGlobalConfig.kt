package io.github.swqxdba.bridge.meta

import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.Type

object BridgeGlobalConfig {
    @JvmStatic
    //源码目录 如果源码目录不为空 则会尝试扫描源码给生成的字段添加注释
    var sourceCodeDir: String? = null

    @JvmStatic
    //属性类型转换器
    var propertyTypeConverter: PropertyTypeConverter? = null

    @JvmStatic
    //属性名称转换器
    var propertyNameConverter: PropertyNameConverter? = null

    @JvmStatic
    //属性类型转换器
    var methodParamTypeConverter: MethodParamTypeConverter? = null

    @JvmStatic
    //属性类型转换器
    var methodReturnTypeConverter: MethodReturnTypeConverter? = null

    /**
     * 是否启用详细日志 启用后可能会降低性能
     */
    @JvmStatic
    var enableDetailLog: Boolean = false


}


/**
 * 属性类型转换器 用于对成员属性的类型进行替换
 */
interface PropertyTypeConverter {
    /**
     * @param propertyType 属性的类型
     * @param getter 属性的getter方法
     * @param ownerClass 属性所在的类
     */
    fun convert(propertyType: Type, getter: Method, ownerClass: Class<*>): Type
}

/**
 * 属性名称转换器 比如需要根据一些注解信息来重命名属性 如JsonProperty
 */
interface PropertyNameConverter {
    /**
     * @param propertyName 属性的名称
     * @param ownerClass 属性所在的类
     */
    fun convert(propertyName: String, ownerClass: Class<*>): String
}

/**
 * 属性类型转换器 用于对方法参数类型进行转换
 */
interface MethodParamTypeConverter {
    /**
     * @param param 方法参数
     */
    fun convert(param: Parameter): Type
}

/**
 * 属性类型转换器 用于对方法返回值类型进行转换
 */
interface MethodReturnTypeConverter {
    /**
     * @param returnType 属性的类型
     * @param method 属性的getter方法
     * @param ownerClass 属性所在的类
     */
    fun convert(returnType: Type, method: Method, ownerClass: Class<*>): Type
}
