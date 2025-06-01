package com.my.BatteryReceiver

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.app.Notification
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.os.Build
import com.my.BatteryReceiver.R

class BatteryMonitorService : Service() {
	companion object {
		private var shouldRestart = false
		
		fun stopService() {
			shouldRestart = false
		}
	}
private val channelId = "battery_monitor_channel"
private lateinit var batteryReceiver: BatteryReceiver

override fun onBind(intent: Intent?) = null

override fun onCreate() {
super.onCreate()

createNotificationChannel()
startForeground(777, createNotification())

batteryReceiver = BatteryReceiver()
registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
}

private fun createNotificationChannel() {
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
val channel = NotificationChannel(
channelId,
"Battery Monitor",
NotificationManager.IMPORTANCE_HIGH
)
getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}
}

private fun createNotification(): Notification {
// 1. Создаем Intent для открытия SettingsActivity
val tapIntent = Intent(this, SettingsActivity::class.java).apply {
flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
}

// 2. Обертка в PendingIntent
val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
PendingIntent.getActivity(
this,
0,
tapIntent,
PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
} else {
PendingIntent.getActivity(
this,
0,
tapIntent,
PendingIntent.FLAG_UPDATE_CURRENT
)
}

// 3. Собираем уведомление
return NotificationCompat.Builder(this, channelId)
.setContentTitle("Мониторинг батареи")
//.setContentText(message)
.setSmallIcon(R.drawable.ic_battery_monitor)
.setContentIntent(pendingIntent) // Клик по всему уведомлению!
.setPriority(NotificationCompat.PRIORITY_MAX)
.build()
}

override fun onStartCommand (intent: Intent?, flags: Int, startId: Int): Int {
shouldRestart = true
return START_STICKY
}

override fun onDestroy() {
super.onDestroy()
unregisterReceiver(batteryReceiver)
if (shouldRestart) { // Проверяем наш флаг
val restartIntent = Intent(this, BatteryMonitorService::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
startForegroundService(restartIntent)
} else {
startService(restartIntent)
}
}
}
}
