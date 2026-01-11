package com.example.deadmanswitch

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // 1. 权限请求器：启动即索要短信权限
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        if (!smsGranted) {
            Toast.makeText(this, "警告：未授予短信权限，紧急求助功能将失效！", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. 执行权限检查
        checkAndRequestPermissions()

        val btnCheckIn = findViewById<Button>(R.id.btnCheckIn)

        // 3. 初始化日历历史和签到状态
        setupCalendarGrid()
        checkCheckInStatus(btnCheckIn)

        // 4. 签到按钮逻辑
        btnCheckIn.setOnClickListener {
            // Android 12+ 精准闹钟特殊权限检查
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "请授予精准闹钟权限以保证监测运行", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    return@setOnClickListener
                }
            }
            handleCheckInAction(btnCheckIn)
        }

        // 跳转设置
        findViewById<ImageButton>(R.id.btnToSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(Manifest.permission.SEND_SMS)
        // 旧版本安卓额外申请存储权限用于背景图片读取
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun handleCheckInAction(btn: Button) {
        val prefs = getSharedPreferences("LifeMonitorPrefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val todayStr = SimpleDateFormat("M月d日", Locale.CHINA).format(Date(now))

        // A. 更新历史记录 (JSON 存储)
        val historyJson = prefs.getString("check_in_history", "[]") ?: "[]"
        val historyType = object : TypeToken<MutableSet<String>>() {}.type
        val historySet: MutableSet<String> = Gson().fromJson(historyJson, historyType)
        historySet.add(todayStr)

        prefs.edit().apply {
            putLong("last_check_in_time", now)
            putString("check_in_history", Gson().toJson(historySet))
            apply()
        }

        // B. 界面反馈
        btn.text = "已签到"
        btn.isEnabled = false
        applyMonetColor(btn)

        // C. 启动/重置 24小时死亡开关
        scheduleDeathTimer(this)

        // D. 刷新日历
        setupCalendarGrid()

        Toast.makeText(this, "签到成功，监测已重置", Toast.LENGTH_SHORT).show()
    }

    private fun setupCalendarGrid() {
        val gvCalendar = findViewById<GridView>(R.id.gvCalendar)
        val prefs = getSharedPreferences("LifeMonitorPrefs", Context.MODE_PRIVATE)

        val historyJson = prefs.getString("check_in_history", "[]") ?: "[]"
        val historySet: Set<String> = Gson().fromJson(historyJson, object : TypeToken<Set<String>>() {}.type)

        // 生成最近 15 天日期
        val dateList = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -14)
        for (i in 0..14) {
            dateList.add(SimpleDateFormat("M月d日", Locale.CHINA).format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        gvCalendar.adapter = object : BaseAdapter() {
            override fun getCount(): Int = dateList.size
            override fun getItem(p: Int) = dateList[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val date = dateList[position]
                val isDone = historySet.contains(date)

                val tv = TextView(this@MainActivity).apply {
                    text = "$date\n${if (isDone) "●" else "○"}"
                    gravity = Gravity.CENTER
                    setPadding(0, 25, 0, 25)
                    textSize = 12f
                    // 动态样式：签到为绿，未签到为半透明白
                    if (isDone) {
                        setBackgroundColor(Color.parseColor("#4CAF50"))
                        setTextColor(Color.WHITE)
                    } else {
                        setBackgroundColor(Color.parseColor("#1AFFFFFF"))
                        setTextColor(Color.LTGRAY)
                    }
                }
                return tv
            }
        }
    }

    private fun checkCheckInStatus(btn: Button) {
        val prefs = getSharedPreferences("LifeMonitorPrefs", Context.MODE_PRIVATE)
        val lastCheckIn = prefs.getLong("last_check_in_time", 0)

        if (lastCheckIn != 0L) {
            val lastDate = Calendar.getInstance().apply { timeInMillis = lastCheckIn }
            val now = Calendar.getInstance()

            if (lastDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                lastDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                btn.text = "已签到"
                btn.isEnabled = false
                applyMonetColor(btn)
            }
        }
    }

    private fun scheduleDeathTimer(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DeathSwitchReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun applyMonetColor(btn: Button) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val colors = WallpaperManager.getInstance(this).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            btn.setTextColor(colors?.primaryColor?.toArgb() ?: Color.GREEN)
        } else {
            btn.setTextColor(Color.GREEN)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshBackground()
        setupCalendarGrid() // 确保跨天时刷新网格
    }

    private fun refreshBackground() {
        val prefs = getSharedPreferences("LifeMonitorPrefs", Context.MODE_PRIVATE)
        val ivBg = findViewById<ImageView>(R.id.ivMainBg)
        val uriStr = prefs.getString("bg_uri", null)
        val blurVal = prefs.getFloat("blur_level", 20f)
        uriStr?.let {
            try {
                ivBg.setImageURI(Uri.parse(it))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ivBg.setRenderEffect(RenderEffect.createBlurEffect(blurVal + 0.1f, blurVal + 0.1f, Shader.TileMode.CLAMP))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}