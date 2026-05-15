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
import com.aiapkbuilder.app.data.model.BuildStatus
import com.aiapkbuilder.app.data.model.ExportConfig
import com.aiapkbuilder.app.data.model.GeneratedProjectPlan
import com.aiapkbuilder.app.data.model.ShareConfig
import com.aiapkbuilder.app.ui.components.ExportDialog
import com.aiapkbuilder.app.ui.components.ShareDialog
import com.aiapkbuilder.app.viewmodel.ProjectDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToBuildLog: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToHistory: (String) -> Unit = {},
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(projectId) { viewModel.loadProject(projectId) }
    val uiState by viewModel.uiState.collectAsState()
    val project = uiState.project

    var showExportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { config ->
                viewModel.exportProject(config.copy(projectId = projectId))
                showExportDialog = false
            },
            isExporting = uiState.isExporting
        )
    }

    if (showShareDialog) {
        ShareDialog(
            onDismiss = { showShareDialog = false },
            onGenerateLink = { config ->
                viewModel.generateShareLink(config.copy(artifactId = projectId))
            },
            onShareViaIntent = { viewModel.shareProject() },
            shareLink = uiState.generatedShareLink,
            shareUrl = uiState.shareUrl,
            qrCodeBitmap = uiState.qrCodeBitmap,
            isGenerating = uiState.isGeneratingShareLink
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Project", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToBuildLog) {
                        Icon(Icons.Filled.Terminal, contentDescription = "Build Logs")
                    }
                    IconButton(onClick = onNavigateToDownloads) {
                        Icon(Icons.Filled.Download, contentDescription = "Downloads")
                    }
                    if (project?.buildStatus == BuildStatus.SUCCESS) {
                        IconButton(onClick = { showShareDialog = true }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (project == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (project.buildStatus) {
                            BuildStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            BuildStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BuildStatusBadge(project.buildStatus)
                            Spacer(Modifier.weight(1f))
                            if (project.buildStatus == BuildStatus.BUILDING || project.buildStatus == BuildStatus.GENERATING) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(project.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoChip(Modifier.weight(1f), "Type", project.appType.displayName, Icons.Filled.Category)
                    InfoChip(Modifier.weight(1f), "Version", project.versionName, Icons.Filled.Tag)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoChip(Modifier.weight(1f), "Min SDK", "API ${project.minSdk}", Icons.Filled.Android)
                    InfoChip(Modifier.weight(1f), "Provider", project.buildProvider.displayName, Icons.Filled.Cloud)
                }
            }

            if (project.features.isNotEmpty()) {
                item {
                    Text("Features", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    project.features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(feature, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (project.screens.isNotEmpty()) {
                item {
                    Text("Screens", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    project.screens.forEach { screen ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(Icons.Filled.Smartphone, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(screen, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            project.generatedProjectPlan?.let { plan ->
                item {
                    Text("Generated Code Structure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    CodeStructureCard(plan)
                }
            }

            if (project.buildStatus == BuildStatus.SUCCESS) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.downloadApk() },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Download, null)
                            Spacer(Modifier.width(6.dp))
                            Text("APK", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FolderZip, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.shareProject() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Share, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                        OutlinedButton(
                            onClick = { viewModel.recordBuildHistory() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.History, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Save Build")
                        }
                    }
                }
            }

            if (project.buildStatus == BuildStatus.FAILED) {
                item {
                    project.errorMessage?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Build Error", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.height(4.dp))
                                Text(err, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.retryBuild() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Build")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onNavigateToBuildLog,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Terminal, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Build Logs")
                    }
                    OutlinedButton(
                        onClick = { onNavigateToHistory(projectId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.History, null)
                        Spacer(Modifier.width(6.dp))
                        Text("History")
                    }
                }
            }

            if (uiState.isBuilding || uiState.buildProgress > 0f) {
                item {
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Build Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uiState.buildProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(uiState.buildMessage, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CodeStructureCard(plan: GeneratedProjectPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Project Structure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))

            // Main Activity
            CodeFileItem("MainActivity.kt", "Entry point with navigation setup")

            // Screens
            if (plan.screens.isNotEmpty()) {
                Text("Screens/", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary)
                plan.screens.forEach { screen ->
                    CodeFileItem("  ${screen.name}Screen.kt", screen.description)
                }
            }

            // ViewModels
            Text("ViewModels/", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary)
            plan.screens.forEach { screen ->
                CodeFileItem("  ${screen.name}ViewModel.kt", "Business logic for ${screen.name}")
            }

            // Data Layer
            Text("Data/", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary)
            CodeFileItem("  AppDatabase.kt", "Room database configuration")
            CodeFileItem("  Models.kt", "Data models and entities")
            CodeFileItem("  Repository.kt", "Data access layer")

            // Dependencies
            Text("Dependencies", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary)
            CodeFileItem("  Compose BOM", "UI framework")
            CodeFileItem("  Room", "Database")
            CodeFileItem("  Hilt", "Dependency injection")
            if (plan.features.contains("Database")) {
                CodeFileItem("  Room KTX", "Database extensions")
            }
        }
    }
}

@Composable
private fun CodeFileItem(filename: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            filename,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}