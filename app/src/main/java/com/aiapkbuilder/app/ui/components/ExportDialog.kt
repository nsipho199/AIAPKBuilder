package com.aiapkbuilder.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aiapkbuilder.app.data.model.ExportConfig
import com.aiapkbuilder.app.data.model.ExportType

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportConfig) -> Unit,
    isExporting: Boolean = false
) {
    var exportType by remember { mutableStateOf(ExportType.BOTH) }
    var includeMetadata by remember { mutableStateOf(true) }
    var includeHistory by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    var compressionLevel by remember { mutableFloatStateOf(6f) }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FolderZip, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Export Project", fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Choose export options", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("Export Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = exportType == ExportType.SOURCE,
                        onClick = { exportType = ExportType.SOURCE },
                        label = { Text("Source") },
                        leadingIcon = { Icon(Icons.Filled.Code, null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = exportType == ExportType.BINARY,
                        onClick = { exportType = ExportType.BINARY },
                        label = { Text("APK/AAB") },
                        leadingIcon = { Icon(Icons.Filled.Android, null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = exportType == ExportType.BOTH,
                        onClick = { exportType = ExportType.BOTH },
                        label = { Text("Both") },
                        leadingIcon = { Icon(Icons.Filled.FolderZip, null, modifier = Modifier.size(16.dp)) }
                    )
                }

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File name (optional)") },
                    placeholder = { Text("Auto-generated") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null) }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeMetadata, onCheckedChange = { includeMetadata = it })
                    Spacer(Modifier.width(6.dp))
                    Text("Include metadata", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeHistory, onCheckedChange = { includeHistory = it })
                    Spacer(Modifier.width(6.dp))
                    Text("Include build history", style = MaterialTheme.typography.bodyMedium)
                }

                Text("Compression: ${compressionLevel.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = compressionLevel,
                    onValueChange = { compressionLevel = it },
                    valueRange = 1f..9f,
                    steps = 7
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Fast", style = MaterialTheme.typography.labelSmall)
                    Text("Max", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExport(ExportConfig(
                        projectId = "",
                        exportType = exportType,
                        includeMetadata = includeMetadata,
                        includeHistory = includeHistory,
                        compressionLevel = compressionLevel.toInt(),
                        fileName = fileName
                    ))
                },
                enabled = !isExporting,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExporting) {
                Text("Cancel")
            }
        }
    )
}
