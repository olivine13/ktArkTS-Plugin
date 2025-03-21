package me.olivine.harmony

import org.gradle.api.Plugin
import org.gradle.api.Project

class HarmonyPlugin : Plugin<Project> {

    companion object {
        const val TAG = "ktArkTS"
    }

    override fun apply(project: Project) {
        println("$TAG: apply project:${project.name}")
        project.dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
        project.dependencies.add("implementation", "li.songe:json5:0.0.1")

        val extension = project.extensions.create("harmony", HarmonyExtension::class.java)
        project.afterEvaluate {
            project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
                println("$TAG: sync HarmonyPlugin")
                project.tasks.findByName("jsNodeProductionRun")?.let { task ->
                    println("$TAG: register harmonyBuild")
                    // register a task named as "harmonyBuild"
                    project.tasks.register("harmonyBuild", HarmonyBuildTask::class.java) {
                        getLibraryName().set(extension.bundle ?: "library")
                        getOutputDir().set(extension.output)
                        getWorkSpaceDir().set(extension.workspace)
                        dependsOn(task.name)
                    }
                    project.tasks.register("harmonyRun", HarmonyRunTask::class.java) {
                        getWorkSpaceDir().set(extension.workspace)
                        if (project.hasProperty("full")) {
                            println("${HarmonyRunTask.TAG}: full compile")
                            dependsOn("harmonyBuild")
                        }
                    }
                }
            }
        }
    }
}