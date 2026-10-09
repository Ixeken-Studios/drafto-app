package com.ixeken.drafto.data.repository

import com.ixeken.drafto.data.local.dao.TodoDao
import com.ixeken.drafto.data.mapper.toDomain
import com.ixeken.drafto.data.mapper.toEntity
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.repository.TodoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementacion de [TodoRepository] respaldada por Room DAO en SQLite.
 *
 * Despacha todas las operaciones de I/O suspendidas y flujos reactivos sobre [Dispatchers.IO]
 * para garantizar cero jank y permitir que la interfaz se ejecute a 120 FPS sin bloqueos.
 */
class TodoRepositoryImpl(
    private val todoDao: TodoDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TodoRepository {

    override fun observeAllTodos(): Flow<List<TodoItem>> {
        return todoDao.observeAllTodos()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getAllTodosSync(): List<TodoItem> = withContext(ioDispatcher) {
        todoDao.getAllTodosSync().map { it.toDomain() }
    }

    override fun observePendingTodos(): Flow<List<TodoItem>> {
        return todoDao.observePendingTodos()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun observeCompletedTodos(): Flow<List<TodoItem>> {
        return todoDao.observeCompletedTodos()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveTodo(todo: TodoItem): Unit = withContext(ioDispatcher) {
        todoDao.insertTodo(todo.toEntity())
    }

    override suspend fun toggleCompleted(id: String, isCompleted: Boolean): Unit = withContext(ioDispatcher) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        todoDao.updateCompleted(id = id, isCompleted = isCompleted, completedAt = completedAt)
    }

    override suspend fun togglePinned(id: String, isPinned: Boolean): Unit = withContext(ioDispatcher) {
        todoDao.updatePinned(id = id, isPinned = isPinned)
    }

    override suspend fun toggleSubtask(todoId: String, subtaskId: String, isDone: Boolean): Unit = withContext(ioDispatcher) {
        val existingEntity = todoDao.getTodoByIdSync(todoId) ?: return@withContext
        val updatedSubtasks = existingEntity.subtasks.map { subtask ->
            if (subtask.id == subtaskId) subtask.copy(isDone = isDone) else subtask
        }
        val allDone = updatedSubtasks.isNotEmpty() && updatedSubtasks.all { it.isDone }
        val updatedEntity = existingEntity.copy(
            subtasks = updatedSubtasks,
            isCompleted = allDone,
            completedAt = if (allDone) System.currentTimeMillis() else null
        )
        todoDao.updateTodo(updatedEntity)
    }

    override suspend fun getTodoById(id: String): TodoItem? = withContext(ioDispatcher) {
        todoDao.getTodoByIdSync(id)?.toDomain()
    }

    override suspend fun deleteTodo(id: String): Unit = withContext(ioDispatcher) {
        todoDao.deleteTodo(id)
    }

    override fun observeTodosByCollection(collectionId: String): Flow<List<TodoItem>> {
        return todoDao.observeTodosByCollection(collectionId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getTodosByCollectionSync(collectionId: String): List<TodoItem> = withContext(ioDispatcher) {
        todoDao.getTodosByCollectionSync(collectionId).map { it.toDomain() }
    }
}
