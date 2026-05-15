package com.aiapkbuilder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aiapkbuilder.app.data.model.AppType

data class AppTemplate(
    val type: AppType,
    val prompt: String,
    val complexity: String
)

private val templates = AppType.entries.map { type ->
    AppTemplate(
        type = type,
        prompt = defaultPromptFor(type),
        complexity = complexityFor(type)
    )
}

private fun defaultPromptFor(type: AppType) = when (type) {
    AppType.CALCULATOR -> "Build a scientific calculator app with history log and unit conversion"
    AppType.NOTES -> "Create a rich notes app with markdown support, tags, search, and cloud sync"
    AppType.CHAT -> "Build a real-time chat app with rooms, media sharing, and push notifications"
    AppType.ECOMMERCE -> "Create a full e-commerce app with product catalog, cart, checkout, and order tracking"
    AppType.DELIVERY -> "Build a food delivery app with restaurant listing, ordering, and live tracking"
    AppType.TAXI -> "Create a taxi booking app with map integration, driver matching, and fare estimation"
    AppType.SCHOOL -> "Build a school management system with attendance, grades, timetable, and announcements"
    AppType.AI_ASSISTANT -> "Create an AI chatbot assistant with voice input, history, and multiple AI models"
    AppType.FINANCE -> "Build a personal finance app with expense tracking, budgets, charts, and reports"
    AppType.PRODUCTIVITY -> "Create a task management app with projects, due dates, reminders, and team collaboration"
    AppType.PORTFOLIO -> "Build a portfolio app showcasing projects, skills, contact form, and resume download"
    AppType.BUSINESS -> "Create a CRM app with contacts, deals pipeline, notes, and activity tracking"
    AppType.STREAMING -> "Build a video streaming UI with categories, search, player, and favorites"
    AppType.DASHBOARD -> "Create an analytics dashboard with charts, KPIs, filters, and data export"
    AppType.WEATHER -> "Build a weather app with 7-day forecast, radar map, and severe weather alerts"
    AppType.FITNESS -> "Create a fitness tracker with workout logging, progress charts, and meal planner"
    AppType.SOCIAL -> "Build a social app with feed, profiles, posts, stories, and messaging"
    AppType.CUSTOM -> "Describe your custom app idea in detail..."
}

private fun complexityFor(type: AppType) = when (type) {
    AppType.CALCULATOR, AppType.NOTES, AppType.WEATHER -> "Simple"
    AppType.CHAT, AppType.FINANCE, AppType.PRODUCTIVITY, AppType.PORTFOLIO, AppType.FITNESS -> "Medium"
    else -> "Advanced"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onNavigateToGenerate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = templates.filter {
        searchQuery.isEmpty() || it.type.displayName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Templates", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search templates...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { template ->
                    TemplateCard(
                        template = template,
                        onClick = { onNavigateToGenerate(template.prompt) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: AppTemplate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                Icons.Filled.Android,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(template.type.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            AssistChip(
                onClick = {},
                label = { Text(template.complexity, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                template.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
        }
    }
}
