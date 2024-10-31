package io.github.swqxdba.bridge.meta

/**
 * 用于覆写注释
 * 某些情况下，无法通过正则正确地找到注释，可以用该注解在字段或者类上添加该注解，来指定注释
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION,AnnotationTarget.CLASS,AnnotationTarget.FIELD)
annotation class ApiComment (val value:String)