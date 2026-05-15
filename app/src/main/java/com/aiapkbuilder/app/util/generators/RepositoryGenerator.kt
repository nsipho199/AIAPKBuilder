package com.aiapkbuilder.app.util.generators

import com.aiapkbuilder.app.data.model.RepositoryBlueprint
import com.aiapkbuilder.app.util.safeExecute

/**
 * Generates Kotlin Repository code based on blueprints.
 */
class RepositoryGenerator {

    /**
     * Generates a complete Repository class from a blueprint.
     */
    fun generateRepository(
        blueprint: RepositoryBlueprint,
        packageName: String,
        dataModelName: String? = null
    ): Result<String> = safeExecute {
        val imports = generateImports(blueprint)
        val interfaceDefinition = generateInterface(blueprint)
        val classDefinition = generateClassDefinition(blueprint)
        val body = generateClassBody(blueprint, dataModelName)

        """
package $packageName.data.repository

$imports

$interfaceDefinition

$classDefinition {
$body
}
        """.trimIndent()
    }

    private fun generateImports(blueprint: RepositoryBlueprint): String {
        val imports = mutableSetOf(
            "import kotlinx.coroutines.flow.Flow",
            "import kotlinx.coroutines.flow.flow",
            "import kotlinx.coroutines.flow.map",
            "import javax.inject.Inject",
            "import javax.inject.Singleton"
        )

        // Add data source specific imports
        when (blueprint.dataSource) {
            "api" -> {
                imports.add("import retrofit2.Response")
                blueprint.dependencies.find { it.second.contains("ApiService") }?.let {
                    imports.add("import ${packageName}.data.api.${it.second}")
                }
            }
            "local" -> {
                blueprint.dependencies.find { it.second.contains("Dao") }?.let {
                    imports.add("import ${packageName}.data.local.${it.second}")
                }
            }
            "both" -> {
                blueprint.dependencies.find { it.second.contains("ApiService") }?.let {
                    imports.add("import ${packageName}.data.api.${it.second}")
                }
                blueprint.dependencies.find { it.second.contains("Dao") }?.let {
                    imports.add("import ${packageName}.data.local.${it.second}")
                }
            }
        }

        return imports.sorted().joinToString("\n")
    }

    private fun generateInterface(blueprint: RepositoryBlueprint): String {
        val interfaceName = blueprint.name.replace("Repository", "RepositoryInterface")
        val methods = blueprint.functions.joinToString("\n    ") { "suspend fun $it" }

        return """
interface $interfaceName {
    $methods
}
        """.trimIndent()
    }

    private fun generateClassDefinition(blueprint: RepositoryBlueprint): String {
        val dependencies = blueprint.dependencies.joinToString(",\n    ") {
            "${it.first}: ${it.second}"
        }

        return """
@Singleton
class ${blueprint.name} @Inject constructor(
    $dependencies
) : ${blueprint.name.replace("Repository", "RepositoryInterface")}
        """.trimIndent()
    }

    private fun generateClassBody(
        blueprint: RepositoryBlueprint,
        dataModelName: String?
    ): String {
        val body = StringBuilder()

        blueprint.functions.forEach { function ->
            body.append(generateFunction(function, blueprint.dataSource, dataModelName))
            body.append("\n")
        }

        return body.toString()
    }

    private fun generateFunction(
        functionSignature: String,
        dataSource: String,
        dataModelName: String?
    ): String {
        val functionName = functionSignature.substringBefore("(").trim()
        val params = functionSignature.substringAfter("(").substringBefore(")").trim()

        return when {
            functionName.startsWith("getData()") -> generateGetDataFunction(dataSource, dataModelName)
            functionName.startsWith("getDataById") -> generateGetDataByIdFunction(dataSource, dataModelName, params)
            functionName.startsWith("insertData") -> generateInsertFunction(dataSource, dataModelName, params)
            functionName.startsWith("updateData") -> generateUpdateFunction(dataSource, dataModelName, params)
            functionName.startsWith("deleteData") -> generateDeleteFunction(dataSource, dataModelName, params)
            else -> generateGenericFunction(functionSignature, dataSource)
        }
    }

    private fun generateGetDataFunction(dataSource: String, dataModelName: String?): String {
        val dataType = dataModelName ?: "Any"
        val returnType = "Flow<List<$dataType>>"

        return when (dataSource) {
            "api" -> """
    override fun getData(): $returnType = flow {
        try {
            val response = apiService.getData()
            if (response.isSuccessful) {
                emit(response.body() ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
            """.trimIndent()

            "local" -> """
    override fun getData(): $returnType = dao.getAllData()
            """.trimIndent()

            "both" -> """
    override fun getData(): $returnType = flow {
        // Try local first
        dao.getAllData().collect { localData ->
            emit(localData)
            // Then try to refresh from API
            try {
                val response = apiService.getData()
                if (response.isSuccessful) {
                    val remoteData = response.body() ?: emptyList()
                    // Update local cache
                    remoteData.forEach { dao.insertData(it) }
                    emit(remoteData)
                }
            } catch (e: Exception) {
                // Keep local data
            }
        }
    }
            """.trimIndent()

            else -> """
    override fun getData(): $returnType = flow {
        emit(emptyList<$dataType>())
    }
            """.trimIndent()
        }
    }

    private fun generateGetDataByIdFunction(
        dataSource: String,
        dataModelName: String?,
        params: String
    ): String {
        val dataType = dataModelName ?: "Any"
        val returnType = "Flow<$dataType?>"

        return when (dataSource) {
            "api" -> """
    override fun getDataById(id: String): $returnType = flow {
        try {
            val response = apiService.getDataById(id)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
            """.trimIndent()

            "local" -> """
    override fun getDataById(id: String): $returnType = dao.getDataById(id)
            """.trimIndent()

            "both" -> """
    override fun getDataById(id: String): $returnType = flow {
        // Try local first
        dao.getDataById(id).collect { localData ->
            emit(localData)
            // Then try to refresh from API if needed
            if (localData == null) {
                try {
                    val response = apiService.getDataById(id)
                    if (response.isSuccessful) {
                        val remoteData = response.body()
                        remoteData?.let { dao.insertData(it) }
                        emit(remoteData)
                    }
                } catch (e: Exception) {
                    // Keep null
                }
            }
        }
    }
            """.trimIndent()

            else -> """
    override fun getDataById(id: String): $returnType = flow {
        emit(null)
    }
            """.trimIndent()
        }
    }

    private fun generateInsertFunction(
        dataSource: String,
        dataModelName: String?,
        params: String
    ): String {
        return when (dataSource) {
            "api" -> """
    override suspend fun insertData(data: ${dataModelName ?: "Any"}) {
        try {
            apiService.insertData(data)
        } catch (e: Exception) {
            // Handle API error
            throw e
        }
    }
            """.trimIndent()

            "local" -> """
    override suspend fun insertData(data: ${dataModelName ?: "Any"}) {
        dao.insertData(data)
    }
            """.trimIndent()

            "both" -> """
    override suspend fun insertData(data: ${dataModelName ?: "Any"}) {
        // Insert to local first
        dao.insertData(data)
        // Then sync to API
        try {
            apiService.insertData(data)
        } catch (e: Exception) {
            // Handle API error - local data is saved
        }
    }
            """.trimIndent()

            else -> """
    override suspend fun insertData(data: ${dataModelName ?: "Any"}) {
        // TODO: Implement insert
    }
            """.trimIndent()
        }
    }

    private fun generateUpdateFunction(
        dataSource: String,
        dataModelName: String?,
        params: String
    ): String {
        return when (dataSource) {
            "api" -> """
    override suspend fun updateData(data: ${dataModelName ?: "Any"}) {
        try {
            apiService.updateData(data)
        } catch (e: Exception) {
            throw e
        }
    }
            """.trimIndent()

            "local" -> """
    override suspend fun updateData(data: ${dataModelName ?: "Any"}) {
        dao.updateData(data)
    }
            """.trimIndent()

            "both" -> """
    override suspend fun updateData(data: ${dataModelName ?: "Any"}) {
        dao.updateData(data)
        try {
            apiService.updateData(data)
        } catch (e: Exception) {
            // Handle API error
        }
    }
            """.trimIndent()

            else -> """
    override suspend fun updateData(data: ${dataModelName ?: "Any"}) {
        // TODO: Implement update
    }
            """.trimIndent()
        }
    }

    private fun generateDeleteFunction(
        dataSource: String,
        dataModelName: String?,
        params: String
    ): String {
        return when (dataSource) {
            "api" -> """
    override suspend fun deleteData(id: String) {
        try {
            apiService.deleteData(id)
        } catch (e: Exception) {
            throw e
        }
    }
            """.trimIndent()

            "local" -> """
    override suspend fun deleteData(id: String) {
        dao.deleteData(id)
    }
            """.trimIndent()

            "both" -> """
    override suspend fun deleteData(id: String) {
        dao.deleteData(id)
        try {
            apiService.deleteData(id)
        } catch (e: Exception) {
            // Handle API error
        }
    }
            """.trimIndent()

            else -> """
    override suspend fun deleteData(id: String) {
        // TODO: Implement delete
    }
            """.trimIndent()
        }
    }

    private fun generateGenericFunction(functionSignature: String, dataSource: String): String {
        return """
    override suspend fun $functionSignature {
        // TODO: Implement $functionSignature
        // Data source: $dataSource
    }
        """.trimIndent()
    }
}