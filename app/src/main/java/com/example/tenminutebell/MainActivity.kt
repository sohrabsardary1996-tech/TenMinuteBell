package com.example.tenminutebell

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        val title = TextView(this).apply { text = "Ten Minute Bell"; textSize = 28f; gravity = Gravity.CENTER }
        status = TextView(this).apply { text = "زنگ هر ۱۰ دقیقه یک‌بار پخش می‌شود"; textSize = 18f; gravity = Gravity.CENTER; setPadding(0, 24, 0, 32) }
        val start = Button(this).apply { text = "شروع زنگ"; setOnClickListener { startService(); status.text = "فعال است — هر ۱۰ دقیقه یک زنگ" } }
        val stop = Button(this).apply { text = "توقف"; setOnClickListener { stopService(Intent(this@MainActivity, BellService::class.java)); status.text = "متوقف شده است" } }
        layout.addView(title, ViewGroup.LayoutParams(-1, -2)); layout.addView(status, ViewGroup.LayoutParams(-1, -2))
        layout.addView(start, ViewGroup.LayoutParams(-1, -2)); layout.addView(stop, ViewGroup.LayoutParams(-1, -2))
        setContentView(layout)
    }
    private fun startService() {
        val intent = Intent(this, BellService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }
}
