package com.aiapkbuilder.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aiapkbuilder.app.data.model.AppType
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.viewmodel.GenerateViewModel
import com.aiapkbuilder.app.viewmodel.GenerateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    onNavigateToProject: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFeatures: () -> Unit,
    viewModel: GenerateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.createdProjectId) {
        uiState.createdProjectId?.let { id ->
            onNavigateToProject(id)
            viewModel.clearCreatedProject()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate App", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Tune, contentDescription = "AI Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Prompt Input
                OutlinedTextField(
                    value = uiState.prompt,
                    onValueChange = viewModel::onPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Describe your app idea") },
                    placeholder = { Text("e.g. Build a weather app with 5-day forecast and location detection") },
                    minLines = 4,
                    maxLines = 8,
                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.prompt.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onPromptChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            item {
                // Prompt Suggestions
                Text("Quick Start Ideas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(promptSuggestions) { suggestion ->
                        SuggestionChip(
                            text = suggestion,
                            onClick = { viewModel.onPromptChange(suggestion) }
                        )
                    }
                }
            }

            item {
                Text("App Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppType.entries) { type ->
                        FilterChip(
                            selected = uiState.selectedAppType == type,
                            onClick = { viewModel.onAppTypeSelected(type) },
                            label = { Text(type.displayName) }
                        )
                    }
                }
            }

            item {
                Text("Build Provider", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BuildProvider.entries.forEach { provider ->
                        BuildProviderOption(
                            provider = provider,
                            selected = uiState.selectedProvider == provider,
                            onSelect = { viewModel.onProviderSelected(provider) }
                        )
                    }
                }
            }

            item {
                // Additional Features
                Text("Additional Features", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableFeatures) { feature ->
                        val selected = feature in uiState.selectedFeatures
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.onFeatureToggled(feature) },
                            label = { Text(feature) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onNavigateToFeatures,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Customize Features")
                }
            }

            item {
                // Error state
                AnimatedVisibility(visible = uiState.error != null) {
                    uiState.error?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            item {
                // Generate button
                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.generateApp()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = uiState.prompt.isNotBlank() && !uiState.isLoading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(uiState.loadingMessage)
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate & Build APK", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    GenerationProgressCard(message = uiState.loadingMessage, progress = uiState.progress)
                }
            }
        }
    }
}

@Composable
private fun BuildProviderOption(
    provider: BuildProvider,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    buildProviderDescription(provider),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GenerationProgressCard(message: String, progress: Float) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Generating your app...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun buildProviderDescription(provider: BuildProvider) = when (provider) {
    BuildProvider.LOCAL -> "Generate project files locally on your device"
    BuildProvider.GITHUB_ACTIONS -> "Compile APK via GitHub Actions CI/CD (requires token)"
    BuildProvider.CODEMAGIC -> "Cloud build using Codemagic (requires API key)"
    BuildProvider.DOCKER -> "Self-hosted Docker-based build environment"
    BuildProvider.SELF_HOSTED -> "Your own build server endpoint"
    BuildProvider.COMMUNITY -> "Shared community build nodes (free, slower)"
}

private val availableFeatures = listOf(
    "Dark Mode", "Authentication", "Database", "REST API",
    "Push Notifications", "Offline Support", "Maps Integration",
    "Camera", "QR Scanner", "Biometric Auth", "Analytics",
    "Social Login", "Payment Gateway", "Real-time Updates"
)

private val promptSuggestions = listOf(
    "Create a task management app with categories and due dates",
    "Build a recipe finder with ingredient search and meal planning",
    "Make a fitness tracker with workout logs and progress charts",
    "Develop a budget app with expense tracking and financial goals",
    "Design a travel journal with photos, maps, and trip notes",
    "Build a habit tracker with streaks and motivational reminders",
    "Create a music player with playlists and offline downloads",
    "Make a shopping list app with barcode scanning",
    "Develop a language learning app with flashcards and quizzes",
    "Build a meditation app with guided sessions and progress tracking"
)

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text, maxLines = 2) },
        modifier = Modifier.widthIn(max = 200.dp)
    )
}