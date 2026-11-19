package com.example.studyplanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studyplanner.data.AppDatabase
import com.example.studyplanner.data.Subject
import com.example.studyplanner.ui.theme.StudyPlannerTheme
import kotlinx.coroutines.launch

class SubjectsActivity : ComponentActivity() {

    private val subjectDao by lazy { AppDatabase.getInstance(this).subjectDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyPlannerTheme {
                val scope = rememberCoroutineScope()
                val subjectsFlow = remember { subjectDao.getAll() }
                val subjects by subjectsFlow.collectAsState(initial = emptyList())

                SubjectsScreen(
                    subjects = subjects,
                    onAdd = { name -> scope.launch { subjectDao.insert(Subject(name = name)) } },
                    onEdit = { subject, newName ->
                        scope.launch { subjectDao.update(subject.copy(name = newName)) }
                    },
                    onDelete = { subject -> scope.launch { subjectDao.delete(subject) } },
                    onSubjectClick = { subject -> openTasks(subject.id) },
                    onCalendarClick = { openCalendar() }
                )
            }
        }
    }

    private fun openTasks(subjectId: Int) {
        val intent = Intent(this, TasksActivity::class.java).putExtra("subjectId", subjectId)
        startActivity(intent)
    }

    private fun openCalendar() {
        startActivity(Intent(this, CalendarActivity::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectsScreen(
    subjects: List<Subject>,
    onAdd: (String) -> Unit,
    onEdit: (Subject, String) -> Unit,
    onDelete: (Subject) -> Unit,
    onSubjectClick: (Subject) -> Unit,
    onCalendarClick: () -> Unit
) {
    var newName by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var editName by rememberSaveable { mutableStateOf("") }
    var deletingId by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subjects") },
                actions = {
                    TextButton(onClick = onCalendarClick) { Text("Calendar") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New subject") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        onAdd(newName.trim())
                        newName = ""
                    }
                ) { Text("Add") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (subjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No subjects yet")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjects, key = { it.id }) { subject ->
                        SubjectRow(
                            subject = subject,
                            onClick = { onSubjectClick(subject) },
                            onEditClick = {
                                editName = subject.name
                                editingId = subject.id
                            },
                            onDeleteClick = { deletingId = subject.id }
                        )
                    }
                }
            }
        }
    }

    val subjectToEdit = subjects.firstOrNull { it.id == editingId }
    if (subjectToEdit != null) {
        AlertDialog(
            onDismissRequest = { editingId = null },
            title = { Text("Edit subject") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editName.isNotBlank(),
                    onClick = {
                        onEdit(subjectToEdit, editName.trim())
                        editingId = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingId = null }) { Text("Cancel") }
            }
        )
    }

    val subjectToDelete = subjects.firstOrNull { it.id == deletingId }
    if (subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("Delete subject") },
            text = {
                Text("Delete \"${subjectToDelete.name}\"? All its tasks will be deleted too.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(subjectToDelete)
                        deletingId = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SubjectRow(
    subject: Subject,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${subject.name}")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${subject.name}")
            }
        }
    }
}

private val previewSubjects = listOf(
    Subject(id = 1, name = "Mobile Computing"),
    Subject(id = 2, name = "Databases"),
    Subject(id = 3, name = "Software Engineering")
)

@Preview(showBackground = true)
@Composable
private fun SubjectsScreenPreview() {
    StudyPlannerTheme {
        SubjectsScreen(previewSubjects, {}, { _, _ -> }, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun SubjectsScreenLandscapePreview() {
    StudyPlannerTheme {
        SubjectsScreen(previewSubjects, {}, { _, _ -> }, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SubjectsScreenEmptyPreview() {
    StudyPlannerTheme {
        SubjectsScreen(emptyList(), {}, { _, _ -> }, {}, {}, {})
    }
}
