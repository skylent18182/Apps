package com.example.apkbuilder.model

enum class LogType {
    INFO,
    SUCCESS,
    WARN,
    ERROR,
    COMMAND,
    STEP
}

data class BuildLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val message: String,
    val type: LogType = LogType.INFO
)

data class ProjectFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isDirectory: Boolean = false,
    val extension: String = ""
)

data class ProjectInfo(
    val projectName: String,
    val packageName: String,
    val versionCode: String,
    val versionName: String,
    val minSdk: String,
    val targetSdk: String,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val files: List<ProjectFile>,
    val totalSizeBytes: Long,
    val sourceLanguage: String = "Kotlin / Java",
    val manifestContent: String? = null,
    val gradleContent: String? = null
)

data class BuiltApk(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val packageName: String,
    val versionName: String,
    val fileSizeBytes: Long,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sha256: String,
    val isSigned: Boolean = true
)

data class BuildOptions(
    val outputFileName: String = "app-release.apk",
    val isRelease: Boolean = false,
    val signWithDebugKey: Boolean = true,
    val zipalign: Boolean = true,
    val cleanBuild: Boolean = true
)

sealed interface BuildUiState {
    object Idle : BuildUiState
    data class Analyzing(val status: String, val progress: Float) : BuildUiState
    data class ProjectReady(val project: ProjectInfo) : BuildUiState
    data class Compiling(val step: String, val progress: Float, val currentTask: String) : BuildUiState
    data class Completed(val apk: BuiltApk) : BuildUiState
    data class Failed(val error: String, val details: String? = null) : BuildUiState
}
