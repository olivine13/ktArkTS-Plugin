// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish")
    kotlin("plugin.serialization")
}

group = "me.olivine"
version = "1.0.0"

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("li.songe:json5:0.0.1")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.1.0")
    }
}

gradlePlugin {
    website.set("https://github.com/olivine13/ktArkTS-Plugin")
    vcsUrl.set("https://github.com/olivine13/ktArkTS-Plugin.git")
    plugins {
        create("harmonyPlugin") {
            id = "me.olivine.ktarkts"
            implementationClass = "me.olivine.harmony.HarmonyPlugin"
            displayName = "Gradle plugin for KMM Multiplatform with ArkTS"
            description =
                "A Gradle plugin that simplifies and enhances the configuration of Kotlin Multiplatform projects for programming harmony application"
            tags.set(listOf("kotlin", "arkts", "harmony", "android studio"))
        }
    }
}

publishing {
    repositories {
        maven {
            name = "localPluginRepository" // 仓库名称（自定义）
            url = uri("../repo") // 仓库 URL
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            versionMapping {
                allVariants {
                    fromResolutionResult()
                }
            }

            pom {
                name.set("ktArkTS-Plugin")
                description.set("A Gradle plugin that simplifies and enhances the configuration of Kotlin Multiplatform projects for programming harmony application")
                url.set("https://github.com/olivine13/ktArkTS-Plugin")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}