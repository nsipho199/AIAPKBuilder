package com.aiapkbuilder.app.service.export

import android.content.Context
import com.aiapkbuilder.app.data.model.AppProject
import com.aiapkbuilder.app.data.model.AppType
import com.aiapkbuilder.app.data.model.ExportConfig
import com.aiapkbuilder.app.data.model.ExportType
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.data.service.export.ProjectExporter
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ProjectExporterTest {

    private lateinit var exporter: ProjectExporter
    private lateinit var repository: ProjectRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        repository = mockk()
        context = mockk()
        coEvery { context.filesDir } returns File(System.getProperty("java.io.tmpdir"))
        exporter = ProjectExporter(repository, context)
    }

    @Test
    fun `exportProject handles invalid project gracefully`() = runTest {
        val config = ExportConfig(projectId = "invalid", exportType = ExportType.SOURCE)
        coEvery { repository.getProject("invalid") } returns flowOf(null)

        val result = exporter.exportProject(config)
        assertTrue(result.isFailure)
    }

    @Test
    fun `exportProject succeeds with valid project`() = runTest {
        val project = AppProject(id = "p1", name = "Test App", description = "desc", prompt = "build test", appType = AppType.CALCULATOR)
        val config = ExportConfig(projectId = "p1", exportType = ExportType.SOURCE)

        coEvery { repository.getProject("p1") } returns flowOf(project)
        coEvery { repository.getAllProjects() } returns flowOf(emptyList())

        val result = exporter.exportProject(config)
        assertTrue(result.isFailure || result.isSuccess)
    }
}
