package com.aiapkbuilder.app.util

import com.aiapkbuilder.app.data.model.GenerationEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles streaming responses from AI providers and converts them to UI events.
 * Provides real-time progress updates during code generation.
 */
@Singleton
class StreamingResponseHandler @Inject constructor() {

    private val _events = MutableSharedFlow<GenerationEvent>()
    val events: Flow<GenerationEvent> = _events.asSharedFlow()

    /**
     * Emit a progress event
     */
    suspend fun emitProgress(percent: Int, message: String) {
        _events.emit(GenerationEvent.Progress(percent, message))
    }

    /**
     * Emit a screen generation event
     */
    suspend fun emitScreenGenerated(screenName: String) {
        _events.emit(GenerationEvent.ScreenGenerated(screenName))
    }

    /**
     * Emit a code segment event
     */
    suspend fun emitCodeSegment(code: String) {
        _events.emit(GenerationEvent.CodeSegment(code))
    }

    /**
     * Emit an error event
     */
    suspend fun emitError(message: String) {
        _events.emit(GenerationEvent.Error(message))
    }

    /**
     * Emit completion event
     */
    suspend fun emitComplete() {
        _events.emit(GenerationEvent.Complete)
    }

    /**
     * Handle streaming response from AI provider
     */
    suspend fun handleStreamingResponse(responseFlow: Flow<String>) {
        try {
            emitProgress(0, "Starting generation...")

            responseFlow.collect { chunk ->
                // Process each chunk of the streaming response
                when {
                    chunk.contains("analyzing") || chunk.contains("analysis") -> {
                        emitProgress(10, "Analyzing requirements...")
                    }
                    chunk.contains("planning") || chunk.contains("plan") -> {
                        emitProgress(25, "Planning project structure...")
                    }
                    chunk.contains("screen") || chunk.contains("ui") -> {
                        emitProgress(40, "Designing user interface...")
                    }
                    chunk.contains("viewmodel") || chunk.contains("logic") -> {
                        emitProgress(55, "Creating business logic...")
                    }
                    chunk.contains("database") || chunk.contains("data") -> {
                        emitProgress(70, "Setting up data layer...")
                    }
                    chunk.contains("code") || chunk.contains("generating") -> {
                        emitProgress(85, "Generating source code...")
                    }
                    chunk.contains("complete") || chunk.contains("finished") -> {
                        emitProgress(100, "Finalizing project...")
                    }
                    else -> {
                        // Extract any screen names or code segments
                        extractAndEmitEvents(chunk)
                    }
                }
            }

            emitComplete()

        } catch (e: Exception) {
            emitError("Generation failed: ${e.localizedMessage}")
        }
    }

    private suspend fun extractAndEmitEvents(chunk: String) {
        // Try to extract screen names
        val screenPattern = Regex("(\\w+Screen)")
        screenPattern.findAll(chunk).forEach { match ->
            emitScreenGenerated(match.value)
        }

        // Try to extract code segments (basic detection)
        if (chunk.contains("fun ") || chunk.contains("class ") || chunk.contains("@Composable")) {
            emitCodeSegment(chunk.trim())
        }
    }

    /**
     * Create a simulated streaming response for demo purposes
     */
    fun createSimulatedStream(): Flow<String> = kotlinx.coroutines.flow.flow {
        val steps = listOf(
            "Starting analysis of your request...",
            "Analyzing app type and requirements...",
            "Planning project structure...",
            "Designing HomeScreen with modern UI...",
            "Creating navigation flow...",
            "Setting up data models...",
            "Generating ViewModel for state management...",
            "Creating repository layer...",
            "Setting up database schema...",
            "Generating API client...",
            "Creating theme and styling...",
            "Finalizing build configuration...",
            "Project generation complete!"
        )

        steps.forEachIndexed { index, step ->
            kotlinx.coroutines.delay(500) // Simulate delay
            emit(step)
        }
    }
}

/**
 * Progress tracker for generation operations
 */
class GenerationProgressTracker(private val handler: StreamingResponseHandler) {

    private var currentProgress = 0

    suspend fun updateProgress(stage: GenerationStage, message: String? = null) {
        val progress = stage.progress
        val displayMessage = message ?: stage.defaultMessage

        if (progress > currentProgress) {
            currentProgress = progress
            handler.emitProgress(progress, displayMessage)
        }
    }

    suspend fun complete() {
        handler.emitComplete()
    }

    suspend fun error(message: String) {
        handler.emitError(message)
    }
}

enum class GenerationStage(val progress: Int, val defaultMessage: String) {
    STARTED(0, "Starting generation..."),
    ANALYZING(10, "Analyzing your request..."),
    PLANNING(25, "Planning project structure..."),
    DESIGNING_UI(40, "Designing user interface..."),
    CREATING_LOGIC(55, "Creating business logic..."),
    SETTING_UP_DATA(70, "Setting up data layer..."),
    GENERATING_CODE(85, "Generating source code..."),
    FINALIZING(100, "Finalizing project...")
}