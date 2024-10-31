package io.github.swqxdba.bridge.codegen.typescript.meta

import io.github.swqxdba.bridge.codegen.typescript.TsGenGlobalConfig
import io.github.swqxdba.bridge.codegen.typescript.TsUtil
import io.github.swqxdba.bridge.codegen.typescript.TypeScriptTypeConverter
import io.github.swqxdba.bridge.docreader.DocHelper
import io.github.swqxdba.bridge.meta.BridgeUtil
import io.github.swqxdba.bridge.meta.TypeHolder
import io.github.swqxdba.bridge.meta.TypeInfo
import io.github.swqxdba.bridge.meta.basicTypes.Types
import io.github.swqxdba.bridge.meta.toTypeInfo
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable


/**
 * 代表一个类型
 */
data class TsTypeMeta private constructor(var typeInfo: TypeInfo) : Comparable<TsTypeMeta> {

    val logger = LogManager.getLogger()

    companion object {

        private val cache = mutableMapOf<TypeInfo, TsTypeMeta>()
        fun create(typeInfo: TypeInfo): TsTypeMeta {
            if (!cache.containsKey(typeInfo)) {
                cache[typeInfo] = TsTypeMeta(typeInfo)
            }
            return cache[typeInfo]!!
        }
    }

    val comment: String//注释

    //如Response<String>
    var typeString: String

    //如Response<T> 用于生成定义
    var typeDefineString: String? = null

    //如Response 用于import和生成定义文件
    var typeRowString: String? = null

    //成员变量
    var members = mutableListOf<TsPropertyMeta>()

    //需要导入的类型
    var needImportTypes = mutableSetOf<String>()

    var types: MutableSet<TsTypeMeta> = mutableSetOf()//依赖的其他类型

    var defineTypeModifier: String = "interface"

    var areEnum = false

    //枚举类型的成员
    var enumMemberLines: MutableList<String> = mutableListOf()

    //扩展类型字符串 如 "extends Base"
    var extendStr: String = ""

    init {
        cache[typeInfo] = this//避免栈溢出

        comment = DocHelper.recoverComment(typeInfo.comment)
        typeString = TypeScriptTypeConverter.convert(typeInfo, false)!!
        typeDefineString = TypeScriptTypeConverter.convert(typeInfo, true)
        typeRowString = BridgeUtil.resolveRawType(typeInfo.javaType)?.simpleName

        resolveMembers()

        resolveEnum()


        if (TsGenGlobalConfig.useExtendToGenType) {
            val superTypeInfo = typeInfo.superTypeInfo()
            if (superTypeInfo != null) {
                //这里先create再判断是不是Object 确保类型被注册
                val superType = create(superTypeInfo)
                if (!superType.typeInfo.isSpecialSuperType()) {
                    val extendType = superType.typeString
                    types.add(superType)
                    superType.typeRowString?.let {
                        needImportTypes.add(it)
                    }
                    extendStr = "extends $extendType"


                    //自动判断是否需要导入
                    val addDependentType = { classType: Type ->
                        val toTypeInfo = classType.toTypeInfo()
                        val tsTypeMeta = create(toTypeInfo)
                        //如 Data0<Data1> 此时加入Data0
                        if (!toTypeInfo.isSpecialSuperType()) {
                            tsTypeMeta.typeRowString?.let {
                                needImportTypes.add(it)
                            }
                            types.add(tsTypeMeta)
                        }
                    }

                    //解析要导入的类型
                    BridgeUtil.getNeedImportTypes(superTypeInfo.javaType).forEach {
                        addDependentType(it)
                    }
                }

            }
        }
        //不要导入自己
        types.removeIf {
            it.typeInfo == this.typeInfo
        }
        needImportTypes.removeIf { it == typeRowString }

        //移除导入Enum
        if (areEnum) {
            types.removeIf {
                it.typeInfo == Enum::class.java.toTypeInfo()
            }
            needImportTypes.removeIf {
                it == "Enum"
            }
            extendStr = ""
        }
    }


    //处理枚举类型
    private fun resolveEnum() {
        val toRawType = typeInfo.toRawType()

        if (toRawType != null) {
            val javaType = toRawType.javaType
            if (javaType is Class<*> && javaType.isEnum) {
                defineTypeModifier = "enum"
                areEnum = true
                val enumMap = TsGenGlobalConfig.enumConvert.doConvert(javaType as Class<Enum<*>>)
                enumMemberLines = enumMap.map {
                    if (it.key == it.value) {
                        it.key
                    } else {
                        "${it.key} = ${it.value}"
                    }
                }.toMutableList()
            }
        }
    }

    //处理成员变量 和需要导入的类型
    private fun resolveMembers() {
        //这里要替换成RawType才会有成员 比如Response<String>是没有成员的 Response本身才有
        val toRawType = typeInfo.toRawType()
        val rawTypeMembers = if (TsGenGlobalConfig.useExtendToGenType) {
            toRawType?.selfMembers
        } else {
            toRawType?.members
        }
        if (rawTypeMembers != null) {
            for (member in rawTypeMembers) {
                val propertyInfo = member.value
                if (propertyInfo.typeInfo == Types._void) {
                    continue
                }
                create(propertyInfo.typeInfo)
                members.add(TsPropertyMeta(propertyInfo))

                if (TypeHolder.baseTypes.contains(propertyInfo.typeInfo)) {
                    continue
                }
                //你不能导入一个<T>
                if (propertyInfo.typeInfo.javaType is TypeVariable<*>) {
                    continue
                }
                val deepNeedDefineTypes = TsUtil.getDeepNeedDefineTypes(propertyInfo.typeInfo.javaType)
                needImportTypes.addAll(
                    deepNeedDefineTypes
                        .mapNotNull {
                            TsUtil.getRawName(it)
                        })
                types.addAll(TsUtil.getDeepNeedDefineTypes(propertyInfo.typeInfo.javaType).map {
                    create(it.toTypeInfo())
                })
            }
        }

        types.addAll(TsUtil.getDeepNeedDefineTypes(typeInfo.javaType).map {
            create(it.toTypeInfo())
        })

        for (type in types) {
            logger.info("find dependency property type of ${type.typeString} for ${this.typeString}")
        }

        //移除自身
        needImportTypes.removeIf {
            it == TsUtil.getRawName(typeInfo.javaType)
        }

        members.sortedBy { it.name }
    }

    /**
     * 转换成用于定义的类型 如Response<Person> => Response<T>  因为成员只记录在javaType为Class的类型中
     */
    fun toRawTypeInfo(): TsTypeMeta? {

        val javaType = typeInfo.javaType

        return BridgeUtil.resolveRawType(javaType)?.let {
            create(TypeHolder.registerType(it))
        }
    }

    override fun compareTo(other: TsTypeMeta): Int {
        return this.typeString.compareTo(other.typeString)
    }
}



