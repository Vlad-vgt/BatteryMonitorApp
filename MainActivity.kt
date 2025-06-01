package com.my.BatteryReceiver

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)

// НЕ устанавливаем layout - Activity будет только переходником
//startService(Intent(this, BatteryMonitorService::class.java))
startActivity(Intent(this, SettingsActivity::class.java))
finish() // Закрываем себя сразу
}
}