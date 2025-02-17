package io.github.swqxdba.bridge.codegen.typescript

import java.util.function.Function

interface EnumConstantsGenerator {

    fun getConstants(clazz: Class<out Enum<*>>): List<EnumConstant>

    companion object {
        @JvmStatic
        @JvmOverloads
        fun createOptionGenerator(
            labelFieldGetter: Function<in Enum<*>, Any>,
            valueFieldGetter: Function<in Enum<*>, Any>,
            labelFieldName: String = "label",
            valueFieldName: String = "value",
            constantNameGetter: Function<Class<out Enum<*>>, String> =
                Function<Class<out Enum<*>>, String> { t: Class<out Enum<*>> ->
                    t.simpleName + "Constant"
                }
        ): EnumConstantsGenerator {
            return OptionConstantsGenerator(
                labelFieldGetter,
                valueFieldGetter,
                labelFieldName,
                valueFieldName,
                constantNameGetter
            )
        }
    }


    class OptionConstantsGenerator(
        val labelFieldGetter: Function<in Enum<*>, Any>,
        val valueFieldGetter: Function<in Enum<*>, Any>,
        val labelFieldName: String,
        val valueFieldName: String,
        val constantNameGetter: Function<Class<out Enum<*>>, String>
    ) : EnumConstantsGenerator {
        override fun getConstants(clazz: Class<out Enum<*>>): List<EnumConstant> {
            val str = clazz.enumConstants.map {
                """
                |    {
                |        ${labelFieldName}: ${parseValue(labelFieldGetter.apply(it))},
                |        ${valueFieldName}: ${parseValue(valueFieldGetter.apply(it))}
                |    }
                """.trimMargin()
            }.joinToString(",\n")

            return listOf(
                EnumConstant(
                    constantNameGetter.apply(clazz), "[\n${str}\n]"
                )
            )
        }

        private fun parseValue(value: Any): String {
            return if (value is String) {
                "\"${value}\""
            } else {
                value.toString()
            }

        }
    }

}