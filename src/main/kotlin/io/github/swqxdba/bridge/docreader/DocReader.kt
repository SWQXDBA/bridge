package io.github.swqxdba.bridge.docreader

import io.github.swqxdba.bridge.meta.ApiComment
import java.lang.reflect.Field
import java.lang.reflect.Method

interface DocReader {
    fun readClassDoc(sourceCode: String, className: String, clazz: Class<*>? = null): List<String>

    fun readMethodDoc(sourceCode: String, className: String, methodName: String, method: Method? = null): List<String>

    fun readFieldDoc(
        sourceCode: String,
        className: String,
        fieldName: String,
        fieldTypeName: String,
        field: Field? = null
    ): List<String>
}

class AnnotationFirstDocDeaderWrapper(private val src: DocReader) : DocReader by src {
    override fun readClassDoc(sourceCode: String, className: String, clazz: Class<*>?): List<String> {
        return if (clazz?.isAnnotationPresent(ApiComment::class.java) == true) {
            listOf(clazz.getAnnotation(ApiComment::class.java).value)
        } else {
            src.readClassDoc(sourceCode, className, clazz)
        }
    }

    override fun readMethodDoc(
        sourceCode: String,
        className: String,
        methodName: String,
        method: Method?
    ): List<String> {
        return if (method?.isAnnotationPresent(ApiComment::class.java) == true) {
            listOf(method.getAnnotation(ApiComment::class.java).value)
        } else {
            src.readMethodDoc(sourceCode, className, methodName, method)
        }
    }

    override fun readFieldDoc(
        sourceCode: String,
        className: String,
        fieldName: String,
        fieldTypeName: String,
        field: Field?
    ): List<String> {
        return if (field?.isAnnotationPresent(ApiComment::class.java) == true) {
            listOf(field.getAnnotation(ApiComment::class.java).value)
        } else {
            src.readFieldDoc(sourceCode, className, fieldName, fieldTypeName, field)
        }
    }
}