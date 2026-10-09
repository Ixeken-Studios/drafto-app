package com.ixeken.drafto.domain.repository

import com.ixeken.drafto.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio de dominio para la gestión y sincronización reactiva de tareas.
 *
 * Define operaciones observables y atómicas para desacoplar los casos de uso y la interfaz
 * gráfica de la persistencia subyacente en SQLite/Room, asegurando que los flujos emitan
 * listas inmutables de [TodoItem].
 */
interface TodoRepository {
    fun observeAllTodos(): Flow<List<TodoItem>>
    suspend fun getAllTodosSync(): List<TodoItem>
    fun observePendingTodos(): Flow<List<TodoItem>>
    fun observeCompletedTodos(): Flow<List<TodoItem>>
    suspend fun saveTodo(todo: TodoItem)
    suspend fun toggleCompleted(id: String, isCompleted: Boolean)
    suspend fun togglePinned(id: String, isPinned: Boolean)
    suspend fun toggleSubtask(todoId: String, subtaskId: String, isDone: Boolean)
    suspend fun getTodoById(id: String): TodoItem?
    suspend fun deleteTodo(id: String)
    fun observeTodosByCollection(collectionId: String): Flow<List<TodoItem>>
    suspend fun getTodosByCollectionSync(collectionId: String): List<TodoItem>
}
