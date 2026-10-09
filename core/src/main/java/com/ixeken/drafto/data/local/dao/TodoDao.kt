package com.ixeken.drafto.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ixeken.drafto.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) reactivo para la tabla `todos`.
 *
 * Expone flujos observables con ordenamiento indexado en SQLite
 * y operaciones atomicas de mutacion para persistencia de tareas.
 */
@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY isPinned DESC, isCompleted ASC, createdAt DESC")
    fun observeAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY isPinned DESC, isCompleted ASC, createdAt DESC")
    suspend fun getAllTodosSync(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun observePendingTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isCompleted = 1 ORDER BY completedAt DESC, createdAt DESC")
    fun observeCompletedTodos(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("UPDATE todos SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun updateCompleted(id: String, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE todos SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean)

    @Query("SELECT * FROM todos WHERE collectionId = :collectionId ORDER BY isPinned DESC, isCompleted ASC, createdAt DESC")
    fun observeTodosByCollection(collectionId: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE collectionId = :collectionId ORDER BY isPinned DESC, isCompleted ASC, createdAt DESC")
    suspend fun getTodosByCollectionSync(collectionId: String): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getTodoByIdSync(id: String): TodoEntity?

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodo(id: String)
}
