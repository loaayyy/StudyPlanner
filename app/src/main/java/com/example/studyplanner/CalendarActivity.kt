package com.example.studyplanner

import android.os.Bundle
import android.widget.CalendarView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// Calendar screen (XML Views): pick a day, see that day's tasks from every subject
class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var tvDayTitle: TextView
    private lateinit var lvTasks: ListView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // Same ids in layout/ and layout-land/, so this works in both orientations
        calendarView = findViewById(R.id.calendarView)
        tvDayTitle = findViewById(R.id.tvDayTitle)
        lvTasks = findViewById(R.id.lvTasks)
        tvEmpty = findViewById(R.id.tvEmpty)
    }
}
