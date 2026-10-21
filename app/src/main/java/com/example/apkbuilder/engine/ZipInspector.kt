package com.example.apkbuilder.engine

import android.content.Context
import com.example.apkbuilder.model.ProjectFile
import com.example.apkbuilder.model.ProjectInfo
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object ZipInspector {

    fun inspectZip(
        context: Context,
        inputStream: InputStream,
        projectOutputFolder: File
    ): ProjectInfo {
        if (projectOutputFolder.exists()) {
            projectOutputFolder.deleteRecursively()
        }
        projectOutputFolder.mkdirs()

        val files = mutableListOf<ProjectFile>()
        var totalBytes = 0L

        var manifestContent: String? = null
        var gradleContent: String? = null

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(8192)

            while (entry != null) {
                val entryFile = File(projectOutputFolder, entry.name)
                val normalizedPath = entryFile.canonicalPath
                val baseDirPath = projectOutputFolder.canonicalPath

                // Security check against Zip Slip vulnerability
                if (!normalizedPath.startsWith(baseDirPath)) {
                    entry = zis.nextEntry
                    continue
                }

                if (entry.isDirectory) {
                    entryFile.mkdirs()
                    files.add(
                        ProjectFile(
                            name = entryFile.name,
                            path = entry.name,
                            sizeBytes = 0L,
                            isDirectory = true
                        )
                    )
                } else {
                    entryFile.parentFile?.mkdirs()
                    var entryBytes = 0L
                    entryFile.outputStream().use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                            entryBytes += len
                        }
                    }
                    totalBytes += entryBytes
                    val ext = entryFile.extension

                    files.add(
                        ProjectFile(
                            name = entryFile.name,
                            path = entry.name,
                            sizeBytes = entryBytes,
                            isDirectory = false,
                            extension = ext
                        )
                    )

                    if (entry.name.endsWith("AndroidManifest.xml")) {
                        manifestContent = entryFile.readText()
                    }
                    if (entry.name.endsWith("build.gradle") || entry.name.endsWith("build.gradle.kts")) {
                        gradleContent = entryFile.readText()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Parse extracted manifest and build.gradle
        val packageName = extractPackageName(manifestContent, gradleContent)
        val permissions = extractPermissions(manifestContent)
        val activities = extractActivities(manifestContent)
        val services = extractServices(manifestContent)
        val minSdk = extractValue(gradleContent, "minSdk", "24")
        val targetSdk = extractValue(gradleContent, "targetSdk", "35")
        val versionCode = extractValue(gradleContent, "versionCode", "1")
        val versionName = extractVersionName(gradleContent)
        val projectName = extractProjectName(manifestContent, packageName)

        val hasKotlin = files.any { it.extension == "kt" }
        val hasJava = files.any { it.extension == "java" }
        val language = when {
            hasKotlin && hasJava -> "Kotlin & Java"
            hasKotlin -> "Kotlin"
            hasJava -> "Java"
            else -> "Android XML / Native"
        }

        return ProjectInfo(
            projectName = projectName,
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            minSdk = minSdk,
            targetSdk = targetSdk,
            permissions = permissions,
            activities = activities,
            services = services,
            files = files,
            totalSizeBytes = totalBytes,
            sourceLanguage = language,
            manifestContent = manifestContent,
            gradleContent = gradleContent
        )
    }

    private fun extractPackageName(manifest: String?, gradle: String?): String {
        manifest?.let {
            val match = Regex("""package\s*=\s*["']([^"']+)["']""").find(it)
            if (match != null) return match.groupValues[1]
        }
        gradle?.let {
            val matchNamespace = Regex("""namespace\s*[=:]?\s*['"]([^'"]+)['"]""").find(it)
            if (matchNamespace != null) return matchNamespace.groupValues[1]

            val matchAppId = Regex("""applicationId\s*[=:]?\s*['"]([^'"]+)['"]""").find(it)
            if (matchAppId != null) return matchAppId.groupValues[1]
        }
        return "com.example.app"
    }

    private fun extractProjectName(manifest: String?, defaultPkg: String): String {
        manifest?.let {
            val match = Regex("""android:label\s*=\s*["']([^"']+)["']""").find(it)
            if (match != null) {
                val label = match.groupValues[1]
                if (!label.startsWith("@string/")) return label
            }
        }
        return defaultPkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private fun extractPermissions(manifest: String?): List<String> {
        if (manifest == null) return emptyList()
        val regex = Regex("""<uses-permission[^>]+android:name\s*=\s*["']([^"']+)["']""")
        return regex.findAll(manifest).map { it.groupValues[1] }.toList()
    }

    private fun extractActivities(manifest: String?): List<String> {
        if (manifest == null) return emptyList()
        val regex = Regex("""<activity[^>]+android:name\s*=\s*["']([^"']+)["']""")
        return regex.findAll(manifest).map { it.groupValues[1] }.toList()
    }

    private fun extractServices(manifest: String?): List<String> {
        if (manifest == null) return emptyList()
        val regex = Regex("""<service[^>]+android:name\s*=\s*["']([^"']+)["']""")
        return regex.findAll(manifest).map { it.groupValues[1] }.toList()
    }

    private fun extractValue(gradle: String?, key: String, default: String): String {
        if (gradle == null) return default
        val regex = Regex("""$key\s*[=:]?\s*(\d+)""")
        val match = regex.find(gradle)
        return match?.groupValues?.get(1) ?: default
    }

    private fun extractVersionName(gradle: String?): String {
        if (gradle == null) return "1.0"
        val regex = Regex("""versionName\s*[=:]?\s*['"]([^'"]+)['"]""")
        val match = regex.find(gradle)
        return match?.groupValues?.get(1) ?: "1.0"
    }
}
