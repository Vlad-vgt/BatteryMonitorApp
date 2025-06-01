package com.my.BatteryReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.widget.Toast

class NotificationDismissReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent?) {
		when (intent?.action) {
			"ACTION_DISMISS" -> {
				// 1. Останавливаем сигнал через статический метод
				BatteryReceiver.stopAlarmStatic(context)
				
				Toast.makeText(context, "Сигнал отключен", Toast.LENGTH_SHORT).show()
			}
		}
	}
}