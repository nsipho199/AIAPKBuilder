package com.aiapkbuilder.app.data.repository

import com.aiapkbuilder.app.data.local.TemplateDao
import com.aiapkbuilder.app.data.model.AppType
import com.aiapkbuilder.app.data.model.ProjectTemplate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing project templates.
 * Templates provide scaffolding for different app types.
 */
@Singleton
class TemplateRepository @Inject constructor(
    private val templateDao: TemplateDao
) {
    fun getBuiltInTemplates(): Flow<List<ProjectTemplate>> =
        templateDao.getBuiltInTemplates()

    fun getTemplatesByType(type: AppType): Flow<List<ProjectTemplate>> =
        templateDao.getTemplatesByType(type)

    suspend fun getTemplateById(id: String): ProjectTemplate? =
        templateDao.getTemplateById(id)

    suspend fun insertTemplate(template: ProjectTemplate) =
        templateDao.insertTemplate(template)

    suspend fun updateTemplate(template: ProjectTemplate) =
        templateDao.updateTemplate(template)

    suspend fun deleteTemplate(id: String) =
        templateDao.deleteTemplate(id)

    fun getTemplateCount(): Flow<Int> =
        templateDao.getTemplateCount()
}
