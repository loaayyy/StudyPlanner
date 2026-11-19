package com.example.studyplanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studyplanner.data.AppDatabase
import com.example.studyplanner.data.SubjectDao
import com.example.studyplanner.data.Task
import com.example.studyplanner.data.TaskDao
import com.example.studyplanner.ui.theme.StudyPlannerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val NO_SUBJECT = -1

class TasksActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val subjectId = intent.getIntExtra("subjectId", NO_SUBJECT)
        if (subjectId == NO_SUBJECT) {
            finish()
            return
        }

        val database = AppDatabase.getInstance(this)

        setContent {
            StudyPlannerTheme {
                TasksRoute(
                    subjectId = subjectId,
                    subjectDao = database.subjectDao(),
                    taskDao = database.taskDao(),
                    onBack = { finish() },
                    onOpenTask = { taskId ->
                        startActivity(
                            Intent(this, TaskDetailsActivity::class.java)
                                .putExtra("taskId", taskId)
                        )
                    },
                    onAddTask = {
                        startActivity(
                            Intent(this, TaskFormActivity::class.java)
                                .putExtra("subjectId", subjectId)
                        )
                    }
                )
            }
        }
    }
}

/** Talks to the database and holds the screen state; the UI itself lives in [TasksScreen]. */
@Composable
private fun TasksRoute(
    subjectId: Int,
    subjectDao: SubjectDao,
    taskDao: TaskDao,
    onBack: () -> Unit,
    onOpenTask: (Int) -> Unit,
    onAddTask: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var subjectName by rememberSaveable { mutableStateOf("") }
    val tasks by remember(subjectId) { taskDao.getBySubject(subjectId) }
        .collectAsState(initial = emptyList())

    LaunchedEffect(subjectId) {
        subjectName = subjectDao.getById(subjectId)?.name.orEmpty()
    }

    TasksScreen(
        subjectName = subjectName,
        tasks = tasks,
        onBack = onBack,
        onAddTask = onAddTask,
        onOpenTask = onOpenTask,
        onToggleDone = { task ->
            scope.launch { taskDao.update(task.copy(isDone = !task.isDone)) }
        },
        onDeleteTask = { task ->
            scope.launch { taskDao.delete(task) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    subjectName: String,
    tasks: List<Task>,
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onOpenTask: (Int) -> Unit,
    onToggleDone: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    // Only the id is saved, so the confirm dialog survives rotation.
    var pendingDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingDelete = tasks.firstOrNull { it.id == pendingDeleteId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = subjectName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
    ) { innerPadding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No tasks yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onClick = { onOpenTask(task.id) },
                        onToggleDone = { onToggleDone(task) },
                        onDelete = { pendingDeleteId = task.id }
                    )
                }
            }
        }
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete task?") },
            text = { Text("\"${task.title}\" will be removed permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTask(task)
                    pendingDeleteId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone() })

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (task.isDone) 0.5f else 1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                )
                Text(
                    text = "${task.type} · ${formatDeadline(task.deadline)}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete task")
            }
        }
    }
}

// SimpleDateFormat (not java.time) so it is safe on every device from minSdk 24.
private fun formatDeadline(millis: Long): String =
    SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()).format(Date(millis))

@Preview(showBackground = true)
@Preview(showBackground = true, widthDp = 640, heightDp = 360)
@Composable
private fun TasksScreenPreview() {
    val now = System.currentTimeMillis()
    StudyPlannerTheme {
        TasksScreen(
            subjectName = "Mobile Computing",
            tasks = listOf(
                Task(1, 1, "Submit the Compose lab report with a very long title", "Assignment", now + 86_400_000, "", false),
                Task(2, 1, "Quiz 1", "Quiz", now + 172_800_000, "", true),
                Task(3, 1, "Final project", "Project", now + 604_800_000, "", false)
            ),
            onBack = {},
            onAddTask = {},
            onOpenTask = {},
            onToggleDone = {},
            onDeleteTask = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TasksScreenEmptyPreview() {
    StudyPlannerTheme {
        TasksScreen("Algorithms", emptyList(), {}, {}, {}, {}, {})
    }
}