package com.my.BatteryReceiver

import android.widget.Toast
import android.util.Log
import android.app.PendingIntent
import com.my.BatteryReceiver.R
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.os.Build
import android.app.NotificationChannel // Для API 26+

class BatteryReceiver : BroadcastReceiver() {
	
	companion object {
		const val LOW_BATTERY_NOTIFICATION_ID = 1
		const val FULL_BATTERY_NOTIFICATION_ID = 2
		
		private var isCharging = false //Зарядка подключена?
		private var isLowBatteryAlertActive = false
		private var isFullBatteryAlertActive = false
		var isRingerOnLowBattery = false //Контроль рингтона при низком заряде
		var isRingerOnFullBattery = false //Контроль при высоком заряде
		
		private var ringtone: Ringtone? = null
		
		fun stopAlarmStatic(context: Context) {
			ringtone?.isLooping = false
			ringtone?.stop()
			ringtone = null
			if (isCharging) {
				isRingerOnFullBattery = true
			}else{
				isRingerOnLowBattery = true
			}
			//При выкл. рингтона поднимаем флаг
			//чтобы не включался при выкл. экрана
			
			// Отменяем уведомления
			val manager = context.getSystemService(NotificationManager::class.java)
			manager.cancel(FULL_BATTERY_NOTIFICATION_ID)
			manager.cancel(LOW_BATTERY_NOTIFICATION_ID)
			
			isLowBatteryAlertActive = false
			isFullBatteryAlertActive = false
		}
	}
	
	override fun onReceive(context: Context, intent: Intent) {
		val prefs = context.getSharedPreferences("battery_prefs", Context.MODE_PRIVATE)
		if (!prefs.getBoolean("monitoring_enabled", true)) return
		
		var batteryPct: Float = 0f
		var maxLevel: Int = 0
		var minLevel: Int = 0
		
		if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
		val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
		val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
		batteryPct = level * 100 / scale.toFloat()
		val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
		isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
		
		minLevel = prefs.getInt("min_battery", 20)
		maxLevel = prefs.getInt("max_battery", 80)
		
		val isLowBattery = batteryPct <= minLevel
		val isPlugged = isCharging || intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1) !=0
		
		if (batteryPct >= maxLevel && isCharging && !isFullBatteryAlertActive) {
			isFullBatteryAlertActive = true
			playAlarm(context, prefs)
			createNotificationChannel(context)
			showNotification(context, "Достигнут максимальный заряд", "Отключите устройство от зарядки",
			FULL_BATTERY_NOTIFICATION_ID)
		}
		else if (!isCharging) {
			if (isFullBatteryAlertActive) {
				stopAlarmStatic(context)
			}
		}
		
		if (isLowBattery && !isPlugged && !isLowBatteryAlertActive) {
			isLowBatteryAlertActive = true
			playAlarm(context, prefs)
			createNotificationChannel(context)
			showNotification(context, "Низкий заряд", "Осталось ${batteryPct.toInt()}%! Подключите зарядку",
			LOW_BATTERY_NOTIFICATION_ID)
		}
		else if (isPlugged && isLowBatteryAlertActive) {
			stopAlarmStatic(context)
		}
		}
		//Зарядка вкл/выкл сбрасываем флаг
		//для дальнейшего вывода сообщения и рингтона
		if (isCharging || batteryPct >= minLevel) {
			isRingerOnLowBattery = false
		}
		if (!isCharging) {
			isRingerOnFullBattery = false
		}
		// Если флаг поднят (true) выкл. рингтон
		if ((isRingerOnLowBattery && batteryPct <= minLevel)
		|| (isRingerOnFullBattery && batteryPct >= maxLevel)) { 
			stopAlarmStatic(context)
		}
	}
	
	private fun playAlarm(context: Context, prefs: SharedPreferences) {
	ringtone?.stop() // Сначала останавливаем предыдущий
	
	val soundUri = prefs.getString("alarm_sound_uri", null)?.let { Uri.parse(it) }
	ringtone = soundUri?.let { RingtoneManager.getRingtone(context, it) }
	ringtone?.apply {
	if (prefs.getBoolean("repeat_sound", false)) {
	isLooping = true
	}
	play()
	}
	}
	
	private fun showNotification(
	context: Context,
	title: String, 
	message: String,
	notificationId: Int) {
		
	val channelId = "battery_alerts"
	val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
	as NotificationManager
	
	// 1. Создаем Intent для обработки нажатия
	val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
	action = "ACTION_DISMISS" // Уникальное действие
	}
	
	// 2. Создаем PendingIntent с явным флагом
	val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
	PendingIntent.getBroadcast(
	context,
	0,
	dismissIntent,
	PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
	)
	} else {
	PendingIntent.getBroadcast(
	context,
	0,
	dismissIntent,
	PendingIntent.FLAG_UPDATE_CURRENT
	)
	}
	
	// 3. Строим уведомление
	val notification = NotificationCompat.Builder(context, channelId)
	.setContentTitle(title)
	.setContentText(message)
	.setSmallIcon(R.drawable.ic_launcher_message)
	.setPriority(NotificationCompat.PRIORITY_HIGH)
	.setAutoCancel(true)
	.addAction(
	R.drawable.ic_close,
	"Отключить сигнал",
	pendingIntent
	)
	.build()
	
	notificationManager.notify(notificationId,
	notification)
	}
	
	private fun createNotificationChannel(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
			"battery_alerts",
			"Уведомления о батарее",
			NotificationManager.IMPORTANCE_HIGH
			).apply {
				setSound(null, null)
				enableVibration(false)
				description = "Уведомления о уровне заряда батареи"
			}
			val manager = context.getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(channel)
		}
	}
}