package me.olivine.harmony

import java.io.File

abstract class HarmonyExtension {

    abstract var bundle: String?

    abstract var workspace: File?

    abstract var output: File?
}