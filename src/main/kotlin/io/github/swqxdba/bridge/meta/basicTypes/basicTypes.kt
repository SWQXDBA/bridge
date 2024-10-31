package io.github.swqxdba.bridge.meta.basicTypes

import io.github.swqxdba.bridge.meta.TypeInfo
import java.math.BigDecimal


object Types {

    val _void = TypeInfo(
        Void::class.javaPrimitiveType!!,
    )

    val _Void = TypeInfo(
        Void::class.java,
    )

    val _Object = TypeInfo(
        Object::class.java,
    )

    val _boolean = TypeInfo(
        Boolean::class.javaPrimitiveType!!,
    )

    val _Boolean = TypeInfo(
        Boolean::class.javaObjectType,

        )

    val _byte: TypeInfo = TypeInfo(
        Byte::class.javaPrimitiveType!!,

        )

    val _Byte: TypeInfo = TypeInfo(
        Byte::class.javaObjectType,

        )

    val _short: TypeInfo = TypeInfo(
        Short::class.javaPrimitiveType!!,
    )

    val _Short: TypeInfo = TypeInfo(
        Short::class.javaObjectType,
    )

    val _int: TypeInfo = TypeInfo(
        Int::class.javaPrimitiveType!!,
    )

    val _Int: TypeInfo = TypeInfo(
        Int::class.javaObjectType,
    )

    val _long: TypeInfo = TypeInfo(
        Long::class.javaPrimitiveType!!,
    )

    val _Long: TypeInfo = TypeInfo(
        Long::class.javaObjectType,
    )

    val _float: TypeInfo = TypeInfo(
        Float::class.javaPrimitiveType!!,
    )

    val _Float: TypeInfo = TypeInfo(
        Float::class.javaObjectType,
    )

    val _double: TypeInfo = TypeInfo(
        Double::class.javaPrimitiveType!!,
    )

    val _Double: TypeInfo = TypeInfo(
        Double::class.javaObjectType,
    )

    val _char: TypeInfo = TypeInfo(
        Char::class.javaPrimitiveType!!,
    )

    val _Char: TypeInfo = TypeInfo(
        Char::class.javaObjectType,
    )

    val _BigDecimal: TypeInfo =
        TypeInfo(BigDecimal::class.javaObjectType)

    val _String = TypeInfo(String::class.javaObjectType)

    val utilDate = TypeInfo(java.util.Date::class.javaObjectType)

    val sqlDate: TypeInfo = TypeInfo(
        java.sql.Date::class.javaObjectType,
    )

    val sqlTime: TypeInfo = TypeInfo(
        java.sql.Time::class.javaObjectType,
    )

    val sqlTimestamp: TypeInfo = TypeInfo(
        java.sql.Timestamp::class.javaObjectType,
    )

    val localDate: TypeInfo = TypeInfo(
        java.time.LocalDate::class.javaObjectType,
    )

    val localTime: TypeInfo = TypeInfo(
        java.time.LocalTime::class.javaObjectType,
    )

    val localDateTime: TypeInfo = TypeInfo(
        java.time.LocalDateTime::class.javaObjectType,
    )

    val instant: TypeInfo = TypeInfo(
        java.time.Instant::class.javaObjectType,
    )

    val zonedDateTime: TypeInfo = TypeInfo(
        java.time.ZonedDateTime::class.javaObjectType,
    )
}