package com.example.studyplanner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.studyplanner.data.AppDatabase
import com.example.studyplanner.data.Task
import com.example.studyplanner.ui.theme.StudyPlannerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Extra key agreed with the team (Team Guide, section 5)
private const val KEY_TASK_ID = "taskId"
private const val NO_ID = -1
private const val ONE_HOUR_MS = 60 * 60 * 1000L

class TaskDetailsActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getInstance(this) }
    private var taskId = NO_ID

    // Screen state: Compose redraws when these change
    private var task by mutableStateOf<Task?>(null)
    private var subjectName by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        taskId = intent.getIntExtra(KEY_TASK_ID, NO_ID)
        if (taskId == NO_ID) {
            finish()
            return
        }

        setContent {
            StudyPlannerTheme {
                TaskDetailsScreen(
                    task = task,
                    subjectName = subjectName,
                    onEdit = ::openEdit,
                    onShare = ::shareTask,
                    onAddToCalendar = ::addToCalendar
                )
            }
        }
    }

    // Runs when the screen opens and every time we come back (for example after Edit)
    override fun onResume() {
        super.onResume()
        loadTask()
    }

    private fun loadTask() {
        lifecycleScope.launch {
            val loaded = database.taskDao().getById(taskId)
            if (loaded == null) {
                finish() // the task was deleted
                return@launch
            }
            task = loaded
            subjectName = database.subjectDao().getById(loaded.subjectId)?.name.orEmpty()
        }
    }

    // Explicit Intent: open Farida's form in edit mode
    private fun openEdit() {
        startActivity(
            Intent(this, TaskFormActivity::class.java).putExtra(KEY_TASK_ID, taskId)
        )
    }

    // Implicit Intent 1: share the task as plain text through the app chooser
    private fun shareTask() {
        val current = task ?: return
        val text = buildString {
            appendLine("Task: ${current.title}")
            appendLine("Subject: $subjectName")
            appendLine("Type: ${current.type}")
            appendLine("Deadline: ${formatDeadline(current.deadline)}")
            if (current.notes.isNotBlank()) appendLine("Notes: ${current.notes}")
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, current.title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startSafely(Intent.createChooser(send, "Share task via"))
    }

    // Implicit Intent 2: open the calendar app with a new event already filled in
    private fun addToCalendar() {
        val current = task ?: return
        val insert = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, current.title)
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "$subjectName · ${current.type}\n${current.notes}".trim()
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, current.deadline)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, current.deadline + ONE_HOUR_MS)
        }
        startSafely(insert)
    }

    // No app can handle the intent: show a message instead of crashing
    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to do this", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailsScreen(
    task: Task?,
    subjectName: String,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onAddToCalendar: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Task Details") }) }
    ) { innerPadding ->
        if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(task.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = if (task.isDone) "Done" else "Not done yet",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (task.isDone) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailRow(label = "Subject", value = subjectName)
                        DetailRow(label = "Type", value = task.type)
                        DetailRow(label = "Deadline", value = formatDeadline(task.deadline))
                        DetailRow(
                            label = "Notes",
                            value = task.notes.ifBlank { "No notes" }
                        )
                    }
                }

                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit")
                }
                OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                    Text("Share")
                }
                OutlinedButton(onClick = onAddToCalendar, modifier = Modifier.fillMaxWidth()) {
                    Text("Add to Calendar")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

// SimpleDateFormat, not java.time: java.time needs Android 8 and our minSdk is 24
private fun formatDeadline(millis: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()).format(Date(millis))

@Preview(showBackground = true)
@Composable
private fun TaskDetailsPreview() {
    StudyPlannerTheme {
        TaskDetailsScreen(
            task = Task(
                id = 1,
                subjectId = 1,
                title = "Lab 6 hands-on",
                type = "Assignment",
                deadline = System.currentTimeMillis(),
                notes = "Write the 6 unit tests"
            ),
            subjectName = "Mobile Computing",
            onEdit = {},
            onShare = {},
            onAddToCalendar = {}
        )
    }
}
