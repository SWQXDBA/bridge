package io.github.swqxdba.bridge.meta

import java.io.File
import java.net.URLDecoder


internal object PackageClassResolver {
    fun resolveClasses(packageName: String): List<String> {
        val classLoader = PackageClassResolver::class.java.classLoader
        val dir = packageName.replace(".", File.separator)
        val resources = classLoader.getResources(dir).toList()
        val result = mutableListOf<String>()
        for (resource in resources) {
            if (resource.protocol != "file") {
                continue
            }
            val fileName = URLDecoder.decode(resource.file)
            val treeWalk = File(fileName).walk()
            treeWalk.forEach { subFile ->
                val name = subFile.name
                if (name.endsWith(".class")) {
                    val className = subFile.absolutePath.
                    substring(subFile.absolutePath.lastIndexOf(dir))
                        .replace("/",".")
                        .replace("\\",".")
                        .removeSuffix(".class")
                    result+= className
                }

            }
        }
        return result
    }

}