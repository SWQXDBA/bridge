package io.github.swqxdba.bridge.codegen.typescript.meta

import io.github.swqxdba.bridge.codegen.typescript.TypeScriptTypeConverter
import io.github.swqxdba.bridge.docreader.DocHelper
import io.github.swqxdba.bridge.meta.BridgeGlobalConfig
import io.github.swqxdba.bridge.meta.PropertyMeta

class TsPropertyMeta(propertyMeta: PropertyMeta) {
    val readOnly = propertyMeta.readOnly

    val name = BridgeGlobalConfig.propertyNameConverter
        ?.convert(propertyMeta.propertyName, propertyMeta.getter.declaringClass)
        ?:propertyMeta.propertyName

    //可能是具体的类型 也可能是泛型的T T[],等等
    val typeString: String?

    val propertyTsType: TsTypeMeta

    val comment: String// 注释

    init {
        val javaType = propertyMeta.typeInfo.javaType
        typeString = TypeScriptTypeConverter.convert(propertyMeta.typeInfo, false)
        propertyTsType = TsTypeMeta.create(propertyMeta.typeInfo)
        comment = DocHelper.recoverComment(propertyMeta.comment)
    }


}