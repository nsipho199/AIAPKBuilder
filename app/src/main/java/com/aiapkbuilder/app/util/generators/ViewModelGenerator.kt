package com.aiapkbuilder.app.util.generators

import com.aiapkbuilder.app.data.model.ViewModelBlueprint
import com.aiapkbuilder.app.util.safeExecute

/**
 * Generates Kotlin ViewModel code based on blueprints.
 */
class ViewModelGenerator {

    /**
     * Generates a complete ViewModel class from a blueprint.
     */
    fun generateViewModel(
        blueprint: ViewModelBlueprint,
        packageName: String,
        repositoryName: String? = null
    ): Result<String> = safeExecute {
        val imports = generateImports(blueprint)
        val classDefinition = generateClassDefinition(blueprint)
        val stateDefinition = generateStateClass(blueprint)
        val eventDefinition = generateEventClass(blueprint)
        val body = generateClassBody(blueprint, repositoryName)

        """
package $packageName.viewmodel

$imports

$stateDefinition

$eventDefinition

$classDefinition {
$body
}
        """.trimIndent()
    }

    private fun generateImports(blueprint: ViewModelBlueprint): String {
        val imports = mutableSetOf(
            "import androidx.lifecycle.ViewModel",
            "import androidx.lifecycle.viewModelScope",
            "import kotlinx.coroutines.flow.*",
            "import kotlinx.coroutines.launch",
            "import dagger.hilt.android.lifecycle.HiltViewModel",
            "import javax.inject.Inject"
        )

        // Add repository import if needed
        blueprint.dependencies.find { it.second.contains("Repository") }?.let {
            imports.add("import ${packageName}.data.repository.${it.second}")
        }

        return imports.sorted().joinToString("\n")
    }

    private fun generateClassDefinition(blueprint: ViewModelBlueprint): String {
        return """
@HiltViewModel
class ${blueprint.name} @Inject constructor(
    private val repository: ${blueprint.dependencies.firstOrNull()?.second ?: "Any"}
) : ViewModel()
        """.trimIndent()
    }

    private fun generateStateClass(blueprint: ViewModelBlueprint): String {
        return """
data class ${blueprint.stateClass}(
    val isLoading: Boolean = false,
    val data: List<Any> = emptyList(),
    val error: String? = null
)
        """.trimIndent()
    }

    private fun generateEventClass(blueprint: ViewModelBlueprint): String {
        return """
sealed class ${blueprint.eventClass} {
    object LoadData : ${blueprint.eventClass}()
    data class ItemClicked(val item: Any) : ${blueprint.eventClass}()
    data class DeleteItem(val itemId: String) : ${blueprint.eventClass}()
    object Refresh : ${blueprint.eventClass}()
}
        """.trimIndent()
    }

    private fun generateClassBody(
        blueprint: ViewModelBlueprint,
        repositoryName: String?
    ): String {
        val body = StringBuilder()

        // UI State
        body.append("""
    private val _uiState = MutableStateFlow(${blueprint.stateClass}())
    val uiState: StateFlow<${blueprint.stateClass}> = _uiState.asStateFlow()

        """.trimIndent())

        // Init block
        body.append("""
    init {
        loadData()
    }

        """.trimIndent())

        // Functions
        blueprint.functions.forEach { function ->
            body.append(generateFunction(function, repositoryName))
            body.append("\n")
        }

        // Event handler
        body.append(generateEventHandler(blueprint))

        return body.toString()
    }

    private fun generateFunction(functionName: String, repositoryName: String?): String {
        return when {
            functionName.startsWith("loadData") -> generateLoadDataFunction(repositoryName)
            functionName.contains("handleEvent") -> "// Event handling is in onEvent function"
            functionName.contains("updateState") -> generateUpdateStateFunction()
            else -> """
    fun $functionName {
        // TODO: Implement $functionName
        viewModelScope.launch {
            // Implementation here
        }
    }
            """.trimIndent()
        }
    }

    private fun generateLoadDataFunction(repositoryName: String?): String {
        val repositoryCall = if (repositoryName != null) {
            "repository.getData()"
        } else {
            "flowOf(emptyList<Any>())" // Default empty flow
        }

        return """
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                $repositoryCall
                    .collect { data ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            data = data,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }
        """.trimIndent()
    }

    private fun generateUpdateStateFunction(): String {
        return """
    private fun updateState(newState: ${blueprint.stateClass}) {
        _uiState.value = newState
    }
        """.trimIndent()
    }

    private fun generateEventHandler(blueprint: ViewModelBlueprint): String {
        return """

    fun onEvent(event: ${blueprint.eventClass}) {
        when (event) {
            is ${blueprint.eventClass}.LoadData -> loadData()
            is ${blueprint.eventClass}.ItemClicked -> handleItemClicked(event.item)
            is ${blueprint.eventClass}.DeleteItem -> deleteItem(event.itemId)
            is ${blueprint.eventClass}.Refresh -> loadData()
        }
    }

    private fun handleItemClicked(item: Any) {
        // TODO: Handle item click
        viewModelScope.launch {
            // Navigate or perform action
        }
    }

    private fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                // repository.deleteItem(itemId)
                loadData() // Refresh data
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }
        """.trimIndent()
    }
}