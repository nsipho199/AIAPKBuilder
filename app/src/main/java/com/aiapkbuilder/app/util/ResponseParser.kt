package com.aiapkbuilder.app.util

import com.aiapkbuilder.app.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses AI responses and converts them into structured data models.
 * Handles both JSON responses and natural language responses.
 */
@Singleton
class ResponseParser @Inject constructor(
    private val gson: Gson
) {

    /**
     * Parse a JSON response into a GeneratedProjectPlan
     */
    fun parseProjectPlan(jsonResponse: String): Result<GeneratedProjectPlan> = safeExecute {
        try {
            // First try direct JSON parsing
            gson.fromJson(jsonResponse, GeneratedProjectPlan::class.java)
        } catch (e: JsonSyntaxException) {
            // If direct parsing fails, try to extract JSON from natural language
            extractJsonFromText(jsonResponse)?.let { json ->
                gson.fromJson(json, GeneratedProjectPlan::class.java)
            } ?: throw Exception("Could not parse response as valid project plan")
        }
    }

    /**
     * Parse a JSON response into a ProjectBlueprint
     */
    fun parseProjectBlueprint(jsonResponse: String): Result<ProjectBlueprint> = safeExecute {
        try {
            gson.fromJson(jsonResponse, ProjectBlueprint::class.java)
        } catch (e: JsonSyntaxException) {
            throw Exception("Invalid blueprint format: ${e.message}")
        }
    }

    /**
     * Parse screen specifications from AI response
     */
    fun parseScreenSpecs(jsonResponse: String): Result<List<ScreenSpec>> = safeExecute {
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<ScreenSpec>>() {}.type
            gson.fromJson(jsonResponse, type)
        } catch (e: JsonSyntaxException) {
            // Try to extract screen info from natural language
            extractScreensFromText(jsonResponse)
        }
    }

    /**
     * Parse data model specifications
     */
    fun parseDataModels(jsonResponse: String): Result<List<DataModelSpec>> = safeExecute {
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<DataModelSpec>>() {}.type
            gson.fromJson(jsonResponse, type)
        } catch (e: JsonSyntaxException) {
            extractDataModelsFromText(jsonResponse)
        }
    }

    /**
     * Parse API endpoint specifications
     */
    fun parseApiEndpoints(jsonResponse: String): Result<List<String>> = safeExecute {
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            gson.fromJson(jsonResponse, type)
        } catch (e: JsonSyntaxException) {
            extractEndpointsFromText(jsonResponse)
        }
    }

    /**
     * Validate that a response contains valid code
     */
    fun validateCodeResponse(code: String, language: String = "kotlin"): Result<String> = safeExecute {
        when (language.lowercase()) {
            "kotlin" -> validateKotlinCode(code)
            "xml" -> validateXmlCode(code)
            else -> code // Accept as-is for other languages
        }
    }

    private fun validateKotlinCode(code: String): String {
        // Basic validation checks
        val issues = mutableListOf<String>()

        // Check for basic syntax issues
        if (!code.contains("package ")) {
            issues.add("Missing package declaration")
        }

        if (!code.contains("import ")) {
            issues.add("Missing imports")
        }

        if (code.contains("TODO(") || code.contains("FIXME(")) {
            issues.add("Contains TODO/FIXME comments")
        }

        // Check for unbalanced braces (very basic)
        val openBraces = code.count { it == '{' }
        val closeBraces = code.count { it == '}' }
        if (openBraces != closeBraces) {
            issues.add("Unbalanced braces: $openBraces open, $closeBraces close")
        }

        // Check for common Kotlin patterns
        if (code.contains("fun ") && !code.contains("@Composable")) {
            // Regular function - should have proper signature
            if (!code.contains("): ")) {
                issues.add("Function may be missing return type")
            }
        }

        if (issues.isNotEmpty()) {
            throw Exception("Code validation issues: ${issues.joinToString(", ")}")
        }

        return code
    }

    private fun validateXmlCode(code: String): String {
        // Basic XML validation
        if (!code.trim().startsWith("<?xml") && !code.trim().startsWith("<")) {
            throw Exception("Invalid XML: missing root element")
        }

        // Check for balanced tags (very basic)
        val openTags = "<[^/][^>]*>".toRegex().findAll(code).count()
        val closeTags = "</[^>]+>".toRegex().findAll(code).count()

        if (openTags != closeTags) {
            throw Exception("Unbalanced XML tags: $openTags open, $closeTags close")
        }

        return code
    }

    private fun extractJsonFromText(text: String): String? {
        // Try to find JSON block in natural language response
        val jsonPatterns = listOf(
            "```json\\n(.*?)\\n```".toRegex(RegexOption.DOT_MATCHES_ALL),
            "\\{.*\\}".toRegex(RegexOption.DOT_MATCHES_ALL),
            "\"appName\".*\\}".toRegex(RegexOption.DOT_MATCHES_ALL)
        )

        for (pattern in jsonPatterns) {
            pattern.find(text)?.let { match ->
                val json = match.groupValues[1].takeIf { it.isNotBlank() } ?: match.value
                return try {
                    // Validate it's parseable JSON
                    gson.fromJson(json, Any::class.java)
                    json
                } catch (e: Exception) {
                    null
                }
            }
        }

        return null
    }

    private fun extractScreensFromText(text: String): List<ScreenSpec> {
        val screens = mutableListOf<ScreenSpec>()

        // Look for screen descriptions in text
        val screenPatterns = listOf(
            "(\\w+Screen):\\s*([^\\n]+)".toRegex(),
            "Screen:\\s*(\\w+).*?Description:\\s*([^\\n]+)".toRegex(),
            "(\\w+)\\s*-\\s*([^\\n]+)".toRegex()
        )

        for (pattern in screenPatterns) {
            pattern.findAll(text).forEach { match ->
                val screenName = match.groupValues[1]
                val description = match.groupValues.getOrNull(2) ?: "Screen description"
                screens.add(ScreenSpec(
                    name = screenName,
                    route = "/${screenName.lowercase().removeSuffix("screen")}",
                    description = description,
                    uiComponents = extractUiComponents(description)
                ))
            }
        }

        return screens.takeIf { it.isNotEmpty() } ?: listOf(
            ScreenSpec("HomeScreen", "/home", "Main screen", listOf("Content"))
        )
    }

    private fun extractDataModelsFromText(text: String): List<DataModelSpec> {
        val models = mutableListOf<DataModelSpec>()

        // Look for data model descriptions
        val modelPatterns = listOf(
            "Model:\\s*(\\w+).*?Fields:\\s*([^\\n]+)".toRegex(),
            "Entity:\\s*(\\w+).*?Properties:\\s*([^\\n]+)".toRegex()
        )

        for (pattern in modelPatterns) {
            pattern.findAll(text).forEach { match ->
                val modelName = match.groupValues[1]
                val fieldsText = match.groupValues[2]
                val properties = extractProperties(fieldsText)

                models.add(DataModelSpec(
                    name = modelName,
                    properties = properties,
                    isEntity = true
                ))
            }
        }

        return models.takeIf { it.isNotEmpty() } ?: listOf(
            DataModelSpec("Item", listOf("id" to "String", "name" to "String"), true)
        )
    }

    private fun extractEndpointsFromText(text: String): List<String> {
        val endpoints = mutableListOf<String>()

        // Look for API endpoint patterns
        val endpointPatterns = listOf(
            "/api/\\w+".toRegex(),
            "GET\\s+/\\w+".toRegex(),
            "POST\\s+/\\w+".toRegex(),
            "endpoint:\\s*([^\\s\\n]+)".toRegex()
        )

        for (pattern in endpointPatterns) {
            pattern.findAll(text).forEach { match ->
                val endpoint = match.value.replace(Regex("^(GET|POST)\\s+"), "")
                if (endpoint.startsWith("/")) {
                    endpoints.add(endpoint)
                }
            }
        }

        return endpoints.distinct()
    }

    private fun extractUiComponents(description: String): List<String> {
        val components = mutableListOf<String>()

        // Common UI component keywords
        val componentKeywords = listOf(
            "button", "text", "list", "card", "image", "form", "input",
            "toolbar", "navigation", "menu", "dialog", "fab"
        )

        componentKeywords.forEach { keyword ->
            if (description.contains(keyword, ignoreCase = true)) {
                components.add(keyword.capitalize())
            }
        }

        return components.takeIf { it.isNotEmpty() } ?: listOf("Content")
    }

    private fun extractProperties(fieldsText: String): List<Pair<String, String>> {
        val properties = mutableListOf<Pair<String, String>>()

        // Split by common separators
        val fields = fieldsText.split(",", ";", " and ", " or ")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        fields.forEach { field ->
            // Try to extract name and type
            val parts = field.split(":").map { it.trim() }
            if (parts.size >= 2) {
                val name = parts[0]
                val type = parts[1]
                properties.add(name to type)
            } else {
                // Assume string type if not specified
                properties.add(field to "String")
            }
        }

        // Ensure we have at least an ID field
        if (properties.none { it.first == "id" }) {
            properties.add(0, "id" to "String")
        }

        return properties
    }

    /**
     * Clean and format AI response for better parsing
     */
    fun cleanResponse(response: String): String {
        return response
            .trim()
            .removePrefix("```json")
            .removeSuffix("```")
            .trim()
    }

    /**
     * Extract code blocks from AI response
     */
    fun extractCodeBlocks(response: String): List<String> {
        val codeBlocks = mutableListOf<String>()

        // Find all code blocks (```language\ncode\n```)
        val pattern = "```\\w*\\n(.*?)\\n```".toRegex(RegexOption.DOT_MATCHES_ALL)
        pattern.findAll(response).forEach { match ->
            codeBlocks.add(match.groupValues[1].trim())
        }

        return codeBlocks
    }
}