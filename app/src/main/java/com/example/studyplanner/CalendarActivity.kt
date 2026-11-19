package com.example.studyplanner

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.studyplanner.data.AppDatabase
import com.example.studyplanner.data.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Calendar screen (XML Views): pick a day, see that day's tasks from every subject
class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var tvDayTitle: TextView
    private lateinit var lvTasks: ListView
    private lateinit var tvEmpty: TextView

    // One line per task, e.g. "14:30 · Math quiz"
    private lateinit var adapter: ArrayAdapter<String>

    // Any moment inside the day the user picked, in milliseconds
    private var selectedDay: Long = System.currentTimeMillis()

    // "23 Nov" for the header
    private val titleFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)

    // "14:30" for each row
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)

    // The coroutine that is collecting the current day's tasks
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // Same ids in layout/ and layout-land/, so this works in both orientations
        calendarView = findViewById(R.id.calendarView)
        tvDayTitle = findViewById(R.id.tvDayTitle)
        lvTasks = findViewById(R.id.lvTasks)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        lvTasks.adapter = adapter
        // ListView hides itself and shows this message whenever the adapter is empty
        lvTasks.emptyView = tvEmpty

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // month starts from 0 (January = 0), the same as java.util.Calendar, so pass it as is
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            selectDay(cal.timeInMillis)
        }

        // First open: today. After a rotation: the day that was selected before it
        val startDay = if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SELECTED_DAY)) {
            savedInstanceState.getLong(KEY_SELECTED_DAY)
        } else {
            System.currentTimeMillis()
        }
        calendarView.date = startDay
        selectDay(startDay)
    }

    // Called before the activity is destroyed on rotation: keep the selected day
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_SELECTED_DAY, selectedDay)
    }

    // Remember the picked day, update the header and load its tasks
    private fun selectDay(dayMillis: Long) {
        selectedDay = dayMillis
        tvDayTitle.text = "Tasks on ${titleFormat.format(Date(dayMillis))}"
        loadTasks(dayMillis)
    }

    // Show every task (from all subjects) whose deadline falls inside that day
    private fun loadTasks(dayMillis: Long) {
        // Stop the previous day's collector first, or it keeps pushing old tasks into the list
        loadJob?.cancel()

        val start = startOfDay(dayMillis)
        val end = endOfDay(dayMillis)
        val taskDao = AppDatabase.getInstance(this).taskDao()

        loadJob = lifecycleScope.launch {
            // Collect only while the screen is visible; Room runs the query off the main thread
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskDao.getBetween(start, end).collect { tasks ->
                    adapter.clear()
                    adapter.addAll(tasks.map { formatTask(it) })
                }
            }
        }
    }

    // One list row: "14:30 · Math quiz"
    private fun formatTask(task: Task): String =
        "${timeFormat.format(Date(task.deadline))} · ${task.title}"

    // 00:00:00.000 of the day that contains [millis]
    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // 23:59:59.999 of the day that contains [millis] (not 23:59:00, or late tasks get missed)
    private fun endOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    companion object {
        private const val KEY_SELECTED_DAY = "selected_day"
    }
}
