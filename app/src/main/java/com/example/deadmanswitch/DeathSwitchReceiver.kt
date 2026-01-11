package com.example.deadmanswitch

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DeathSwitchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("LifeMonitorPrefs", Context.MODE_PRIVATE)
        val action = intent.action

        // 逻辑 A: 如果是手机重启，重新排布定时器以防监测中断
        if (Intent.ACTION_BOOT_COMPLETED == action) {
            reScheduleTimer(context, prefs)
            return
        }

        // 逻辑 B: 定时任务触发，检查是否真的超时
        val lastCheckIn = prefs.getLong("last_check_in_time", 0)
        val currentTime = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L // 24小时阈值

        // 如果最后签到时间不为0，且当前距离上次签到已超过24小时
        if (lastCheckIn != 0L && (currentTime - lastCheckIn >= oneDayMs)) {
            sendDeathNotification(context, prefs)
        } else {
            // 如果没到时间（可能是由于系统重启提前触发了广播），重新设定剩余时间的闹钟
            reScheduleTimer(context, prefs)
        }
    }

    private fun sendDeathNotification(context: Context, prefs: android.content.SharedPreferences) {
        val json = prefs.getString("saved_contacts", null)
        val customMsg = prefs.getString("sms_content", "这是来自生命监测系统的紧急通知：该用户已超过24小时未签到，疑似发生意外，请立即确认其安全。")

        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<Contact>>() {}.type
            val contacts: MutableList<Contact> = Gson().fromJson(json, type)

            if (contacts.isEmpty()) return // 保护逻辑：无联系人不执行

            // 获取短信管理器
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // 循环发送给所有紧急联系人
            contacts.forEach { contact ->
                try {
                    smsManager.sendTextMessage(contact.phone, null, customMsg, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun reScheduleTimer(context: Context, prefs: android.content.SharedPreferences) {
        val lastCheckIn = prefs.getLong("last_check_in_time", 0)
        if (lastCheckIn == 0L) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DeathSwitchReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算下一次触发的时间点：上次签到时间 + 24小时
        val triggerTime = lastCheckIn + 24 * 60 * 60 * 1000L

        // 如果计算出的时间已经过去，则立即触发；否则设定定时器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}