package com.example.apkbuilder.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.apkbuilder.viewmodel.ApkBuilderViewModel
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: ApkBuilderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val buildOptions by viewModel.buildOptions.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                "Einstellungen & Build-Engine",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Konfiguration für APK-Generierung und Signatur",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Keystore Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Signaturschlüssel (Keystore)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "APKs werden automatisch mit einem generierten RSA-2048 Bit Schlüssel und SHA-256 Digest nach dem Standard-Android v1 (JAR) Signaturschema signiert, damit sie auf Geräten installiert werden können.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Signierung aktiv", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = buildOptions.signWithDebugKey,
                            onCheckedChange = { viewModel.updateBuildOptions(buildOptions.copy(signWithDebugKey = it)) }
                        )
                    }
                }
            }
        }

        // Optimizer Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("APK Optimierung & Zipalign", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Richtet ungekomprimierte Ressourcen an 4-Byte Grenzen aus, um die Speichernutzung zur Laufzeit durch Android mmap() zu minimieren.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Zipalign-Komprimierungsstufe", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = buildOptions.zipalign,
                            onCheckedChange = { viewModel.updateBuildOptions(buildOptions.copy(zipalign = it)) }
                        )
                    }
                }
            }
        }

        // Maintenance Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Speicher & Bereinigung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Löscht temporär entpackte Projektdateien aus dem App-Cache.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                File(context.cacheDir, "current_project").deleteRecursively()
                                File(context.cacheDir, "imported_project").deleteRecursively()
                                Toast.makeText(context, "Cache erfolgreich geleert", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Temporären Cache leeren")
                    }
                }
            }
        }

        // About Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Über APK Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Version 1.0.0 • On-Device APK Compiler Engine\nKomplett offline nutzbar, keine Registrierung oder Cloud erforderlich.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
