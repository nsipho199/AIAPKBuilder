package com.aiapkbuilder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aiapkbuilder.app.data.model.DownloadProgress
import com.aiapkbuilder.app.data.model.DownloadSession
import com.aiapkbuilder.app.data.model.DownloadStatus
import com.aiapkbuilder.app.viewmodel.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearCompleted() }) {
                        Icon(Icons.Filled.ClearAll, contentDescription = "Clear Completed")
                    }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No downloads yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Build an APK and download it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sessions, key = { it.sessionId }) { session ->
                DownloadSessionCard(
                    session = session,
                    progress = activeDownloads[session.sessionId],
                    onPause = { viewModel.pauseDownload(session.sessionId) },
                    onResume = { viewModel.resumeDownload(session.sessionId) },
                    onCancel = { viewModel.cancelDownload(session.sessionId) },
                    onDelete = { viewModel.deleteSession(session.sessionId) }
                )
            }
        }
    }
}

@Composable
private fun DownloadSessionCard(
    session: DownloadSession,
    progress: DownloadProgress?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (session.status) {
                DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (session.status) {
                        DownloadStatus.COMPLETED -> Icons.Filled.CheckCircle
                        DownloadStatus.FAILED -> Icons.Filled.Error
                        DownloadStatus.ACTIVE -> Icons.Filled.Downloading
                        DownloadStatus.PAUSED -> Icons.Filled.PauseCircle
                        else -> Icons.Filled.HourglassEmpty
                    },
                    contentDescription = null,
                    tint = when (session.status) {
                        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        when (session.status) {
                            DownloadStatus.COMPLETED -> "Completed"
                            DownloadStatus.FAILED -> session.errorMessage ?: "Failed"
                            DownloadStatus.ACTIVE -> "Downloading..."
                            DownloadStatus.PAUSED -> "Paused"
                            DownloadStatus.PENDING -> "Queued"
                            DownloadStatus.CANCELLED -> "Cancelled"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                when (session.status) {
                    DownloadStatus.ACTIVE -> {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Filled.Pause, "Pause")
                        }
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, "Cancel")
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Filled.PlayArrow, "Resume")
                        }
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, "Cancel")
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, "Delete")
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, "Delete")
                        }
                    }
                    else -> {}
                }
            }

            if (session.status == DownloadStatus.ACTIVE || session.status == DownloadStatus.PAUSED) {
                Spacer(Modifier.height(10.dp))
                val p = progress ?: DownloadProgress(
                    sessionId = session.sessionId,
                    fileName = session.fileName,
                    downloadedSize = session.downloadedSize,
                    totalSize = session.totalSize,
                    progressPercent = if (session.totalSize > 0) ((session.downloadedSize * 100) / session.totalSize).toInt() else 0,
                    speed = session.speed,
                    etaSeconds = session.etaMs / 1000,
                    status = session.status
                )
                LinearProgressIndicator(
                    progress = { p.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${formatBytes(p.downloadedSize)} / ${formatBytes(p.totalSize)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${p.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (p.speed > 0) {
                    Text(
                        "${formatBytes(p.speed)}/s · ${formatDuration(p.etaSeconds)} remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "calculating..."
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}
