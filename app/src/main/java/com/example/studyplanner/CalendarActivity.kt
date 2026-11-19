package com.example.studyplanner

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

// Calendar screen (XML Views): pick a day, see that day's tasks from every subject
class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var tvDayTitle: TextView
    private lateinit var lvTasks: ListView
    private lateinit var tvEmpty: TextView

    // One line per task, e.g. "14:30 · Math quiz"
    private lateinit var adapter: ArrayAdapter<String>

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
    }

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
}
