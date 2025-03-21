package me.olivine.harmony

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.isSymbolicLink

object FileUtils {

    fun copyFileToDirectory(
        sourceFile: File,
        targetDir: File,
        overwrite: Boolean = true
    ): File {
        require(sourceFile.exists()) { "Source File NotFound: ${sourceFile.absolutePath}" }
        require(sourceFile.isFile) { "Source File is Not File: ${sourceFile.absolutePath}" }
        require(targetDir.isDirectory || targetDir.mkdirs()) {
            "Can't Create Target Dir: ${targetDir.absolutePath}"
        }

        val canonicalTarget = targetDir.canonicalPath
        val resolvedFile = File(targetDir, sourceFile.name).canonicalFile
        require(resolvedFile.canonicalPath.startsWith(canonicalTarget)) {
            "Target Path has Risk: ${sourceFile.name}"
        }

        return Files.copy(
            sourceFile.toPath(),
            resolvedFile.toPath(),
            if (overwrite) StandardCopyOption.REPLACE_EXISTING else StandardCopyOption.COPY_ATTRIBUTES
        ).toFile().also {
            println("File Copy Success: ${it.absolutePath}")
        }
    }

    fun copyDirectory(sourceDir: Path, targetDir: Path, ignoreSymbolLink: Boolean = true) {
        try {
            if (!Files.isDirectory(sourceDir)) {
                throw IllegalArgumentException("Source directory does not exist: $sourceDir")
            }

            Files.createDirectories(targetDir)

            Files.walkFileTree(sourceDir, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (ignoreSymbolLink && dir.isSymbolicLink()) return FileVisitResult.CONTINUE

                    val relativePath = sourceDir.relativize(dir)
                    val targetPath = targetDir.resolve(relativePath)
                    Files.createDirectories(targetPath)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (ignoreSymbolLink && file.isSymbolicLink()) return FileVisitResult.CONTINUE

                    val relativePath = sourceDir.relativize(file)
                    val targetFile = targetDir.resolve(relativePath)
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (e: IOException) {
            throw IOException("Failed to copy directory: ${e.message}")
        }
    }

    fun unzip(inputStream: InputStream, dest: File) {
        if (dest.isFile || !dest.exists()) {
            dest.mkdirs()
        }

        ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            val buffer = ByteArray(1024)

            while (entry != null) {
                val filePath = File(dest, entry.name).canonicalPath
                if (!filePath.startsWith(dest.canonicalPath + File.separator)) {
                    throw IOException("Entry is outside of the target directory: ${entry.name}")
                }

                if (entry.isDirectory) {
                    File(filePath).mkdirs()
                } else {
                    File(filePath).parentFile?.mkdirs()
                    FileOutputStream(filePath).use { fos ->
                        BufferedOutputStream(fos).use { bos ->
                            var length: Int
                            while (zipIn.read(buffer).also { length = it } > 0) {
                                bos.write(buffer, 0, length)
                            }
                        }
                    }
                }
                entry = zipIn.nextEntry
            }
        }
    }

    fun renameFolderUsingNio(oldPath: String, newPath: String): Boolean {
        try {
            val source: Path = Paths.get(oldPath)
            val target: Path = Paths.get(newPath)

            if (!Files.exists(source) || !Files.isDirectory(source)) {
                println("Original File Not Existed or Is Not Folder")
                return false
            }

            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING
            )
            return true
        } catch (e: Exception) {
            when (e) {
                is SecurityException -> println("Permission Not Granted")
                is java.nio.file.FileAlreadyExistsException -> println("Folder Existed")
                else -> println("Exception: ${e.message}")
            }
            return false
        }
    }

    fun deleteDirectoryNio(path: Path): Boolean {
        return try {
            Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            })
            true
        } catch (e: NoSuchFileException) {
            println("Folder not existed: ${e.file}")
            false
        } catch (e: AccessDeniedException) {
            println("Permission Not Granted: ${e.file}")
            false
        } catch (e: IOException) {
            println("IOException: ${e.message}")
            false
        }
    }
}