package com.aiapkbuilder.app.util.generators

import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.util.safeExecute

/**
 * Generates Room database models, DAOs, and database classes.
 */
class DatabaseGenerator {

    /**
     * Generates a complete data model class from a specification.
     */
    fun generateDataModel(
        spec: DataModelSpec,
        packageName: String
    ): Result<String> = safeExecute {
        val imports = generateImports(spec)
        val annotations = generateAnnotations(spec)
        val classDefinition = generateClassDefinition(spec)

        """
package $packageName.data.model

$imports

$annotations
data class ${spec.name}(
${generateProperties(spec.properties)}
)
        """.trimIndent()
    }

    /**
     * Generates a DAO interface from a data model.
     */
    fun generateDao(
        modelName: String,
        packageName: String,
        tableName: String
    ): Result<String> = safeExecute {
        val imports = """
import androidx.room.*
import kotlinx.coroutines.flow.Flow
        """.trimIndent()

        val daoContent = generateDaoContent(modelName, tableName)

        """
package $packageName.data.local

$imports

@Dao
interface ${modelName}Dao {
$daoContent
}
        """.trimIndent()
    }

    /**
     * Generates the main database class.
     */
    fun generateDatabase(
        entities: List<String>,
        packageName: String,
        databaseName: String = "app_database"
    ): Result<String> = safeExecute {
        val imports = """
import androidx.room.Database
import androidx.room.RoomDatabase
        """.trimIndent()

        val entitiesList = entities.joinToString(",\n    ") { it }
        val daoAbstracts = entities.map { "${it}Dao" }.joinToString("\n    ") { "abstract fun ${it.lowercase()}Dao(): ${it}Dao" }

        """
package $packageName.data.local

$imports

@Database(
    entities = [
        $entitiesList
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    $daoAbstracts

    companion object {
        const val DATABASE_NAME = "${databaseName}.db"
    }
}
        """.trimIndent()
    }

    private fun generateImports(spec: DataModelSpec): String {
        val imports = mutableSetOf<String>()

        if (spec.isEntity) {
            imports.add("import androidx.room.Entity")
            imports.add("import androidx.room.PrimaryKey")
        }

        // Add imports based on property types
        spec.properties.forEach { (_, type) ->
            when (type) {
                "List<String>" -> imports.add("import androidx.room.TypeConverters")
                "List<Int>" -> imports.add("import androidx.room.TypeConverters")
            }
        }

        return imports.sorted().joinToString("\n")
    }

    private fun generateAnnotations(spec: DataModelSpec): String {
        return if (spec.isEntity) {
            "@Entity(tableName = \"${spec.name.lowercase()}s\")"
        } else {
            ""
        }
    }

    private fun generateClassDefinition(spec: DataModelSpec): String {
        return "data class ${spec.name}("
    }

    private fun generateProperties(properties: List<Pair<String, String>>): String {
        if (properties.isEmpty()) {
            return "    // No properties defined"
        }

        val props = properties.mapIndexed { index, (name, type) ->
            val primaryKey = if (index == 0 && type == "String") "    @PrimaryKey val " else "    val "
            val comma = if (index < properties.size - 1) "," else ""
            "${primaryKey}$name: $type$comma"
        }

        return props.joinToString("\n")
    }

    private fun generateDaoContent(modelName: String, tableName: String): String {
        return """
    @Query("SELECT * FROM $tableName")
    fun getAll(): Flow<List<$modelName>>

    @Query("SELECT * FROM $tableName WHERE id = :id")
    fun getById(id: String): Flow<$modelName?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: $modelName)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<$modelName>)

    @Update
    suspend fun update(item: $modelName)

    @Delete
    suspend fun delete(item: $modelName)

    @Query("DELETE FROM $tableName WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM $tableName")
    suspend fun deleteAll()
        """.trimIndent()
    }
}