package io.github.swqxdba.bridge.meta

import java.lang.reflect.Type

data class TypeInfo(
    val javaType: Type,
    //唯一的标识
    val identity: String = BridgeUtil.getIdentity(javaType),
    val members: MutableMap<String, PropertyMeta> = mutableMapOf(),

    val selfMembers: MutableMap<String, PropertyMeta> = mutableMapOf(),

    var comment: MutableList<String> = mutableListOf(),

) {

    var commentResolved: Boolean = false
    /**
     * @param atParentType 是否在父类中
     */
    fun addProperty(propertyName: String, propertyTypeInfo: PropertyMeta, atParentType: Boolean) {
        if (atParentType) {
            selfMembers[propertyName] = propertyTypeInfo
        }
        members[propertyName] = propertyTypeInfo
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeInfo) return false

        if (identity != other.identity) return false

        return true
    }

    override fun hashCode(): Int {
        return identity.hashCode()
    }

    fun isCollection(): Boolean {
        if (javaType !is Class<*>) {
            return false
        }
        return Collection::class.java.isAssignableFrom(javaType)
    }

    fun toRawType(): TypeInfo? {
        return BridgeUtil.resolveRawType(javaType)?.let { TypeHolder.registerType(it) }
    }

    fun superTypeInfo(): TypeInfo? {
        val toRawType = BridgeUtil.resolveRawType(this.javaType)
        if (toRawType != null && toRawType.genericSuperclass != null) {
            val superTypeInfo = toRawType.genericSuperclass.toTypeInfo()
            return superTypeInfo
        }
        return null
    }

    fun isSpecialSuperType(): Boolean {
        return javaType == Any::class.java || javaType == Object::class.java || javaType == Enum::class.java
    }

}
