package io.github.swqxdba.bridge.codegen.typescript


import freemarker.template.Configuration
import freemarker.template.Template
import io.github.swqxdba.bridge.codegen.IGenerator
import io.github.swqxdba.bridge.codegen.typescript.meta.TsApiMeta
import io.github.swqxdba.bridge.meta.BridgeGlobalConfig
import io.github.swqxdba.bridge.meta.ControllerMeta
import org.apache.logging.log4j.LogManager
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths


class TsGenerator : IGenerator {


    val logger = LogManager.getLogger(TsGenerator::class.java)


    var basePath = "gens/"

    var genApi = true

    var genApis = true

    var genTypes = true

    var overrideFile = true

    var cleanBasePath = false

    var typeDirName = "types"

    var apiDirName = "api"


    //在各个api中导入所需类型的路径前缀 后面会拼接上typeDirName+"/"+{类型名}
    //比如 apiImportTypePrefix + typeDirName+"/" +"Response"
    var apiImportTypePrefix = "../"

    // 在api和 apis文件中导入的httpClient的路径
    var importHttpClientPath = "../HttpClient"

    override fun generate(controllerMeta: List<ControllerMeta>) {
        if (!Files.exists(Paths.get(basePath))) {
            Files.createDirectories(Paths.get(basePath))
        }
        if (cleanBasePath) {
            Files.walk(Paths.get(basePath))
                .sorted(Comparator.reverseOrder())
                .map { it.toFile() }
                .forEach { it.delete() }
        }

        val tsApiMetaList = controllerMeta.map { TsApiMeta(it, apiImportTypePrefix + "${typeDirName}/") }

        tsApiMetaList.forEach { genOne(it) }

        if (genApis) {
            logger.info("gen apis...")
            genApis(tsApiMetaList)
        }


        logger.info("generate end!")
    }

    val generatedTypes = mutableSetOf<String>()
    private fun genOne(meta: TsApiMeta) {
        if (genApi) {
            if (BridgeGlobalConfig.enableDetailLog) {
                logger.info("gen api => ${meta.clientClassName}")
            }

            val template = getTemplate("Api.ftl")
            val dir = basePath + File.separator + apiDirName
            if (!Files.exists(Paths.get(dir))) {
                File(dir).mkdirs()
            }
            val filePath = dir + File.separator + meta.clientClassName + ".ts"
            writeToFileIfNeed(template, filePath, meta)
        }
        if (genTypes) {
            val template = getTemplate("Type.ftl")
            val dir = basePath + File.separator + typeDirName
            if (!Files.exists(Paths.get(dir))) {
                File(dir).mkdirs()
            }
            val types = meta.deepCollectTypes()

            for (type in types) {

                if (BridgeGlobalConfig.enableDetailLog) {
                    logger.info("try gen for+++${type.typeString}")
                }


                val defineType = type
                val typeRowString = defineType.typeRowString
                if (typeRowString == null) {
                    logger.warn("typeRowString is null for ${defineType.typeString}")
                    continue
                }
                if (generatedTypes.contains(typeRowString)) {
                    if (BridgeGlobalConfig.enableDetailLog) {
                        logger.info("jump generated type for ${typeRowString}")
                    }

                    continue
                }
                generatedTypes.add(typeRowString)
                if (BridgeGlobalConfig.enableDetailLog) {
                    logger.info("gen type => ${typeRowString}")
                }


                val filePath = dir + File.separator + typeRowString + ".ts"
                writeToFileIfNeed(template, filePath, defineType)
            }

        }

        val dir = basePath + File.separator + apiDirName
        if (!Files.exists(Paths.get(dir))) {
            File(dir).mkdirs()
        }
        ClassPathResource("/templates/ts/code/HttpClient.ts").inputStream.use {
            File(dir + File.separator + "HttpClient.ts").writeBytes(it.readBytes())
        }
    }

    //apiList中每个元素对应一个controller
    class Apis(val apiList: List<TsApiMeta>)

    private fun writeToFileIfNeed(template: Template, filePath: String, data: Any) {
        if (overrideFile || !Files.exists(Paths.get(filePath))) {
            template.process(data, FileWriter(filePath))
        }
    }

    private fun genApis(controllerMeta: List<TsApiMeta>) {
        val template = getTemplate("Apis.ftl")
        val filePath = basePath + File.separator + apiDirName + File.separator + "Apis.ts"
        writeToFileIfNeed(template, filePath, Apis(controllerMeta))
    }

    private fun getHttpClient() {

    }

    private fun getTemplate(name: String): Template {
        val cfg = Configuration()
        cfg.setClassForTemplateLoading(TsGenerator::class.java, "/templates/ts/code")
        return cfg.getTemplate(name)
    }

}