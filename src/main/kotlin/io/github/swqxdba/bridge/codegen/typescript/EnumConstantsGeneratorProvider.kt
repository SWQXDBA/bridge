package io.github.swqxdba.bridge.codegen.typescript

interface EnumConstantsGeneratorProvider {
    fun getGenerators(clazz: Class<out Enum<*>>):List<EnumConstantsGenerator>
}