package io.github.swqxdba.bridge.docreader

import java.lang.reflect.Field
import java.lang.reflect.Method

object KotlinDocReader : DocReader {
    override fun readClassDoc(sourceCode: String, className: String,clazz:Class<*>?): List<String> {
        return findClassLineStart(sourceCode, className)?.let {
            DocHelper.lookUpDocAtLinePrevAndSameLine(sourceCode, it)
        } ?: emptyList()
    }

    override fun readMethodDoc(sourceCode: String, className: String, methodName: String,method: Method?): List<String> {
        return findMethodLineStart(sourceCode, methodName)?.let {
            DocHelper.lookUpDocAtLinePrevAndSameLine(sourceCode, it)
        } ?: emptyList()
    }

    override fun readFieldDoc(
        sourceCode: String,
        className: String,
        fieldName: String,
        fieldTypeName: String,field: Field?
    ): List<String> {
        return findFieldLineStart(sourceCode, fieldName)?.let {
            DocHelper.lookUpDocAtLinePrevAndSameLine(sourceCode, it)
        } ?: emptyList()
    }

    private fun findClassLineStart(sourceCode: String, className: String): Int? {
        val pattern = "(class|enum|interface|object) +$className"
        return DocHelper.findPatternLineStart(sourceCode, pattern)
    }

    private fun findMethodLineStart(sourceCode: String, methodName: String): Int? {
        val pattern = "fun .*${DocHelper.escapeExprSpecialWord(methodName)}"
        return DocHelper.findPatternLineStart(sourceCode, pattern)
    }

    fun findFieldLineStart(
        sourceCode: String,
        fieldName: String,
    ): Int? {
        return DocHelper.findPatternLineStart(sourceCode, "val +$fieldName") ?: DocHelper.findPatternLineStart(
            sourceCode,
            "var +$fieldName"
        )
    }

}

fun main() {
    val src = """
            val abc = arrayOf(1,2,3)
            
            var   cdef = 1
        """.trimIndent()
    println(
        KotlinDocReader.findFieldLineStart(
            src, "cdef"
        )
    )
    println(
        src.substring(
            KotlinDocReader.findFieldLineStart(
                src, "cdef"
            )!!
        )
    )
}