package io.github.swqxdba.bridge.codegen.typescript.meta

/**
 * 代表controller中的一个方法
 */
class TsMethodMeta(
    val url: String,//自带引号
    val methodName: String,//不带引号
    val httpMethod: String,//不带引号
    val params: String,//不带引号
    val returnType: String,//不带引号
    val bodyParam: String,//不带引号
    val contentType: String,//不带引号
    val comment:String//注释

):Comparable<TsMethodMeta> {
    override fun compareTo(other: TsMethodMeta): Int {
        return this.methodName.compareTo(other.methodName)
    }
}