package com.ixeken.drafto.data.repository

import com.ixeken.drafto.data.local.dao.TodoDao
import com.ixeken.drafto.data.local.entity.TodoEntity
import com.ixeken.drafto.domain.model.TodoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para validar la logica de orquestacion y delegacion de [TodoRepositoryImpl].
 */
class TodoRepositoryImplTest {

    private class FakeTodoDao : TodoDao {
        val todos = mutableMapOf<String, TodoEntity>()
        var lastUpdatedCompletedId: String? = null
        var lastUpdatedCompletedState: Boolean? = null
        var lastUpdatedCompletedAt: Long? = null
        var lastUpdatedPinnedId: String? = null
        var lastUpdatedPinnedState: Boolean? = null
        var lastDeletedId: String? = null

        override fun observeAllTodos(): Flow<List<TodoEntity>> {
            return flowOf(todos.values.toList())
        }

        override suspend fun getAllTodosSync(): List<TodoEntity> {
            return todos.values.toList()
        }

        override fun observePendingTodos(): Flow<List<TodoEntity>> {
            return flowOf(todos.values.filter { !it.isCompleted })
        }

        override fun observeCompletedTodos(): Flow<List<TodoEntity>> {
            return flowOf(todos.values.filter { it.isCompleted })
        }

        override fun observeTodosByCollection(collectionId: String): Flow<List<TodoEntity>> {
            return flowOf(todos.values.filter { it.collectionId == collectionId })
        }

        override suspend fun getTodosByCollectionSync(collectionId: String): List<TodoEntity> {
            return todos.values.filter { it.collectionId == collectionId }
        }

        override suspend fun insertTodo(todo: TodoEntity): Long {
            todos[todo.id] = todo
            return 1L
        }

        override suspend fun updateTodo(todo: TodoEntity) {
            todos[todo.id] = todo
        }

        override suspend fun updateCompleted(id: String, isCompleted: Boolean, completedAt: Long?) {
            lastUpdatedCompletedId = id
            lastUpdatedCompletedState = isCompleted
            lastUpdatedCompletedAt = completedAt
            todos[id]?.let {
                todos[id] = it.copy(isCompleted = isCompleted, completedAt = completedAt)
            }
        }

        override suspend fun updatePinned(id: String, isPinned: Boolean) {
            lastUpdatedPinnedId = id
            lastUpdatedPinnedState = isPinned
            todos[id]?.let {
                todos[id] = it.copy(isPinned = isPinned)
            }
        }

        override suspend fun getTodoByIdSync(id: String): TodoEntity? {
            return todos[id]
        }

        override suspend fun deleteTodo(id: String) {
            lastDeletedId = id
            todos.remove(id)
        }
    }

    @Test
    fun saveTodoAndObserveAll_worksCorrectly() = runBlocking {
        val fakeDao = FakeTodoDao()
        val repository = TodoRepositoryImpl(fakeDao, Dispatchers.Unconfined)

        val todo = TodoItem(
            id = "t1",
            title = "Test Todo",
            description = "Details",
            isCompleted = false,
            isPinned = false,
            dueDate = null,
            createdAt = 1000L,
            completedAt = null
        )

        repository.saveTodo(todo)
        val emitted = repository.observeAllTodos().first()

        assertEquals(1, emitted.size)
        assertEquals("Test Todo", emitted.first().title)
    }

    @Test
    fun toggleCompleted_setsTimestampWhenCompleted() = runBlocking {
        val fakeDao = FakeTodoDao()
        val repository = TodoRepositoryImpl(fakeDao, Dispatchers.Unconfined)

        repository.toggleCompleted("t1", true)

        assertEquals("t1", fakeDao.lastUpdatedCompletedId)
        assertEquals(true, fakeDao.lastUpdatedCompletedState)
        assertNotNull(fakeDao.lastUpdatedCompletedAt)
    }

    @Test
    fun toggleCompleted_clearsTimestampWhenPending() = runBlocking {
        val fakeDao = FakeTodoDao()
        val repository = TodoRepositoryImpl(fakeDao, Dispatchers.Unconfined)

        repository.toggleCompleted("t1", false)

        assertEquals("t1", fakeDao.lastUpdatedCompletedId)
        assertEquals(false, fakeDao.lastUpdatedCompletedState)
        assertNull(fakeDao.lastUpdatedCompletedAt)
    }

    @Test
    fun togglePinned_updatesPinnedState() = runBlocking {
        val fakeDao = FakeTodoDao()
        val repository = TodoRepositoryImpl(fakeDao, Dispatchers.Unconfined)

        repository.togglePinned("t1", true)

        assertEquals("t1", fakeDao.lastUpdatedPinnedId)
        assertEquals(true, fakeDao.lastUpdatedPinnedState)
    }

    @Test
    fun deleteTodo_removesItem() = runBlocking {
        val fakeDao = FakeTodoDao()
        val repository = TodoRepositoryImpl(fakeDao, Dispatchers.Unconfined)

        repository.deleteTodo("t1")

        assertEquals("t1", fakeDao.lastDeletedId)
    }

    @Test
    fun getAllTodosSync_returnsAllSavedTodos() = runBlocking {
        val fakeDao = FakeTodoDao()
        val repository = TodoRepositoryImpl(fakeDao, Dispatchers.Unconfined)
        val todo = TodoItem(
            id = "t1",
            title = "Sync Todo",
            createdAt = 1000L
        )
        repository.saveTodo(todo)
        val result = repository.getAllTodosSync()
        assertEquals(1, result.size)
        assertEquals("Sync Todo", result.first().title)
    }
}
