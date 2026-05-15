package com.aiapkbuilder.app.data.service.export

import android.content.Context
import com.aiapkbuilder.app.data.model.AppProject
import com.aiapkbuilder.app.data.model.ExportConfig
import com.aiapkbuilder.app.data.model.ExportType
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectExporter @Inject constructor(
    private val projectRepository: ProjectRepository,
    @ApplicationContext private val context: Context
) {
    private val logger = AppLogger.getLogger("ProjectExporter")

    suspend fun exportProject(config: ExportConfig): Result<File> = withContext(Dispatchers.IO) {
        try {
            val project = projectRepository.getProject(config.projectId).let { flow ->
                kotlinx.coroutines.flow.first(flow)
            } ?: return@withContext Result.failure(Exception("Project not found"))

            val exportDir = File(context.cacheDir, "exports")
            exportDir.mkdirs()

            val zipName = config.fileName.ifEmpty {
                "${project.name.replace(" ", "_")}_${System.currentTimeMillis()}.zip"
            }
            val zipFile = File(exportDir, zipName)

            FileOutputStream(zipFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    zos.setLevel(config.compressionLevel)

                    if (config.exportType == ExportType.BINARY || config.exportType == ExportType.BOTH) {
                        project.apkPath?.let { path ->
                            addToZip(zos, File(path), "apk/${File(path).name}")
                        }
                    }

                    if (config.exportType == ExportType.SOURCE || config.exportType == ExportType.BOTH) {
                        if (config.includeResources) {
                            val sourcesDir = context.filesDir
                            addDirectoryToZip(zos, sourcesDir, "source/")
                        }
                    }

                    if (config.includeMetadata) {
                        addMetadataToZip(zos, project)
                    }

                    if (config.includeHistory) {
                        val artifacts = kotlinx.coroutines.flow.first(projectRepository.getArtifactsForProject(config.projectId))
                        artifacts.forEach { artifact ->
                            artifact.localPath?.let { path ->
                                val file = File(path)
                                if (file.exists()) {
                                    addToZip(zos, file, "artifacts/${file.name}")
                                }
                            }
                        }
                    }
                }
            }

            logger.i("Project exported to: ${zipFile.absolutePath}")
            Result.success(zipFile)
        } catch (e: Exception) {
            logger.e("Failed to export project", e)
            Result.failure(e)
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        FileInputStream(file).use { fis ->
            zos.putNextEntry(ZipEntry(entryName))
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }

    private fun addDirectoryToZip(zos: ZipOutputStream, dir: File, basePath: String) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { file ->
            val entryName = basePath + file.name
            if (file.isDirectory) {
                addDirectoryToZip(zos, file, "$entryName/")
            } else {
                addToZip(zos, file, entryName)
            }
        }
    }

    private fun addMetadataToZip(zos: ZipOutputStream, project: AppProject) {
        val metadata = buildString {
            appendLine("Project: ${project.name}")
            appendLine("Description: ${project.description}")
            appendLine("Type: ${project.appType.displayName}")
            appendLine("Version: ${project.versionName} (${project.versionCode})")
            appendLine("Package: ${project.packageName}")
            appendLine("Build Provider: ${project.buildProvider.displayName}")
            appendLine("Status: ${project.buildStatus.displayName}")
            appendLine("Features: ${project.features.joinToString(", ")}")
            appendLine("Screens: ${project.screens.joinToString(", ")}")
            appendLine("Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(project.createdAt))}")
        }
        zos.putNextEntry(ZipEntry("metadata.txt"))
        zos.write(metadata.toByteArray())
        zos.closeEntry()
    }

    suspend fun getExportSize(config: ExportConfig): Long = withContext(Dispatchers.IO) {
        val result = exportProject(config)
        result.getOrNull()?.length() ?: 0L
    }
}
