package me.olivine.harmony

import java.io.File

object RuntimeUtils {

    const val DEBUG = true

    fun exec(command: String, directory: File? = null): String? {
        return exec(arrayListOf(command), directory)
    }

    fun exec(command: List<String>, directory: File? = null): String? {
        try {
            val processBuilder = ProcessBuilder(command)
            directory?.let { processBuilder.directory(it) }
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (DEBUG) {
                println("Output:\n$output")
                println("Exit Code: $exitCode")
            }
            return output
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}