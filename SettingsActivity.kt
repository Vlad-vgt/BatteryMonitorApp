package com.my.BatteryReceiver

import android.provider.Settings
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {
private lateinit var prefs: SharedPreferences
private lateinit var enableMonitoringSwitch: Switch
private lateinit var minBatterySeekBar: SeekBar
private lateinit var maxBatterySeekBar: SeekBar
private lateinit var minBatteryText: TextView
private lateinit var maxBatteryText: TextView
private lateinit var currentToneText: TextView
private lateinit var selectSoundButton: Button
private lateinit var repeatSoundCheckBox: CheckBox
private lateinit var showNotificationSwitch: Switch

companion object {
private const val REQUEST_CODE_SOUND = 101
}

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_settings)

// Инициализация SharedPreferences
prefs = getSharedPreferences("battery_prefs", MODE_PRIVATE)

// Привязка элементов UI
enableMonitoringSwitch = findViewById(R.id.enableMonitoringSwitch)
minBatterySeekBar = findViewById(R.id.minBatterySeekBar)
maxBatterySeekBar = findViewById(R.id.maxBatterySeekBar)
minBatteryText = findViewById(R.id.minBatteryText)
maxBatteryText = findViewById(R.id.maxBatteryText)
selectSoundButton = findViewById(R.id.selectSoundButton)
repeatSoundCheckBox = findViewById(R.id.repeatSoundCheckBox)
showNotificationSwitch = findViewById(R.id.showNotificationSwitch)
currentToneText = findViewById(R.id.currentToneText)

setupUI()
setupListeners()
toggleService()
}

private fun setupUI() {
// Загрузка сохраненных настроек
enableMonitoringSwitch.isChecked = prefs.getBoolean("monitoring_enabled", false)
minBatterySeekBar.progress = prefs.getInt("min_battery", 20)
maxBatterySeekBar.progress = prefs.getInt("max_battery", 80)
repeatSoundCheckBox.isChecked = prefs.getBoolean("repeat_sound", false)
showNotificationSwitch.isChecked = prefs.getBoolean("show_notification", true)

updateBatteryLevelTexts()
}

private fun setupListeners() {
enableMonitoringSwitch.setOnCheckedChangeListener { _, isChecked ->
prefs.edit().putBoolean("monitoring_enabled", isChecked).apply()
toggleService()
}

minBatterySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
prefs.edit().putInt("min_battery", progress).apply()
updateBatteryLevelTexts()
}
override fun onStartTrackingTouch(seekBar: SeekBar?) {}
override fun onStopTrackingTouch(seekBar: SeekBar?) {}
})

maxBatterySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
prefs.edit().putInt("max_battery", progress).apply()
updateBatteryLevelTexts()
}
override fun onStartTrackingTouch(seekBar: SeekBar?) {}
override fun onStopTrackingTouch(seekBar: SeekBar?) {}
})

selectSoundButton.setOnClickListener {
val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Выберите мелодию")

// Восстановление ранее выбранного звука
val currentTone = prefs.getString("alarm_sound_uri", null)
if (currentTone != null) {
putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentTone))
}
}
startActivityForResult(intent, REQUEST_CODE_SOUND)
}

repeatSoundCheckBox.setOnCheckedChangeListener { _, isChecked ->
prefs.edit().putBoolean("repeat_sound", isChecked).apply()
}

showNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
prefs.edit().putBoolean("show_notification", isChecked).apply()
}
}

fun toggleService() {
	if (enableMonitoringSwitch.isChecked) {
	startBatteryService()
	Toast.makeText(this, "Мониторинг включен", Toast.LENGTH_SHORT).show()
	} else {
	BatteryMonitorService.stopService()//Сбрасываем флаг
	stopService(Intent(this, BatteryMonitorService::class.java))
	Toast.makeText(this, "Мониторинг выключен", Toast.LENGTH_SHORT).show()
	}
}

private fun updateBatteryLevelTexts() {
minBatteryText.text = "Мин. заряд: ${minBatterySeekBar.progress}%"
maxBatteryText.text = "Макс. заряд: ${maxBatterySeekBar.progress}%"
val tone = prefs.getString("alarm_tone_text", null)
if (tone == null){
	currentToneText.text = "Выберите мелодию"
}else{
	currentToneText.text = "$tone"
}
}

private fun startBatteryService() {
val serviceIntent = Intent(this, BatteryMonitorService::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
ContextCompat.startForegroundService(this, serviceIntent)
} else {
startService(serviceIntent)
}
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
super.onActivityResult(requestCode, resultCode, data)
if (requestCode == REQUEST_CODE_SOUND && resultCode == RESULT_OK) {
val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
uri?.let {
// Сохраняем URI
prefs.edit().putString("alarm_sound_uri", it.toString()).apply()

// Получаем название мелодии
val ringtoneName = getRingtoneName(it)

//Сохраняем название
prefs.edit().putString("alarm_tone_text", ringtoneName.toString()).apply()

// Устанавливаем название в TextView
currentToneText.text = ringtoneName

Toast.makeText(this, "Мелодия выбрана: $ringtoneName", Toast.LENGTH_SHORT).show()
} ?: run {
// Если uri == null (выбрано "Без звука")
prefs.edit().putString("alarm_sound_uri", null).apply()
currentToneText.text = "Без звука"
}
}
}

private fun getRingtoneName(uri: Uri): String {
return when {
uri == Settings.System.DEFAULT_NOTIFICATION_URI -> "Мелодия по умолчанию"
uri.toString().isEmpty() -> "Без звука"
else -> {
val ringtone = RingtoneManager.getRingtone(this, uri)
ringtone?.getTitle(this) ?: "Неизвестная мелодия"
}
}
}
}