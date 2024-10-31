package io.github.swqxdba.bridge.meta

import org.springframework.web.bind.annotation.RequestMethod
import java.lang.reflect.Method

open class ControllerMeta(
    var className:String,
    var controllerClass:Class<*>,
    var pathOnController:String,
    var apiList:List<ApiMeta>
){
    var comment:MutableList<String> = mutableListOf()
}
open class ApiMeta (
    var method:Method,
    var pathOnMethod:String,
    var httpMethod: RequestMethod,
    var controllerMethodName:String,
    var params:List<ParamMeta>,
    var returnType: TypeInfo,
    var contentType:String,
){
    var comment:MutableList<String> = mutableListOf()
}
open class ParamMeta(
    var paramName:String,
    var paramType: TypeInfo,
    var pathParamName:String?,
    var requestBodyParam:Boolean,
)

//注意这个typeinfo不一定是这个属性的type 因为可能经过BridgeGlobalConfig.propertyTypeConverter转换过
//所以应该读getter的返回类型
open class PropertyMeta (val propertyName:String, val typeInfo:TypeInfo,val readOnly:Boolean,val getter:Method){
    var comment:MutableList<String> = mutableListOf()
    val realTypeInfo  get() = getter.genericReturnType.toTypeInfo()
}
