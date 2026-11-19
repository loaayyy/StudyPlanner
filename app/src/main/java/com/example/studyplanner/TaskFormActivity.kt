
package com.example.studyplanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.studyplanner.data.AppDatabase
import com.example.studyplanner.data.Task
import com.example.studyplanner.data.TaskDao
import com.example.studyplanner.ui.theme.StudyPlannerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val KEY_TASK_ID = "taskId"
private const val KEY_SUBJECT_ID = "subjectId"
private const val NO_ID = -1

private val TASK_TYPES = listOf(
    "Assignment",
    "Quiz",
    "Project",
    "Exam"
)

class TaskFormActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val taskId = intent.getIntExtra(KEY_TASK_ID, NO_ID)
        val subjectId = intent.getIntExtra(KEY_SUBJECT_ID, NO_ID)

        // Add mode needs a valid subjectId.
        // Edit mode only needs a valid taskId.
        if (taskId == NO_ID && subjectId == NO_ID) {
            finish()
            return
        }

        val taskDao = AppDatabase
            .getInstance(applicationContext)
            .taskDao()

        setContent {
            StudyPlannerTheme {
                TaskFormScreen(
                    taskId = taskId,
                    subjectId = subjectId,
                    taskDao = taskDao,
                    onSaved = { finish() },
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun TaskFormScreen(
    taskId: Int,
    subjectId: Int,
    taskDao: TaskDao,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val isEditMode = taskId != NO_ID
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TASK_TYPES.first()) }
    var deadline by rememberSaveable { mutableLongStateOf(defaultDeadline()) }
    var notes by rememberSaveable { mutableStateOf("") }

    var showTitleError by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(isEditMode) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    // Prevents reloading and overwriting user edits after rotation.
    var hasLoaded by rememberSaveable {
        mutableStateOf(!isEditMode)
    }

    var isLoadError by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Load existing task in Edit Mode.
    LaunchedEffect(taskId) {
        if (isEditMode && !hasLoaded) {
            isLoading = true
            errorMessage = null

            try {
                val task = taskDao.getById(taskId)

                if (task == null) {
                    isLoadError = true
                    errorMessage = "Task not found. It may have been deleted."
                } else {
                    title = task.title
                    type = task.type
                    deadline = task.deadline
                    notes = task.notes
                }
            } catch (e: Exception) {
                isLoadError = true
                errorMessage = "Could not load the task. Please try again."
            } finally {
                hasLoaded = true
                isLoading = false
            }
        }
    }

    TaskFormContent(
        isEditMode = isEditMode,
        title = title,
        onTitleChange = {
            title = it
            showTitleError = false
        },
        type = type,
        onTypeChange = { type = it },
        deadline = deadline,
        onDeadlineChange = { deadline = it },
        notes = notes,
        onNotesChange = { notes = it },
        showTitleError = showTitleError,
        isLoading = isLoading,
        isSaving = isSaving,
        isLoadError = isLoadError,
        errorMessage = errorMessage,
        onBack = onBack,
        onSave = {
            if (title.isBlank()) {
                showTitleError = true
                return@TaskFormContent
            }

            if (isSaving || isLoading || isLoadError) {
                return@TaskFormContent
            }

            scope.launch {
                isSaving = true
                errorMessage = null

                try {
                    if (isEditMode) {
                        // Get the original task to preserve its ID,
                        // subjectId and isDone values.
                        val oldTask = taskDao.getById(taskId)

                        if (oldTask == null) {
                            isLoadError = true
                            errorMessage = "Task not found. Changes were not saved."
                            return@launch
                        }

                        val updatedTask = oldTask.copy(
                            title = title.trim(),
                            type = type,
                            deadline = deadline,
                            notes = notes.trim()
                        )

                        taskDao.update(updatedTask)

                    } else {
                        val newTask = Task(
                            subjectId = subjectId,
                            title = title.trim(),
                            type = type,
                            deadline = deadline,
                            notes = notes.trim()
                        )

                        taskDao.insert(newTask)
                    }

                    onSaved()

                } catch (e: Exception) {
                    errorMessage = "Something went wrong while saving. Please try again."
                } finally {
                    isSaving = false
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormContent(
    isEditMode: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    deadline: Long,
    onDeadlineChange: (Long) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    showTitleError: Boolean,
    isLoading: Boolean,
    isSaving: Boolean,
    isLoadError: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Task" else "Add Task",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (isLoadError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorMessage ?: "Unable to load task.",
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }

        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Task Title
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Task Title") },
                    placeholder = { Text("Enter task title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showTitleError,
                    enabled = !isSaving
                )

                if (showTitleError) {
                    Text(
                        text = "Task title is required.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Task Type
                Text(
                    text = "Task Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    TASK_TYPES.forEach { taskType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = type == taskType,
                                    onClick = { onTypeChange(taskType) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = type == taskType,
                                onClick = null,
                                enabled = !isSaving
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(text = taskType)
                        }
                    }
                }

                // Deadline
                Text(
                    text = "Deadline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        showDatePicker(
                            context = context,
                            currentDeadline = deadline,
                            onPicked = onDeadlineChange
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text("Date: ${formatDate(deadline)}")
                }

                OutlinedButton(
                    onClick = {
                        showTimePicker(
                            context = context,
                            currentDeadline = deadline,
                            onPicked = onDeadlineChange
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text("Time: ${formatTime(deadline)}")
                }

                // Optional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Add additional details...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    enabled = !isSaving,
                    minLines = 3
                )

                // Error Message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Save Button
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Save Task")
                    }
                }

                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// Date Picker
private fun showDatePicker(
    context: android.content.Context,
    currentDeadline: Long,
    onPicked: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentDeadline
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val updatedCalendar = Calendar.getInstance().apply {
                timeInMillis = currentDeadline
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }

            onPicked(updatedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

// Time Picker
private fun showTimePicker(
    context: android.content.Context,
    currentDeadline: Long,
    onPicked: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentDeadline
    }

    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val updatedCalendar = Calendar.getInstance().apply {
                timeInMillis = currentDeadline
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            onPicked(updatedCalendar.timeInMillis)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    ).show()
}

// Default deadline: tomorrow at 9:00 AM
private fun defaultDeadline(): Long {
    return Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatDate(time: Long): String {
    return SimpleDateFormat(
        "EEE, dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(time))
}

private fun formatTime(time: Long): String {
    return SimpleDateFormat(
        "hh:mm a",
        Locale.getDefault()
    ).format(Date(time))
}

@Preview(showBackground = true)
@Composable
fun TaskFormPreview() {
    StudyPlannerTheme {
        TaskFormContent(
            isEditMode = false,
            title = "",
            onTitleChange = {},
            type = "Assignment",
            onTypeChange = {},
            deadline = defaultDeadline(),
            onDeadlineChange = {},
            notes = "",
            onNotesChange = {},
            showTitleError = false,
            isLoading = false,
            isSaving = false,
            isLoadError = false,
            errorMessage = null,
            onBack = {},
            onSave = {}
        )
    }
}
