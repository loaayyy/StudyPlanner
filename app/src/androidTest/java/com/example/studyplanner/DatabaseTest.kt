package com.example.studyplanner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.studyplanner.data.AppDatabase
import com.example.studyplanner.data.Subject
import com.example.studyplanner.data.SubjectDao
import com.example.studyplanner.data.Task
import com.example.studyplanner.data.TaskDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var subjectDao: SubjectDao
    private lateinit var taskDao: TaskDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        subjectDao = db.subjectDao()
        taskDao = db.taskDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun subjectCrud() = runBlocking {
        // Create
        val id = subjectDao.insert(Subject(name = "Mobile Computing")).toInt()
        assertEquals("Mobile Computing", subjectDao.getById(id)?.name)

        // Update
        subjectDao.update(Subject(id = id, name = "Mobile"))
        assertEquals("Mobile", subjectDao.getById(id)?.name)

        // Read all
        assertEquals(1, subjectDao.getAll().first().size)

        // Delete
        subjectDao.delete(Subject(id = id, name = "Mobile"))
        assertNull(subjectDao.getById(id))
    }

    @Test
    fun taskCrud() = runBlocking {
        val subjectId = subjectDao.insert(Subject(name = "Math")).toInt()

        // Create
        val taskId = taskDao.insert(
            Task(subjectId = subjectId, title = "Sheet 1", type = "Assignment", deadline = 1000L)
        ).toInt()
        val task = taskDao.getById(taskId)!!
        assertEquals("Sheet 1", task.title)

        // Update (mark as done)
        taskDao.update(task.copy(isDone = true))
        assertTrue(taskDao.getById(taskId)!!.isDone)

        // Read by subject
        assertEquals(1, taskDao.getBySubject(subjectId).first().size)

        // Delete
        taskDao.delete(task)
        assertNull(taskDao.getById(taskId))
    }

    @Test
    fun deletingSubjectDeletesItsTasks() = runBlocking {
        val subjectId = subjectDao.insert(Subject(name = "Physics")).toInt()
        taskDao.insert(Task(subjectId = subjectId, title = "Quiz 1", type = "Quiz", deadline = 1000L))
        taskDao.insert(Task(subjectId = subjectId, title = "Lab 2", type = "Assignment", deadline = 2000L))

        subjectDao.delete(Subject(id = subjectId, name = "Physics"))

        assertTrue(taskDao.getBySubject(subjectId).first().isEmpty())
    }

    @Test
    fun tasksBetweenDates() = runBlocking {
        val subjectId = subjectDao.insert(Subject(name = "Networks")).toInt()
        taskDao.insert(Task(subjectId = subjectId, title = "Early", type = "Quiz", deadline = 100L))
        taskDao.insert(Task(subjectId = subjectId, title = "Inside", type = "Exam", deadline = 500L))
        taskDao.insert(Task(subjectId = subjectId, title = "Late", type = "Project", deadline = 900L))

        val result = taskDao.getBetween(400L, 600L).first()

        assertEquals(1, result.size)
        assertEquals("Inside", result[0].title)
    }
}