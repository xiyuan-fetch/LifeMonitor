package com.example.deadmanswitch

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsActivity : AppCompatActivity() {
    private var contactList = mutableListOf<Contact>()
    private lateinit var adapter: ContactAdapter
    private var selectedUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("LifeMonitorPrefs", Context.MODE_PRIVATE)

        // 1. 初始化列表与长按删除
        loadContacts(prefs)
        val rvContacts = findViewById<RecyclerView>(R.id.rvContacts)
        adapter = ContactAdapter(contactList) { position ->
            showDeleteConfirmDialog(position, prefs)
        }
        rvContacts.layoutManager = LinearLayoutManager(this)
        rvContacts.adapter = adapter

        val etName = findViewById<EditText>(R.id.etName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etSms = findViewById<EditText>(R.id.etCustomSms)
        val tvPreview = findViewById<TextView>(R.id.tvSmsPreview)
        val sbBlur = findViewById<SeekBar>(R.id.sbBlur)

        // 修复截图中的空安全报错：使用 ?. 或 ifEmpty 处理 null
        val savedSms = prefs.getString("sms_content", "") ?: ""
        etSms.setText(savedSms)
        tvPreview.text = "预览：${savedSms.ifEmpty { "默认求救短信" }}"

        etSms.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // 确保 s 不为 null
                tvPreview.text = "预览：${s?.toString() ?: ""}"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<Button>(R.id.btnAddContact).setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                contactList.add(Contact(name, phone))
                adapter.notifyItemInserted(contactList.size - 1)
                saveContacts(prefs)
                etName.text.clear()
                etPhone.text.clear()
            }
        }

        findViewById<Button>(R.id.btnTestSms).setOnClickListener {
            val content = etSms.text.toString().ifEmpty { "测试短信" }
            if (contactList.isEmpty()) {
                Toast.makeText(this, "请先添加联系人", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 修复 SmsManager 过时警告
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            contactList.forEach { contact ->
                try {
                    smsManager.sendTextMessage(contact.phone, null, content, null, null)
                } catch (e: Exception) { e.printStackTrace() }
            }
            Toast.makeText(this, "测试已发出", Toast.LENGTH_SHORT).show()
        }

        val pickImg = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedUri = it.toString()
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        findViewById<Button>(R.id.btnPickBg).setOnClickListener { pickImg.launch("image/*") }

        findViewById<Button>(R.id.btnSaveAll).setOnClickListener {
            prefs.edit().apply {
                putString("sms_content", etSms.text.toString())
                putFloat("blur_level", sbBlur.progress.toFloat())
                selectedUri?.let { putString("bg_uri", it) }
                apply()
            }
            saveContacts(prefs)
            finish()
        }
    }

    private fun showDeleteConfirmDialog(position: Int, prefs: android.content.SharedPreferences) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("要移除 ${contactList[position].name} 吗？")
            .setPositiveButton("删除") { _, _ ->
                contactList.removeAt(position)
                adapter.notifyDataSetChanged()
                saveContacts(prefs)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveContacts(prefs: android.content.SharedPreferences) {
        val json = Gson().toJson(contactList)
        prefs.edit().putString("saved_contacts", json).apply()
    }

    private fun loadContacts(prefs: android.content.SharedPreferences) {
        val json = prefs.getString("saved_contacts", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<Contact>>() {}.type
            contactList = Gson().fromJson(json, type)
        }
    }
}