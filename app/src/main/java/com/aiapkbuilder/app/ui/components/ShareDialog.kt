package com.aiapkbuilder.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aiapkbuilder.app.data.model.ShareConfig
import com.aiapkbuilder.app.data.model.ShareLink
import com.aiapkbuilder.app.data.model.ShareType

@Composable
fun ShareDialog(
    onDismiss: () -> Unit,
    onGenerateLink: (ShareConfig) -> Unit,
    onShareViaIntent: () -> Unit,
    shareLink: ShareLink? = null,
    shareUrl: String = "",
    qrCodeBitmap: Bitmap? = null,
    isGenerating: Boolean = false
) {
    var expirationDays by remember { mutableIntStateOf(7) }
    var password by remember { mutableStateOf("") }
    var maxDownloads by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Share, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Share", fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            if (shareLink != null && qrCodeBitmap != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(180.dp)
                    )
                    Text(
                        shareUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { onShareViaIntent() },
                            label = { Text("Share") },
                            leadingIcon = { Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp)) }
                        )
                        AssistChip(
                            onClick = { /* copy to clipboard */ },
                            label = { Text("Copy") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    if (shareLink.expiresAt != null) {
                        val daysLeft = (shareLink.expiresAt - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                        Text(
                            "Expires in $daysLeft days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Downloads: ${shareLink.downloadCount} · Views: ${shareLink.viewCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Create a shareable link for this build",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("Link Expiration", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "1 Day", 7 to "7 Days", 30 to "30 Days", 0 to "Never").forEach { (days, label) ->
                            FilterChip(
                                selected = expirationDays == days,
                                onClick = { expirationDays = days },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Lock, null) }
                    )

                    OutlinedTextField(
                        value = maxDownloads,
                        onValueChange = { maxDownloads = it.filter { c -> c.isDigit() } },
                        label = { Text("Max downloads (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Download, null) }
                    )

                    Button(
                        onClick = {
                            onGenerateLink(ShareConfig(
                                artifactId = "",
                                shareType = ShareType.LINK,
                                expirationDays = if (expirationDays == 0) null else expirationDays,
                                password = password.ifEmpty { null },
                                maxDownloads = maxDownloads.toIntOrNull()
                            ))
                        },
                        enabled = !isGenerating,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Generate Link")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (shareLink != null) "Close" else "Cancel")
            }
        }
    )
}
