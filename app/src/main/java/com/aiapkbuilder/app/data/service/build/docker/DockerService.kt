package com.aiapkbuilder.app.data.service.build.docker

import com.aiapkbuilder.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing Docker containers for Android builds.
 * Handles container lifecycle, build execution, and artifact extraction.
 */
@Singleton
class DockerService @Inject constructor() {

    private val dockerImage = "aiapkbuilder/android-build:latest"
    private val workspaceDir = "/workspace"
    private val outputDir = "/output"

    /**
     * Checks if Docker is available on the system.
     */
    suspend fun isDockerAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("docker", "--version")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            AppLogger.w("Docker not available", e)
            false
        }
    }

    /**
     * Creates a build container with the source code.
     */
    suspend fun createBuildContainer(projectId: String, sourceFile: File): String = withContext(Dispatchers.IO) {
        val containerName = "aiapkbuilder_build_$projectId"

        // Create output directory
        val hostOutputDir = File(System.getProperty("java.io.tmpdir"), "aiapkbuilder_output_$projectId")
        hostOutputDir.mkdirs()

        // Copy source to temp directory
        val hostSourceDir = File(System.getProperty("java.io.tmpdir"), "aiapkbuilder_source_$projectId")
        hostSourceDir.mkdirs()

        // Extract source zip (assuming it's a zip file)
        extractZip(sourceFile, hostSourceDir)

        // Create container
        val createCommand = listOf(
            "docker", "create",
            "--name", containerName,
            "-v", "${hostSourceDir.absolutePath}:$workspaceDir",
            "-v", "${hostOutputDir.absolutePath}:$outputDir",
            "-w", workspaceDir,
            dockerImage,
            "./gradlew", "assembleDebug"
        )

        executeCommand(createCommand)

        // Return container ID
        val inspectCommand = listOf("docker", "inspect", "-f", "{{.Id}}", containerName)
        val containerId = executeCommand(inspectCommand).trim()

        AppLogger.i("Created build container: $containerId")
        containerId
    }

    /**
     * Starts the build process in the container.
     */
    suspend fun startBuild(containerId: String) = withContext(Dispatchers.IO) {
        val startCommand = listOf("docker", "start", "-i", containerId)
        executeCommand(startCommand)
        AppLogger.i("Started build in container: $containerId")
    }

    /**
     * Gets the status of a container.
     */
    suspend fun getContainerStatus(containerId: String): String = withContext(Dispatchers.IO) {
        val command = listOf("docker", "inspect", "-f", "{{.State.Status}}", containerId)
        executeCommand(command).trim()
    }

    /**
     * Gets the exit code of a container.
     */
    suspend fun getContainerExitCode(containerId: String): Int? = withContext(Dispatchers.IO) {
        try {
            val command = listOf("docker", "inspect", "-f", "{{.State.ExitCode}}", containerId)
            val output = executeCommand(command).trim()
            output.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Streams logs from the container.
     */
    fun getContainerLogs(containerId: String): Flow<String> = flow {
        try {
            val process = ProcessBuilder("docker", "logs", "-f", containerId)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    emit(line)
                    kotlinx.coroutines.delay(50) // Throttle for UI
                }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                emit("Build process exited with code $exitCode")
            }

        } catch (e: Exception) {
            AppLogger.e("Failed to stream container logs", e)
            emit("Error streaming logs: ${e.message}")
        }
    }

    /**
     * Stops a running container.
     */
    suspend fun stopContainer(containerId: String) = withContext(Dispatchers.IO) {
        val command = listOf("docker", "stop", containerId)
        executeCommand(command)
        AppLogger.i("Stopped container: $containerId")
    }

    /**
     * Extracts the built APK from the container.
     */
    suspend fun extractArtifact(containerId: String): String = withContext(Dispatchers.IO) {
        // Find APK file in output directory
        val findCommand = listOf("find", "/tmp", "-name", "*.apk", "-type", "f")
        val apkPath = executeCommand(findCommand).trim().lines().firstOrNull()
            ?: throw IOException("No APK file found in build output")

        // Copy to a permanent location
        val outputFile = File(System.getProperty("java.io.tmpdir"), "build_artifact_${System.currentTimeMillis()}.apk")
        val copyCommand = listOf("cp", apkPath, outputFile.absolutePath)
        executeCommand(copyCommand)

        // Clean up container
        cleanupContainer(containerId)

        outputFile.absolutePath
    }

    /**
     * Cleans up the build container.
     */
    private suspend fun cleanupContainer(containerId: String) = withContext(Dispatchers.IO) {
        try {
            val rmCommand = listOf("docker", "rm", "-f", containerId)
            executeCommand(rmCommand)
            AppLogger.i("Cleaned up container: $containerId")
        } catch (e: Exception) {
            AppLogger.w("Failed to cleanup container", e)
        }
    }

    /**
     * Executes a shell command and returns the output.
     */
    private fun executeCommand(command: List<String>): String {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw IOException("Command failed with exit code $exitCode: ${command.joinToString(" ")}\n$output")
            }

            output
        } catch (e: Exception) {
            AppLogger.e("Command execution failed", e)
            throw e
        }
    }

    /**
     * Extracts a ZIP file to a directory.
     */
    private fun extractZip(zipFile: File, destination: File) {
        // Simple unzip using Java (in real implementation, use a proper ZIP library)
        val process = ProcessBuilder("unzip", "-q", zipFile.absolutePath, "-d", destination.absolutePath)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IOException("Failed to extract ZIP file")
        }
    }
}