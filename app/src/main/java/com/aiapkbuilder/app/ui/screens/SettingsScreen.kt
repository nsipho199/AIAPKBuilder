package com.aiapkbuilder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aiapkbuilder.app.data.model.AIProvider
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showApiKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SettingsSection(title = "AI Provider") {
                    Text("Provider", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    AIProvider.entries.forEach { provider ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.aiSettings.provider == provider,
                                onClick = { viewModel.setAIProvider(provider) }
                            )
                            Text(provider.displayName)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.aiSettings.apiKey,
                        onValueChange = viewModel::setApiKey,
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    null
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.aiSettings.model,
                        onValueChange = viewModel::setAIModel,
                        label = { Text("Model") },
                        placeholder = { Text("gpt-4o / llama3 / gemma2...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (uiState.aiSettings.provider == AIProvider.OLLAMA ||
                        uiState.aiSettings.provider == AIProvider.CUSTOM) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.aiSettings.baseUrl,
                            onValueChange = viewModel::setBaseUrl,
                            label = { Text("Base URL") },
                            placeholder = { Text("http://localhost:11434/v1/") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "GitHub Actions") {
                    OutlinedTextField(
                        value = uiState.buildSettings.githubUsername,
                        onValueChange = viewModel::setGithubUsername,
                        label = { Text("GitHub Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Person, null) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.buildSettings.githubToken,
                        onValueChange = viewModel::setGithubToken,
                        label = { Text("GitHub Personal Access Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Filled.Key, null) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                SettingsSection(title = "Codemagic") {
                    OutlinedTextField(
                        value = uiState.buildSettings.codemagicApiKey,
                        onValueChange = viewModel::setCodemagicKey,
                        label = { Text("Codemagic API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Filled.Key, null) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                SettingsSection(title = "Self-Hosted Build") {
                    OutlinedTextField(
                        value = uiState.buildSettings.selfHostedEndpoint,
                        onValueChange = viewModel::setSelfHostedEndpoint,
                        label = { Text("Build Server URL") },
                        placeholder = { Text("https://your-server.com/build") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Storage, null) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                SettingsSection(title = "App") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Mode", style = MaterialTheme.typography.bodyMedium)
                            Text("Follow system theme", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = uiState.darkMode, onCheckedChange = viewModel::setDarkMode)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Build", style = MaterialTheme.typography.bodyMedium)
                            Text("Start build right after generation", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = uiState.autoBuild, onCheckedChange = viewModel::setAutoBuild)
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::saveSettings,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Settings", fontWeight = FontWeight.SemiBold)
                }
                if (uiState.savedSuccess) {
                    Spacer(Modifier.height(8.dp))
                    Text("Settings saved!", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "AI APK Builder v1.0.0 • Open Source",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), content = content)
        }
    }
}
