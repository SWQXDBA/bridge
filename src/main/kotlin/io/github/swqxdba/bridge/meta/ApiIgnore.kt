package io.github.swqxdba.bridge.meta

/**
 * 标识一个方法 或者controller类中的api生成被忽略
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION,AnnotationTarget.CLASS)
annotation class ApiIgnore
