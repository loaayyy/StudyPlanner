package com.example.studyplanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId ORDER BY deadline")
    fun getBySubject(subjectId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Int): Task?

    @Query("SELECT * FROM tasks WHERE deadline BETWEEN :start AND :end ORDER BY deadline")
    fun getBetween(start: Long, end: Long): Flow<List<Task>>
}