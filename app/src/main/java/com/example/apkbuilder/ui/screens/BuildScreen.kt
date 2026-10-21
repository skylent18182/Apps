package com.example.apkbuilder.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apkbuilder.model.BuildLog
import com.example.apkbuilder.model.LogType
import com.example.apkbuilder.viewmodel.ApkBuilderViewModel
import com.example.ui.theme.*

@Composable
fun BuildScreen(
    viewModel: ApkBuilderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()

    var filterText by remember { mutableStateOf("") }
    val filteredLogs = remember(logs, filterText) {
        if (filterText.isBlank()) logs
        else logs.filter { it.message.contains(filterText, ignoreCase = true) }
    }

    // Auto-scroll when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (logs.isNotEmpty()) TerminalGreen else TerminalDim)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Terminal-Konsole",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${logs.size} Zeilen",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val fullLogText = logs.joinToString("\n") { "[${it.timestamp}] ${it.message}" }
                    clipboard.setPrimaryClip(ClipData.newPlainText("Build Logs", fullLogText))
                    Toast.makeText(context, "Logs kopiert", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Logs kopieren", modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = { viewModel.clearLogs() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Logs leeren", modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            placeholder = { Text("Filter logs...", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (filterText.isNotEmpty()) {
                    IconButton(onClick = { filterText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Terminal Window
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TerminalBg)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (logs.isEmpty()) "Keine Logs vorhanden. Starte einen Build." else "Keine Treffer für Filter.",
                        color = TerminalDim,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        LogLineItem(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun LogLineItem(log: BuildLog) {
    val textColor = when (log.type) {
        LogType.SUCCESS -> TerminalGreen
        LogType.ERROR -> TerminalRed
        LogType.WARN -> TerminalYellow
        LogType.STEP -> TerminalCyan
        LogType.COMMAND -> SkyHighlight
        LogType.INFO -> Color(0xFFE2E8F0)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = log.timestamp,
            color = TerminalDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = log.message,
            color = textColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp
        )
    }
}
