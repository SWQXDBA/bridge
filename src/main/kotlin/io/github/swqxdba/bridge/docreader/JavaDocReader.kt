package io.github.swqxdba.bridge.docreader

import java.lang.reflect.Field
import java.lang.reflect.Method

object JavaDocReader : DocReader {
    override fun readClassDoc(sourceCode: String, className: String,clazz:Class<*>?): List<String> {
        return findClassLineStart(sourceCode, className)?.let {
            DocHelper.lookUpDocAtLinePrevAndSameLine(sourceCode, it)
        } ?: emptyList()
    }

    override fun readMethodDoc(sourceCode: String, className: String, methodName: String,method: Method?): List<String> {
        return findMethodLineStart(sourceCode, className, methodName)?.let {
            DocHelper.lookUpDocAtLinePrevAndSameLine(sourceCode, it)
        } ?: emptyList()
    }

    override fun readFieldDoc(
        sourceCode: String,
        className: String,
        fieldName: String,
        fieldTypeName: String,field: Field?
    ): List<String> {
        return findFieldLineStart(sourceCode, className, fieldName, fieldTypeName)?.let {
            DocHelper.lookUpDocAtLinePrevAndSameLine(sourceCode, it)
        } ?: emptyList()
    }


    private fun findClassLineStart(sourceCode: String, className: String): Int? {
        val pattern = "(class|enum|interface) +$className[^{]*\\{"
        return DocHelper.findPatternLineStart(sourceCode, pattern)
    }

    private fun findMethodLineStart(sourceCode: String, className: String, methodName: String): Int? {
        val classLine = findClassLineStart(sourceCode, className) ?: return null
        val classBody = DocHelper.getStartEndStringBlock(sourceCode.substring(classLine), "{", "}") ?: return null

        //接口或者类的方法定义，比如 toString(); 或者toString() {

        //开头的' +'是为了区分方法定义和调用。比如 void toString 和 .toString 一般定义时前面会有空格 而调用时前面是点
        //因为可能存在其他类上面的同名的方法 因此这个名字的方法的调用可能早于声明出现。
        val methodDefineRegex = " +${DocHelper.escapeExprSpecialWord(methodName)} *\\(.*\\) *([{;])"
            .toRegex(RegexOption.DOT_MATCHES_ALL)//让.能够匹配所有字符 因为默认不能匹配换行符


        val matchResult = methodDefineRegex.find(classBody) ?: return null
        return DocHelper.findLineStartIndex(
            sourceCode,
            //这里要加上classLine，因为classBody是从sourceCode的classLine开始的
            matchResult.range.first + sourceCode.indexOf(classBody)
        )

    }


    private fun findFieldLineStart(
        sourceCode: String,
        className: String,
        fieldName: String,
        fieldTypeName: String
    ): Int? {
        val classLine = findClassLineStart(sourceCode, className) ?: return null
        val classBody = DocHelper.getStartEndStringBlock(sourceCode.substring(classLine), "{", "}") ?: return null

        //String name;

        //考虑到难以还原出fieldTypeName的真实类型 这里做模糊匹配
        //比如List<String> names;  那么fieldTypeName = List 也能匹配到
        //这里只禁止了换行

        val fieldRegex =
            "${DocHelper.escapeExprSpecialWord(fieldTypeName)}[^\\n]* +${DocHelper.escapeExprSpecialWord(fieldName)} *[;|=]".toRegex(RegexOption.DOT_MATCHES_ALL)

        val matchResult = fieldRegex.find(classBody) ?: return null
        return DocHelper.findLineStartIndex(
            sourceCode,
            ////这里要加上classLine，因为classBody是从sourceCode的classLine开始的
            matchResult.range.first + sourceCode.indexOf(classBody)
        )

    }
}

