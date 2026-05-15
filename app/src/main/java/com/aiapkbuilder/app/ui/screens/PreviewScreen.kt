package com.aiapkbuilder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aiapkbuilder.app.data.model.GeneratedProjectPlan
import com.aiapkbuilder.app.viewmodel.GenerateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onNavigateBack: () -> Unit,
    onGenerate: () -> Unit,
    viewModel: GenerateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val previewPlan by viewModel.previewPlan.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Preview", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit Features")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onGenerate,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Generate App")
                    }
                }
            }
        }
    ) { padding ->
        if (previewPlan == null) {
            // Loading preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Generating preview...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PreviewHeader(plan = previewPlan!!)
                }

                item {
                    PreviewSection(title = "App Screens", icon = Icons.Filled.Smartphone) {
                        ScreenPreviewList(previewPlan!!.screens)
                    }
                }

                item {
                    PreviewSection(title = "Features", icon = Icons.Filled.Star) {
                        FeaturePreviewList(previewPlan!!.features)
                    }
                }

                item {
                    PreviewSection(title = "Technical Stack", icon = Icons.Filled.Code) {
                        TechnicalPreview(plan = previewPlan!!)
                    }
                }

                item {
                    PreviewSection(title = "Estimated Build Time", icon = Icons.Filled.Schedule) {
                        BuildTimeEstimate()
                    }
                }
            }
        }
    }

    // Load preview when screen opens
    LaunchedEffect(Unit) {
        viewModel.generatePreview()
    }
}

@Composable
private fun PreviewHeader(plan: GeneratedProjectPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    plan.appName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                plan.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Package,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    plan.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PreviewSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ScreenPreviewList(screens: List<com.aiapkbuilder.app.data.model.Screen>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        screens.forEach { screen ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Web, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(screen.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Text(
                            screen.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePreviewList(features: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        features.forEach { feature ->
            AssistChip(
                onClick = {},
                label = { Text(feature) }
            )
        }
    }
}

@Composable
private fun TechnicalPreview(plan: GeneratedProjectPlan) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("• Kotlin + Jetpack Compose", style = MaterialTheme.typography.bodyMedium)
        Text("• MVVM Architecture", style = MaterialTheme.typography.bodyMedium)
        Text("• Room Database", style = MaterialTheme.typography.bodyMedium)
        Text("• Hilt Dependency Injection", style = MaterialTheme.typography.bodyMedium)
        Text("• Material3 Design", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BuildTimeEstimate() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("~3-5 minutes", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Text("(depending on build provider)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}