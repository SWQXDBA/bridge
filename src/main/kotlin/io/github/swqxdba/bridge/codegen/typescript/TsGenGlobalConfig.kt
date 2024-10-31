package io.github.swqxdba.bridge.codegen.typescript

class TsGenGlobalConfig {
    companion object {
        /**
         * 枚举转换器
         */
        @JvmStatic
        var enumConvert: EnumTypeConverter = EnumTypeConverter.NameStringConverter

        /**
         * 解析方法参数时要忽略的类型 如ServletRequest 这种类型本身也可能不会被解析
         */
        @JvmStatic
        var ignoreParamTypes = mutableSetOf<Class<*>>()

        /**
         * 解析方法参数时要忽略的类型父类 其中类型的子类都会被忽略（也可以用接口） 这种类型本身也可能不会被解析
         */
        @JvmStatic
        var ignoreParamParentTypes = mutableSetOf<Class<*>>()

        /**
         * 使用继承 而不是把父类属性生成到子类
         */
        @JvmStatic
        var useExtendToGenType = true


        /**
         * 是否把Map转换为ES6的Map
         * 如果为true 那么会把Map转换为ES6的Map
         * 如果为false 那么会把Map转换为{[key:string]:any}这种形式
         */
        @JvmStatic
        var resolveMapToEs6Map = false

        /**
         * 是否把LinkedHashMap转换为数组
         * 如果为true 那么会把LinkedHashMap转换为数组+元组
         * 比如: LinkedHashMap<String,Int> => [string,number][]
         *
         * 如果为false 则当做普通的Map处理
         */
        @JvmStatic
        var resolveLinkedHashMapToArrayEntry = true


    }

}