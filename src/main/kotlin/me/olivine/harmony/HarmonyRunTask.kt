package me.olivine.harmony

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import li.songe.json5.Json5
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class HarmonyRunTask : DefaultTask() {

    companion object {
        const val TAG = "ktArkTS-Run"
    }

    @InputDirectory
    abstract fun getWorkSpaceDir(): RegularFileProperty

    @TaskAction
    fun runApplication() {
        val workDir = getWorkSpaceDir().asFile.get()
        val bundleName = Json5
            .parseToJson5Element(File(workDir, "AppScope/app.json5").readText())
            .jsonObject["app"]?.jsonObject?.get("bundleName")?.jsonPrimitive?.content ?: ""
        val entry = Json5
            .parseToJson5Element(File(workDir, "entry/src/main/module.json5").readText())
            .jsonObject["module"]?.jsonObject?.get("abilities")?.jsonArray?.get(0)?.jsonObject?.get(
            "name"
        )?.jsonPrimitive?.content ?: ""
        println("$TAG: bundleName=$bundleName entry=$entry")
        val tempDir = File("data/local/tmp", System.currentTimeMillis().toString())
        println("$TAG: tempDir=$tempDir")
        RuntimeUtils.exec("hvigorw clean --no-daemon".split(" "), workDir)
        RuntimeUtils.exec(
            "hvigorw assembleHap --mode module -p product=default -p buildMode=debug --no-daemon".split(
                " "
            ), workDir
        )
        RuntimeUtils.exec("hdc shell aa force-stop $bundleName".split(" "), workDir)
        RuntimeUtils.exec("hdc shell mkdir $tempDir".split(" "), workDir)
        RuntimeUtils.exec(
            "hdc file send $workDir/entry/build/default/outputs/default/entry-default-signed.hap \"$tempDir\"".split(
                " "
            ), workDir
        )
        RuntimeUtils.exec("hdc shell bm install -p $tempDir".split(" "), workDir)
        RuntimeUtils.exec("hdc shell rm -rf $tempDir".split(" "), workDir)
        RuntimeUtils.exec("hdc shell aa start -a $entry -b $bundleName".split(" "), workDir)
    }
}