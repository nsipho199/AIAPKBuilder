package com.aiapkbuilder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aiapkbuilder.app.viewmodel.GenerateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureSelectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreview: () -> Unit,
    viewModel: GenerateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Features", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToPreview) {
                        Text("Preview", fontWeight = FontWeight.Medium)
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
                Text(
                    "Select the features you want to include in your app",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text("Core Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(coreFeatures) { feature ->
                        FeatureCard(
                            feature = feature,
                            selected = feature.name in uiState.selectedFeatures,
                            onToggle = { viewModel.onFeatureToggled(feature.name) }
                        )
                    }
                }
            }

            item {
                Text("Advanced Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(advancedFeatures) { feature ->
                        FeatureCard(
                            feature = feature,
                            selected = feature.name in uiState.selectedFeatures,
                            onToggle = { viewModel.onFeatureToggled(feature.name) }
                        )
                    }
                }
            }

            item {
                Text("Integration Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(integrationFeatures) { feature ->
                        FeatureCard(
                            feature = feature,
                            selected = feature.name in uiState.selectedFeatures,
                            onToggle = { viewModel.onFeatureToggled(feature.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureOption,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                feature.icon,
                contentDescription = null,
                tint = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                feature.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

data class FeatureOption(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val coreFeatures = listOf(
    FeatureOption("Dark Mode", Icons.Filled.DarkMode),
    FeatureOption("Authentication", Icons.Filled.Person),
    FeatureOption("Database", Icons.Filled.Storage),
    FeatureOption("Offline Support", Icons.Filled.WifiOff)
)

private val advancedFeatures = listOf(
    FeatureOption("Push Notifications", Icons.Filled.Notifications),
    FeatureOption("Maps Integration", Icons.Filled.Map),
    FeatureOption("Camera", Icons.Filled.Camera),
    FeatureOption("QR Scanner", Icons.Filled.QrCodeScanner),
    FeatureOption("Biometric Auth", Icons.Filled.Fingerprint),
    FeatureOption("Analytics", Icons.Filled.Analytics),
    FeatureOption("Real-time Updates", Icons.Filled.Sync),
    FeatureOption("Payment Gateway", Icons.Filled.Payment)
)

private val integrationFeatures = listOf(
    FeatureOption("REST API", Icons.Filled.Api),
    FeatureOption("Social Login", Icons.Filled.Share),
    FeatureOption("Bluetooth", Icons.Filled.Bluetooth),
    FeatureOption("NFC", Icons.Filled.Nfc)
)