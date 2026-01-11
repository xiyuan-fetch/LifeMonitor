package com.example.deadmanswitch // 确保包名正确

import android.content.Context
import android.telephony.SmsManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.app.usage.UsageStatsManager
import java.util.*

class CheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // 1. 获取用户保存的手机号和遗言 (从 SharedPreferences)
        val sharedPref = applicationContext.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val phone = sharedPref.getString("phone", "") ?: ""
        val message = sharedPref.getString("message", "") ?: ""
        val lastCheckIn = sharedPref.getLong("last_check_in", 0L)

        if (phone.isEmpty()) return Result.success()

        // 2. 计算当前时间与最后一次手动签到的时间差 (判断是否超过24小时)
        val isExpired = System.currentTimeMillis() - lastCheckIn > 24 * 60 * 60 * 1000

        // 3. 获取今日屏幕使用时长
        val usageMinutes = getTodayUsageMinutes()

        // 4. 核心逻辑判断：如果超过24小时未签到 且 今日时长 < 10分钟
        if (isExpired && usageMinutes < 10) {
            sendSms(phone, message)
        }

        return Result.success()
    }

    private fun getTodayUsageMinutes(): Long {
        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis())
        var totalTime = 0L
        stats?.forEach { totalTime += it.totalTimeInForeground }
        return totalTime / (1000 * 60)
    }

    private fun sendSms(phone: String, msg: String) {
        try {
            val smsManager = applicationContext.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phone, null, msg, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}