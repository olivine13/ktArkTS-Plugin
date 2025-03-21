package me.olivine.harmony

import me.olivine.harmony.model.PackageVO
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.StringReader

abstract class HarmonyBuildTask : DefaultTask() {

    companion object {
        const val TAG = "ktArkTS-Build"
    }

    @Input
    abstract fun getLibraryName(): Property<String>

    @InputDirectory
    abstract fun getOutputDir(): RegularFileProperty

    @InputDirectory
    abstract fun getWorkSpaceDir(): RegularFileProperty

    @TaskAction
    fun buildArkTS() {
        val rootName = project.rootProject.name
        val name = project.name
        val library = getLibraryName().get()
        val hubDir =
            project.rootProject.layout.buildDirectory.dir("harmony/${rootName}-${name}")
                .get().asFile
        val outputDir = project.rootProject.layout.buildDirectory.dir("harmony/outputs")
            .get().asFile
        try {
            configProject(project, hubDir, library)
        } catch (e: Exception) {
            println("$TAG: config hub failed error:${e.message}")
        }
        try {
            val jsNodeModulesDir =
                project.rootProject.layout.buildDirectory.dir("js/node_modules").get().asFile
            val targetNodeModulesDir =
                project.rootProject.layout.buildDirectory.dir("harmony/backup/node_modules")
                    .get().asFile
            FileUtils.copyDirectory(jsNodeModulesDir.toPath(), targetNodeModulesDir.toPath())
            val subHarFolderName = convertDependencies(targetNodeModulesDir.parentFile)
            if (subHarFolderName.isNotEmpty()) {
                val jsDir =
                    project.rootProject.layout.buildDirectory.dir("harmony/backup/${subHarFolderName}")
                        .get().asFile
                val targetDir =
                    project.rootProject.layout.buildDirectory.dir("harmony/outputs/har")
                        .get().asFile
                if (!targetDir.isDirectory && !targetDir.exists()) {
                    targetDir.mkdirs()
                }
                FileUtils.copyDirectory(jsDir.toPath(), targetDir.toPath())

                val map = mutableMapOf<String, String>()
                targetDir.listFiles()
                    ?.forEach {
                        if (it.extension == "har") {
                            map[it.nameWithoutExtension] =
                                "../../build/harmony/outputs/har/${it.name}"
                        }
                    }
                val updatedJson = Json { prettyPrint = true }.encodeToString(PackageVO(map))
                val mapFile =
                    project.rootProject.layout.buildDirectory.dir("harmony/outputs/map.txt")
                        .get().asFile
                mapFile.writeText(updatedJson)
                jsDir.deleteRecursively()
            }
        } catch (e: Exception) {
            println("$TAG: convert har failed error:${e.message}")
        }
        try {
            println("$TAG: copy js file start")
            val jsDir =
                project.rootProject.layout.buildDirectory.dir("js/packages/${rootName}-${name}/kotlin")
                    .get().asFile
            val targetDir =
                project.rootProject.layout.buildDirectory.dir("harmony/${rootName}-${name}/${library}/src/main/ets")
                    .get().asFile
            if (!targetDir.isDirectory && !targetDir.exists()) {
                targetDir.mkdirs()
            }
            FileUtils.copyDirectory(jsDir.toPath(), targetDir.toPath())
            println("$TAG: copy js file end")
        } catch (e: Exception) {
            println("$TAG: copy js file failed error:${e.message}")
        }
        RuntimeUtils.exec(
            listOf(
                "hvigorw",
                "assembleHar",
                "--mode",
                "module",
                "-p",
                "module=${library}@default",
                "-p",
                "product=default",
                "--no-daemon"
            ), hubDir
        )
        val output =
            project.rootProject.layout.buildDirectory.dir("harmony/${rootName}-${name}/${library}/build/default/outputs/default/${library}.har")
                .get().asFile
        FileUtils.copyFileToDirectory(output, outputDir)
        getOutputDir().orNull?.let { file ->
            FileUtils.copyFileToDirectory(output, file.asFile)
        }
        RuntimeUtils.exec(
            listOf(
                "hvigorw",
                "--sync",
                "-p",
                "product=default",
                "--analyze=normal",
                "--parallel",
                "--incremental",
                "--no-daemon"
            ), hubDir
        )
    }

    private fun configProject(project: Project, dest: File, library: String) {
        println("$TAG: configProject start")
        val rootName = project.rootProject.name
        val name = project.name
        this.javaClass.getResourceAsStream("/project.template").let { FileUtils.unzip(it, dest) }
        val buildFile = File(dest, "build-profile.json5")
        buildFile.writeText(buildFile.readText().replace("library", library))
        val templateFile = File(dest, "library")
        val libraryFile = File(dest, library)
        if (!libraryFile.exists()) {
            val ohPackageFile = File(templateFile, "oh-package.json5")
            val moduleFile = File(templateFile, "src/main/module.json5")
            val indexFile = File(templateFile, "src/main/ets/Index.ets")
            println("$TAG: change build-profile.json5")
            println("$TAG: change og-package.json5")
            ohPackageFile.writeText(ohPackageFile.readText().replace("library", library))
            println("$TAG: change module.json5")
            moduleFile.writeText(moduleFile.readText().replace("library", library))
            indexFile.writeText("import * as kt from \"./${rootName}-${name}\"")
            FileUtils.renameFolderUsingNio(templateFile.absolutePath, libraryFile.absolutePath)
        }
        FileUtils.deleteDirectoryNio(templateFile.toPath())
        println("$TAG: configProject end")
    }

    private fun convertDependencies(dest: File): String {
        println("$TAG: convert dependencies to ohpm-packages start")
        updatePackageJson(dest)
        var result = ""
        RuntimeUtils.exec(listOf("ohpm", "convert", "node_modules"), dest)?.let {
            val sr = StringReader(it)
            val line = sr.readLines().find { str ->
                return@find str.contains("Converted packages are saved")
            }.toString()
            val pattern = "convert_\\d+".toRegex()
            val folderName = pattern.find(line)?.value
            result = folderName ?: ""
        }
        println("$TAG: convert dependencies to ohpm-packages end")
        return result
    }

    private fun updatePackageJson(dir: File) {
        dir.listFiles()
            ?.forEach {
                if (it.isDirectory) {
                    updatePackageJson(it)
                } else if (it.name.equals("package.json")) {
                    println("$TAG: package.json file=${it.name}")
                    try {
                        val jsonString = it.readText()
                        val element = Json.parseToJsonElement(jsonString).jsonObject
                        val result = element.jsonObject.toMutableMap().apply {
                            remove("dependencies")
                        }
                        it.writeText(Json { prettyPrint = true }.encodeToString(result))
                        println("$TAG: modified package.json file=${it.absoluteFile}")
                    } catch (e: Exception) {
                        println("$TAG: operate failed error:$e")
                    }
                }
            }
    }
}