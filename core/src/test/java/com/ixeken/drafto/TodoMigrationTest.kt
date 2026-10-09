package com.ixeken.drafto

import androidx.sqlite.db.SupportSQLiteDatabase
import com.ixeken.drafto.data.local.DraftoDatabase
import com.ixeken.drafto.data.local.entity.TodoEntity
import com.ixeken.drafto.data.mapper.toDomain
import com.ixeken.drafto.data.mapper.toEntity
import com.ixeken.drafto.domain.model.TodoFilter
import com.ixeken.drafto.domain.model.TodoItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Suite de pruebas unitarias para validar la integridad del subsistema de tareas (Notebook To-dos).
 *
 * Cubre:
 * 1. Serialización y deserialización JSON con `kotlinx.serialization`.
 * 2. Exhaustividad del enum [TodoFilter].
 * 3. Fidelidad y bidireccionalidad del mapeador [TodoMapperKt].
 * 4. Verificación de versiones y sentencias DDL en la migración de base de datos [DraftoDatabase.MIGRATION_5_6].
 */
class TodoMigrationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // ---------------------------------------------------------------------------------------------
    // 1. Pruebas de Serialización / Deserialización JSON de TodoItem
    // ---------------------------------------------------------------------------------------------

    @Test
    fun todoItem_jsonSerialization_roundtripPreservesAllFields() {
        val originalItem = TodoItem(
            id = "todo-full-101",
            title = "Implementar pruebas de Room",
            description = "Validar integridad DDL y mapeadores bidireccionales",
            isCompleted = true,
            isPinned = true,
            dueDate = 1750000000L,
            createdAt = 1700000000L,
            completedAt = 1710000000L
        )

        val encodedJson = json.encodeToString(originalItem)
        val decodedItem = json.decodeFromString<TodoItem>(encodedJson)

        assertEquals(originalItem, decodedItem)
        assertEquals("todo-full-101", decodedItem.id)
        assertEquals("Implementar pruebas de Room", decodedItem.title)
        assertEquals("Validar integridad DDL y mapeadores bidireccionales", decodedItem.description)
        assertTrue(decodedItem.isCompleted)
        assertTrue(decodedItem.isPinned)
        assertEquals(1750000000L, decodedItem.dueDate)
        assertEquals(1700000000L, decodedItem.createdAt)
        assertEquals(1710000000L, decodedItem.completedAt)
    }

    @Test
    fun todoItem_jsonSerialization_handlesDefaultAndNullableValues() {
        val minimalItem = TodoItem(
            id = "todo-min-202",
            title = "Tarea básica",
            createdAt = 1700000000L
        )

        val encodedJson = json.encodeToString(minimalItem)
        val decodedItem = json.decodeFromString<TodoItem>(encodedJson)

        assertEquals(minimalItem, decodedItem)
        assertEquals("", decodedItem.description)
        assertEquals(false, decodedItem.isCompleted)
        assertEquals(false, decodedItem.isPinned)
        assertNull(decodedItem.dueDate)
        assertNull(decodedItem.completedAt)
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Pruebas de Exhaustividad del Enum TodoFilter
    // ---------------------------------------------------------------------------------------------

    @Test
    fun todoFilter_enumCompletenessAndSerialization() {
        val expectedFilters = listOf(TodoFilter.ALL, TodoFilter.PENDING, TodoFilter.COMPLETED)
        assertEquals(3, TodoFilter.entries.size)
        assertTrue(TodoFilter.entries.containsAll(expectedFilters))

        for (filter in expectedFilters) {
            val encoded = json.encodeToString(filter)
            val decoded = json.decodeFromString<TodoFilter>(encoded)
            assertEquals(filter, decoded)
            assertEquals(filter, TodoFilter.valueOf(filter.name))
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 3. Pruebas de Mapeo Bidireccional TodoMapper
    // ---------------------------------------------------------------------------------------------

    @Test
    fun todoMapper_biDirectionalFidelity_entityToDomainAndBack() {
        val originalEntity = TodoEntity(
            id = "entity-303",
            title = "Preparar release v6",
            description = "Verificar migraciones y performance 120 FPS",
            isCompleted = true,
            isPinned = false,
            dueDate = 1780000000L,
            createdAt = 1720000000L,
            completedAt = 1725000000L
        )

        val domain = originalEntity.toDomain()
        assertEquals(originalEntity.id, domain.id)
        assertEquals(originalEntity.title, domain.title)
        assertEquals(originalEntity.description, domain.description)
        assertEquals(originalEntity.isCompleted, domain.isCompleted)
        assertEquals(originalEntity.isPinned, domain.isPinned)
        assertEquals(originalEntity.dueDate, domain.dueDate)
        assertEquals(originalEntity.createdAt, domain.createdAt)
        assertEquals(originalEntity.completedAt, domain.completedAt)

        val roundtripEntity = domain.toEntity()
        assertEquals(originalEntity, roundtripEntity)
    }

    @Test
    fun todoMapper_domainToEntity_preservesNullsAndEmptyFields() {
        val originalDomain = TodoItem(
            id = "domain-404",
            title = "Tarea sin fecha",
            description = "",
            isCompleted = false,
            isPinned = true,
            dueDate = null,
            createdAt = 1700000000L,
            completedAt = null
        )

        val entity = originalDomain.toEntity()
        assertEquals(originalDomain.id, entity.id)
        assertEquals(originalDomain.title, entity.title)
        assertEquals("", entity.description)
        assertEquals(false, entity.isCompleted)
        assertEquals(true, entity.isPinned)
        assertNull(entity.dueDate)
        assertEquals(1700000000L, entity.createdAt)
        assertNull(entity.completedAt)

        val roundtripDomain = entity.toDomain()
        assertEquals(originalDomain, roundtripDomain)
    }

    // ---------------------------------------------------------------------------------------------
    // 4. Validación de la Migración Room MIGRATION_5_6
    // ---------------------------------------------------------------------------------------------

    @Test
    fun databaseMigration_5_to_6_verifiesVersionsAndDdlStatements() {
        val migration = DraftoDatabase.MIGRATION_5_6

        assertEquals(5, migration.startVersion)
        assertEquals(6, migration.endVersion)

        val executedStatements = mutableListOf<String>()
        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        migration.migrate(fakeDb)

        assertEquals(3, executedStatements.size)

        // Verificación de sentencia CREATE TABLE `todos`
        val createTableSql = executedStatements[0]
        assertTrue("Debe crear la tabla todos", createTableSql.contains("CREATE TABLE IF NOT EXISTS `todos`"))
        assertTrue("Debe incluir columna id primaria", createTableSql.contains("`id` TEXT NOT NULL PRIMARY KEY"))
        assertTrue("Debe incluir columna title", createTableSql.contains("`title` TEXT NOT NULL"))
        assertTrue("Debe incluir columna description", createTableSql.contains("`description` TEXT NOT NULL"))
        assertTrue("Debe incluir columna isCompleted", createTableSql.contains("`isCompleted` INTEGER NOT NULL"))
        assertTrue("Debe incluir columna isPinned", createTableSql.contains("`isPinned` INTEGER NOT NULL"))
        assertTrue("Debe incluir columna dueDate anulable", createTableSql.contains("`dueDate` INTEGER"))
        assertTrue("Debe incluir columna createdAt", createTableSql.contains("`createdAt` INTEGER NOT NULL"))
        assertTrue("Debe incluir columna completedAt anulable", createTableSql.contains("`completedAt` INTEGER"))

        // Verificación de índices secundarios para consultas optimizadas
        val firstIndexSql = executedStatements[1]
        assertTrue(
            "Debe crear índice compuesto por estado y creación",
            firstIndexSql.contains("CREATE INDEX IF NOT EXISTS `index_todos_isCompleted_createdAt` ON `todos` (`isCompleted`, `createdAt`)")
        )

        val secondIndexSql = executedStatements[2]
        assertTrue(
            "Debe crear índice para items fijados",
            secondIndexSql.contains("CREATE INDEX IF NOT EXISTS `index_todos_isPinned` ON `todos` (`isPinned`)")
        )
    }
}
