package com.aiapkbuilder.app.util.generators

import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.util.safeExecute

/**
 * Generates Kotlin Compose code for screens based on blueprints.
 */
class ComposeScreenGenerator {

    /**
     * Generates a complete Compose screen from a blueprint.
     */
    fun generateScreen(
        blueprint: ScreenBlueprint,
        packageName: String,
        viewModelName: String,
        colorScheme: ColorScheme
    ): Result<String> = safeExecute {
        val imports = generateImports(blueprint)
        val composableFunction = generateComposableFunction(blueprint, viewModelName)
        val previewFunction = generatePreviewFunction(blueprint, colorScheme)

        """
package $packageName.ui.screens

$imports

@Composable
fun ${blueprint.name}(
    viewModel: $viewModelName = hiltViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ${generateScreenContent(blueprint)}
}

$previewFunction
        """.trimIndent()
    }

    private fun generateImports(blueprint: ScreenBlueprint): String {
        val imports = mutableSetOf(
            "import androidx.compose.foundation.layout.*",
            "import androidx.compose.material3.*",
            "import androidx.compose.runtime.*",
            "import androidx.compose.ui.*",
            "import androidx.compose.ui.tooling.preview.Preview",
            "import androidx.hilt.navigation.compose.hiltViewModel",
            "import androidx.navigation.compose.rememberNavController",
            "import androidx.navigation.NavHostController"
        )

        // Add component-specific imports
        blueprint.components.forEach { component ->
            when (component.type) {
                ComponentType.BUTTON -> {
                    imports.add("import androidx.compose.material3.Button")
                    imports.add("import androidx.compose.material3.OutlinedButton")
                }
                ComponentType.TEXT -> {
                    imports.add("import androidx.compose.material3.Text")
                }
                ComponentType.LIST -> {
                    imports.add("import androidx.compose.foundation.lazy.LazyColumn")
                    imports.add("import androidx.compose.foundation.lazy.items")
                }
                ComponentType.CARD -> {
                    imports.add("import androidx.compose.material3.Card")
                    imports.add("import androidx.compose.material3.CardDefaults")
                }
                ComponentType.BAR -> {
                    imports.add("import androidx.compose.material3.TopAppBar")
                    imports.add("import androidx.compose.material3.ExperimentalMaterial3Api")
                }
                ComponentType.FORM -> {
                    imports.add("import androidx.compose.material3.TextField")
                    imports.add("import androidx.compose.material3.OutlinedTextField")
                }
                ComponentType.IMAGE -> {
                    imports.add("import androidx.compose.foundation.Image")
                    imports.add("import coil.compose.rememberAsyncImagePainter")
                }
                ComponentType.CUSTOM -> {
                    // Add custom imports as needed
                }
            }
        }

        return imports.sorted().joinToString("\n")
    }

    private fun generateComposableFunction(
        blueprint: ScreenBlueprint,
        viewModelName: String
    ): String {
        return """
@Composable
fun ${blueprint.name}(
    viewModel: $viewModelName = hiltViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ${generateScreenContent(blueprint)}
}
        """.trimIndent()
    }

    private fun generateScreenContent(blueprint: ScreenBlueprint): String {
        val content = StringBuilder()

        content.append("Scaffold(\n")
        content.append("    topBar = {\n")
        content.append("        ${generateTopBar(blueprint)}\n")
        content.append("    },\n")
        content.append("    floatingActionButton = {\n")
        content.append("        ${generateFab(blueprint)}\n")
        content.append("    }\n")
        content.append(") { padding ->\n")
        content.append("    Column(\n")
        content.append("        modifier = Modifier\n")
        content.append("            .fillMaxSize()\n")
        content.append("            .padding(padding)\n")
        content.append("            .padding(16.dp),\n")
        content.append("        verticalArrangement = Arrangement.spacedBy(16.dp)\n")
        content.append("    ) {\n")

        blueprint.components.forEach { component ->
            if (component.name != "TopAppBar" && component.name != "FloatingActionButton") {
                content.append("        ${generateComponent(component)}\n")
            }
        }

        content.append("    }\n")
        content.append("}")

        return content.toString()
    }

    private fun generateTopBar(blueprint: ScreenBlueprint): String {
        val topBarComponent = blueprint.components.find { it.name.contains("TopAppBar") }
        return if (topBarComponent != null) {
            """
TopAppBar(
    title = { Text("${blueprint.name.replace("Screen", "")}") },
    navigationIcon = {
        IconButton(onClick = { navController.navigateUp() }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
    }
)
            """.trimIndent()
        } else {
            "TopAppBar(title = { Text(\"${blueprint.name.replace("Screen", "")}\") })"
        }
    }

    private fun generateFab(blueprint: ScreenBlueprint): String {
        val fabComponent = blueprint.components.find { it.name.contains("FloatingActionButton") }
        return if (fabComponent != null) {
            """
FloatingActionButton(onClick = { /* Handle FAB click */ }) {
    Icon(Icons.Default.Add, contentDescription = "Add")
}
            """.trimIndent()
        } else {
            "{}"
        }
    }

    private fun generateComponent(component: ComponentSpec): String {
        return when (component.type) {
            ComponentType.BUTTON -> generateButton(component)
            ComponentType.TEXT -> generateText(component)
            ComponentType.LIST -> generateList(component)
            ComponentType.CARD -> generateCard(component)
            ComponentType.FORM -> generateForm(component)
            ComponentType.IMAGE -> generateImage(component)
            ComponentType.CUSTOM -> generateCustomComponent(component)
            else -> "// TODO: Implement ${component.name}"
        }
    }

    private fun generateButton(component: ComponentSpec): String {
        val text = component.properties["text"] ?: "Button"
        val onClick = component.eventHandlers.firstOrNull()?.handlerCode ?: "{}"

        return """
Button(
    onClick = { $onClick },
    modifier = Modifier.fillMaxWidth()
) {
    Text("$text")
}
        """.trimIndent()
    }

    private fun generateText(component: ComponentSpec): String {
        val text = component.properties["text"] ?: "Sample Text"
        val style = component.properties["style"] ?: "MaterialTheme.typography.bodyLarge"

        return """
Text(
    text = "$text",
    style = $style
)
        """.trimIndent()
    }

    private fun generateList(component: ComponentSpec): String {
        return """
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(uiState.items) { item ->
        ${generateListItem(component)}
    }
}
        """.trimIndent()
    }

    private fun generateListItem(component: ComponentSpec): String {
        return """
Card(
    modifier = Modifier.fillMaxWidth(),
    onClick = { /* Handle item click */ }
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = item.title, style = MaterialTheme.typography.titleMedium)
        Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
    }
}
        """.trimIndent()
    }

    private fun generateCard(component: ComponentSpec): String {
        return """
Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Card Title",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Card content goes here",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
        """.trimIndent()
    }

    private fun generateForm(component: ComponentSpec): String {
        return """
var textValue by remember { mutableStateOf("") }

OutlinedTextField(
    value = textValue,
    onValueChange = { textValue = it },
    label = { Text("Input Field") },
    modifier = Modifier.fillMaxWidth()
)
        """.trimIndent()
    }

    private fun generateImage(component: ComponentSpec): String {
        return """
Image(
    painter = rememberAsyncImagePainter("https://via.placeholder.com/150"),
    contentDescription = "Image",
    modifier = Modifier
        .size(100.dp)
        .clip(CircleShape)
)
        """.trimIndent()
    }

    private fun generateCustomComponent(component: ComponentSpec): String {
        return """
// Custom component: ${component.name}
// TODO: Implement custom component
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
        .background(Color.LightGray, RoundedCornerShape(8.dp)),
    contentAlignment = Alignment.Center
) {
    Text("${component.name}")
}
        """.trimIndent()
    }

    private fun generatePreviewFunction(
        blueprint: ScreenBlueprint,
        colorScheme: ColorScheme
    ): String {
        return """
@Preview(showBackground = true)
@Composable
fun ${blueprint.name}Preview() {
    AIAPKBuilderTheme {
        ${blueprint.name}()
    }
}
        """.trimIndent()
    }
}