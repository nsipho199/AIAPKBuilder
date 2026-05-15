package com.aiapkbuilder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aiapkbuilder.app.viewmodel.BuildLogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildLogScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    viewModel: BuildLogViewModel = hiltViewModel()
) {
    LaunchedEffect(projectId) { viewModel.loadLogs(projectId) }
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.logLines.size) {
        if (uiState.logLines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.logLines.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build Logs", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::copyLogs) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Logs")
                    }
                    IconButton(onClick = viewModel::refreshLogs) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress bar when building
            if (uiState.isBuilding) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Terminal output
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117))
                    .padding(12.dp)
            ) {
                if (uiState.logLines.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Terminal,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF58A6FF)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Waiting for build output...",
                            color = Color(0xFF8B949E),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item {
                            Text(
                                "$ AI APK Builder — Build Log",
                                color = Color(0xFF58A6FF),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "─".repeat(50),
                                color = Color(0xFF30363D),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                        items(uiState.logLines) { line ->
                            Text(
                                text = line.text,
                                color = when {
                                    line.text.contains("ERROR", ignoreCase = true) -> Color(0xFFFF7B72)
                                    line.text.contains("WARN", ignoreCase = true) -> Color(0xFFE3B341)
                                    line.text.contains("SUCCESS") || line.text.contains("BUILD SUCCESSFUL") -> Color(0xFF3FB950)
                                    line.text.startsWith("$") -> Color(0xFF58A6FF)
                                    else -> Color(0xFFCDD9E5)
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        if (uiState.isBuilding) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.5.dp,
                                        color = Color(0xFF58A6FF)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Building...", color = Color(0xFF8B949E), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
