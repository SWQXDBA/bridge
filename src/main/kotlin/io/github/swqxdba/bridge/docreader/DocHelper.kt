package io.github.swqxdba.bridge.docreader

import io.github.swqxdba.bridge.docreader.DocHelper.lookUpDocAtLinePrevAndSameLine

object DocHelper {
    fun getStartEndStringBlock(srcContent: String, startStr: String, endStr: String, index: Int = 0): String? {
        val src: String = srcContent.substring(index)
        var starCount = 0
        var startIndex: Int? = null
        var endIndex: Int? = null
        for ((index, c) in src.withIndex()) {
            if (subStartWith(src, index, startStr)) {
                if (startIndex == null) {
                    startIndex = index
                }
                starCount++
            }
            if (subStartWith(src, index, endStr)) {
                starCount--
                if (starCount == 0) {
                    endIndex = index
                    break
                }
            }
        }
        return if (startIndex != null && endIndex != null) {
            src.substring(startIndex, endIndex + endStr.length)
        } else {
            null
        }
    }


    //匹配一个子串 从str的startIndex开始匹配targetStr
    private fun subStartWith(str: String, startIndex: Int, targetStr: String): Boolean {
        for (i in targetStr.indices) {
            if (str[startIndex + i] != targetStr[i]) {
                return false
            }
        }
        return true
    }


    /**
     * 查找行的开始位置
     */
    fun findLineStartIndex(source: String, lineAnyCharIndex: Int): Int {
        var index = lineAnyCharIndex
        while (index >= 0) {
            if (source[index] == '\n') {
                return index + 1
            }
            index--
        }
        return 0
    }

    /**
     * 查找行的开始位置
     */
    fun prevLineStart(source: String, lineAnyCharIndex: Int): Int {
        //如果\n需要直接返回上一行 不然findLineStartIndex会定位到\n后的下一行 导致永远无法找到上一行
        if (source[lineAnyCharIndex] == '\n') {
            return lineAnyCharIndex - 1
        }
        val lineStartIndex = findLineStartIndex(source, lineAnyCharIndex)
        return findLineStartIndex(source, lineStartIndex) - 2//-2是因为有\n换行符
    }

    /**
     * 查找行的开始位置
     */
    fun readLine(source: String, lineAnyCharIndex: Int): String {
        findLineStartIndex(source, lineAnyCharIndex).let {
            var endIndex = it
            while (endIndex < source.length) {
                if (source[endIndex] == '\n') {
                    break
                }
                endIndex++
            }
            return source.substring(it, endIndex)
        }
    }

    fun prevLine(source: String, lineAnyCharIndex: Int): String {
        val lineStartIndex = findLineStartIndex(source, lineAnyCharIndex)
        val prevLineStartIndex = findLineStartIndex(source, lineStartIndex - 1)
        return readLine(source, prevLineStartIndex)
    }

    fun maybeDocLine(lineStr: String): Boolean {
        val line = lineStr.trim()
        return line.startsWith("/**") || line.endsWith("*/") || line.startsWith("*") || line.startsWith("//")
    }

    fun haveDocSymbol(lineStr: String): Boolean {
        val line = lineStr.trim()
        return line.contains("/**") || line.contains("*/") || line.contains("*") || line.contains("//")
    }

    //移除如'/**'或者'/*'或者'*/'或者'*'
    fun deleteDocSymbol(lines: List<String>): List<String> {

        return lines.map {
            var cur = it
            while (true) {
                val initLength = cur.length
                cur = cur.removePrefix("/**")
                    .removePrefix("/*")
                    .removeSuffix("*/")
                    .removePrefix("*")
                    .removePrefix("/")
                    .removeSuffix("/")
                    .trim()
                if (cur.length == initLength) {
                    break
                }
            }
            cur
        }
    }

    /**
     * 读取上一行和同一行的注释
     */
    fun lookUpDocAtLinePrevAndSameLine(sourceCode: String, codeLineIndex: Int): List<String>? {

        val doc1 = lookUpPrevDoc(sourceCode, codeLineIndex)
        val doc2 = lookUpSameLineDoc(sourceCode, codeLineIndex)

        if (doc1 == null && doc2 == null) {
            return null
        }
        val doc = mutableListOf<String>()
        if (doc1 != null) {
            doc.addAll(doc1)
        }
        if (doc2 != null) {
            doc.add(doc2)
        }
        return doc
    }

    //读取上一个注释 比如在代码行上方的注释
    fun lookUpPrevDoc(sourceCode: String, codeLineIndex: Int): List<String>? {
        var prevLineStartIndex = prevLineStart(sourceCode, codeLineIndex)
        var prevLine = readLine(sourceCode, prevLineStartIndex)

        //如果上一行是注解则跳过 跳过空行 其他语言可能不要跳过注解等等 这里先写死
        while ( prevLine.trim().startsWith("@")||
            //注解可能跨越多行 这里支持第二行结束的情况
            (prevLine.trim().endsWith(")") &&!maybeDocLine(prevLine)) ||
            prevLine.trim().isEmpty()) {
            prevLineStartIndex = prevLineStart(sourceCode, prevLineStartIndex)
            prevLine = readLine(sourceCode, prevLineStartIndex)
        }

        if (!maybeDocLine(prevLine)) {
            return null
        }

        val docLines = mutableListOf<String>()
        while (maybeDocLine(prevLine) || prevLine.isEmpty()) {
            if (prevLine.isNotEmpty()) {
                docLines.add(prevLine)
            }

            prevLineStartIndex = prevLineStart(sourceCode, prevLineStartIndex)
            prevLine = readLine(sourceCode, prevLineStartIndex)
        }
        docLines.reverse()

        return deleteDocSymbol(docLines)
    }

    //读取同一行的注释
    fun lookUpSameLineDoc(sourceCode: String, codeLineIndex: Int): String? {
        val line = readLine(sourceCode, codeLineIndex)
        if (!haveDocSymbol(line)) {
            return null
        }

        val pattern = "(//|/\\*|\\*/|\\*)"
        val regex = Regex(pattern)
        //匹配 注释后的内容
        val matchResult = regex.find(line) ?: return null
        val start = line.indexOf(matchResult.value)

        return deleteDocSymbol(listOf(line.substring(start)))[0]
    }

    //还原注释
    fun recoverComment(commentLines:List<String>?):String{
        if(commentLines.isNullOrEmpty()){
            return ""
        }
        val sb = StringBuilder()
        sb.append("/**\n")
        commentLines.forEach {
            sb.append("* ").append(it).append("\n")
        }
        sb.append("*/")
        return sb.toString()
    }
    //转义正则特殊字符 （）[]{}|+*?^$
    fun escapeExprSpecialWord(keyword: String): String {
        var keyword = keyword
        if (keyword.isNotBlank()) {
            val fbsArr = arrayOf("\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|")
            for (key in fbsArr) {
                if (keyword.contains(key)) {
                    keyword = keyword.replace(key, "\\" + key)
                }
            }
        }
        return keyword
    }
    fun findPatternLineStart(sourceCode:String,pattern: String):Int?{
        val regex = Regex(pattern)
        val matchResult = regex.find(sourceCode) ?: return null
        return findLineStartIndex(sourceCode, matchResult.range.first)
    }
}

fun main() {
    val pattern = "(class|enum|interface) +Person *\\{"
    val test = """
        import java.util.function.Function;
        //Person表示一个人
        class Person {
             String age;//age
        
        //666
        
        //名称1
        
        
        @Deprecated
        
        
         String name;//名称2
         
         public void toString();//777
    
          //打印
          public void toString2(){
          
          }
        }
        
    """.trimIndent()
    println(lookUpDocAtLinePrevAndSameLine(test, test.indexOf("String name;")))


//    val lookUpPrevDoc = lookUpPrevDoc(test, test.indexOf("String name"))
//
//    println(lookUpPrevDoc)
//
//    val sameLineDoc = lookUpSameLineDoc(test, test.indexOf("String name"))
//
//    println(sameLineDoc)

    println("读取方法注释 Person.toString")
    JavaDocReader.readMethodDoc(test,"Person","toString").forEach {
        println(it)
    }
    println("读取方法注释 Person.toString2")
    JavaDocReader.readMethodDoc(test,"Person","toString2").forEach {
        println(it)
    }


    println("读取Class注释 Person")
    JavaDocReader.readClassDoc(test,"Person").forEach {
        println(it)
    }


    println("读取Field注释 Person.name")
    JavaDocReader.readFieldDoc(test,"Person","name","String").forEach {
        println(it)
    }
}