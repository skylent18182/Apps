package com.example.apkbuilder.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apkbuilder.engine.ApkPackagerEngine
import com.example.apkbuilder.engine.ZipInspector
import com.example.apkbuilder.model.BuildLog
import com.example.apkbuilder.model.BuildOptions
import com.example.apkbuilder.model.BuildUiState
import com.example.apkbuilder.model.BuiltApk
import com.example.apkbuilder.model.LogType
import com.example.apkbuilder.model.ProjectInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ApkBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<BuildUiState>(BuildUiState.Idle)
    val uiState: StateFlow<BuildUiState> = _uiState.asStateFlow()

    private val _logs = MutableStateFlow<List<BuildLog>>(emptyList())
    val logs: StateFlow<List<BuildLog>> = _logs.asStateFlow()

    private val _builtApks = MutableStateFlow<List<BuiltApk>>(emptyList())
    val builtApks: StateFlow<List<BuiltApk>> = _builtApks.asStateFlow()

    private val _buildOptions = MutableStateFlow(BuildOptions())
    val buildOptions: StateFlow<BuildOptions> = _buildOptions.asStateFlow()

    private var currentProjectDir: File? = null
    private var currentProjectInfo: ProjectInfo? = null

    init {
        loadExistingApks()
        // Pre-load sample project automatically so the user immediately has a functional demo ready to build!
        loadSampleProject()
    }

    private fun addLog(message: String, type: LogType = LogType.INFO) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val log = BuildLog(timestamp = time, message = message, type = type)
        _logs.value = _logs.value + log
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun updateBuildOptions(options: BuildOptions) {
        _buildOptions.value = options
    }

    fun loadSampleProject() {
        viewModelScope.launch {
            try {
                _uiState.value = BuildUiState.Analyzing("Beispiel-Projekt wird geladen...", 0.3f)
                addLog("Lade integriertes Beispiel-Projekt aus Assets...", LogType.STEP)

                val context = getApplication<Application>()
                val assetStream: InputStream = context.assets.open("sample_project.zip")
                val projectDir = File(context.cacheDir, "current_project")

                withContext(Dispatchers.IO) {
                    val info = ZipInspector.inspectZip(context, assetStream, projectDir)
                    currentProjectDir = projectDir
                    currentProjectInfo = info

                    // Configure default output name
                    val defaultName = "${info.projectName.lowercase().replace(" ", "-")}.apk"
                    _buildOptions.value = _buildOptions.value.copy(outputFileName = defaultName)
                }

                val info = currentProjectInfo!!
                addLog("Projekt erfolgreich geladen: ${info.projectName} (${info.packageName})", LogType.SUCCESS)
                addLog("${info.files.size} Dateien extrahiert, MinSDK=${info.minSdk}, TargetSDK=${info.targetSdk}", LogType.INFO)
                _uiState.value = BuildUiState.ProjectReady(info)
            } catch (e: Exception) {
                addLog("Fehler beim Laden des Beispiel-Projekts: ${e.message}", LogType.ERROR)
                _uiState.value = BuildUiState.Failed("Fehler beim Laden", e.localizedMessage)
            }
        }
    }

    fun loadProjectFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = BuildUiState.Analyzing("Projekt-ZIP wird analysiert...", 0.2f)
                addLog("Öffne ausgewählte ZIP-Datei: $uri", LogType.STEP)

                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Datei konnte nicht gelesen werden.")

                val projectDir = File(context.cacheDir, "imported_project")

                withContext(Dispatchers.IO) {
                    val info = ZipInspector.inspectZip(context, inputStream, projectDir)
                    currentProjectDir = projectDir
                    currentProjectInfo = info

                    val defaultName = "${info.projectName.lowercase().replace(" ", "-")}.apk"
                    _buildOptions.value = _buildOptions.value.copy(outputFileName = defaultName)
                }

                val info = currentProjectInfo!!
                addLog("Projekt erfolgreich analysiert: ${info.projectName}", LogType.SUCCESS)
                addLog("Paketname: ${info.packageName} | Version: ${info.versionName}", LogType.INFO)
                addLog("Dateien im Projekt: ${info.files.size}", LogType.INFO)
                _uiState.value = BuildUiState.ProjectReady(info)
            } catch (e: Exception) {
                addLog("Fehler beim Entpacken der ZIP: ${e.message}", LogType.ERROR)
                _uiState.value = BuildUiState.Failed("Analyse fehlgeschlagen", e.localizedMessage)
            }
        }
    }

    fun startBuild() {
        val info = currentProjectInfo ?: run {
            addLog("Kein Projekt zum Bauen ausgewählt!", LogType.ERROR)
            return
        }
        val dir = currentProjectDir ?: run {
            addLog("Projektverzeichnis ungültig!", LogType.ERROR)
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = BuildUiState.Compiling("Initialisierung", 0.05f, "Build starten")
                addLog("=========================================", LogType.STEP)
                addLog("STARTE BUILD-PROZESS FÜR ${info.projectName}", LogType.STEP)
                addLog("=========================================", LogType.STEP)

                val context = getApplication<Application>()
                val builtApk = withContext(Dispatchers.IO) {
                    ApkPackagerEngine.buildApk(
                        context = context,
                        projectInfo = info,
                        projectDir = dir,
                        options = _buildOptions.value,
                        onProgress = { step, progress, task ->
                            _uiState.value = BuildUiState.Compiling(step, progress, task)
                        },
                        onLog = { log ->
                            _logs.value = _logs.value + log
                        }
                    )
                }

                _builtApks.value = listOf(builtApk) + _builtApks.value.filter { it.filePath != builtApk.filePath }
                _uiState.value = BuildUiState.Completed(builtApk)
                addLog("APK erfolgreich generiert und in Bibliothek gespeichert!", LogType.SUCCESS)
            } catch (e: Exception) {
                addLog("Build-Fehler: ${e.message}", LogType.ERROR)
                _uiState.value = BuildUiState.Failed("Build fehlgeschlagen", e.localizedMessage)
            }
        }
    }

    fun installApk(context: Context, apk: BuiltApk) {
        try {
            val file = File(apk.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "APK-Datei existiert nicht mehr.", Toast.LENGTH_SHORT).show()
                return
            }

            // On Android 8.0+ (Oreo), verify permission to install unknown apps
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Bitte erlaube 'Apps aus dieser Quelle installieren' für den APK Builder.",
                        Toast.LENGTH_LONG
                    ).show()
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Installationsfehler: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareApk(context: Context, apk: BuiltApk) {
        try {
            val file = File(apk.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "APK-Datei nicht gefunden.", Toast.LENGTH_SHORT).show()
                return
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "APK: ${apk.fileName}")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(shareIntent, "APK teilen"))
        } catch (e: Exception) {
            Toast.makeText(context, "Fehler beim Teilen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteApk(apk: BuiltApk) {
        val file = File(apk.filePath)
        if (file.exists()) file.delete()
        _builtApks.value = _builtApks.value.filter { it.id != apk.id }
        if (_uiState.value is BuildUiState.Completed && (_uiState.value as BuildUiState.Completed).apk.id == apk.id) {
            currentProjectInfo?.let { _uiState.value = BuildUiState.ProjectReady(it) }
        }
    }

    private fun loadExistingApks() {
        val context = getApplication<Application>()
        val apkDir = File(context.filesDir, "apks")
        if (!apkDir.exists()) return

        val list = mutableListOf<BuiltApk>()
        apkDir.listFiles()?.filter { it.extension == "apk" }?.forEach { file ->
            val sha256 = try {
                val digest = MessageDigest.getInstance("SHA-256")
                FileInputStream(file).use { fis ->
                    val buf = ByteArray(8192)
                    var len: Int
                    while (fis.read(buf).also { len = it } > 0) digest.update(buf, 0, len)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (_: Exception) { "Unbekannt" }

            list.add(
                BuiltApk(
                    fileName = file.name,
                    packageName = "com.android.app",
                    versionName = "1.0",
                    fileSizeBytes = file.length(),
                    filePath = file.absolutePath,
                    timestamp = file.lastModified(),
                    sha256 = sha256,
                    isSigned = true
                )
            )
        }
        _builtApks.value = list.sortedByDescending { it.timestamp }
    }
}
