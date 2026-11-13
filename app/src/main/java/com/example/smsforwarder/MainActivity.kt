package com.example.smsforwarder

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var logText: TextView
    private lateinit var dbHelper: LogDatabaseHelper
    
    private lateinit var etKeyword: EditText
    private lateinit var etNtfyUrl: EditText
    private lateinit var etFeishuUrl: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainScrollView = findViewById<View>(R.id.mainScrollView)
        ViewCompat.setOnApplyWindowInsetsListener(mainScrollView) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        logText = findViewById(R.id.logText)
        dbHelper = LogDatabaseHelper(this)
        
        etKeyword = findViewById(R.id.etKeyword)
        etNtfyUrl = findViewById(R.id.etNtfyUrl)
        etFeishuUrl = findViewById(R.id.etFeishuUrl)

        loadSettings()

        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            saveSettings()
        }

        findViewById<Button>(R.id.btnPermissions).setOnClickListener {
            checkAndRequestPermissions()
        }

        findViewById<Button>(R.id.btnAppInfo).setOnClickListener {
            openAppDetailsSettings()
        }

        findViewById<Button>(R.id.btnNotificationAccess).setOnClickListener {
            openNotificationAccessSettings()
        }

        findViewById<Button>(R.id.btnIgnoreBattery).setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        findViewById<Button>(R.id.btnXiaomiAutoStart).setOnClickListener {
            openXiaomiAutoStartSettings()
        }

        refreshLogs()
    }

    override fun onResume() {
        super.onResume()
        refreshLogs()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("SmsSettings", Context.MODE_PRIVATE)
        etKeyword.setText(prefs.getString("keyword", ""))
        etNtfyUrl.setText(prefs.getString("ntfy_url", "https://minli52129.onrender.com/duanxin"))
        etFeishuUrl.setText(prefs.getString("feishu_url", ""))
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("SmsSettings", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("keyword", etKeyword.text.toString().trim())
            putString("ntfy_url", etNtfyUrl.text.toString().trim())
            putString("feishu_url", etFeishuUrl.text.toString().trim())
            apply()
        }
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), 100)
        } else {
            Toast.makeText(this, "常规短信权限已授予", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppDetailsSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            Toast.makeText(this, "请允许【通知类短信】；Android 15-17 请在右上角点击【允许受限设置】", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNotificationAccessSettings() {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
            Toast.makeText(this, "请找到【短信转发】并允许通知读取（若灰色请先在应用信息右上角允许受限设置）", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开通知读取设置，请手动在设置中搜索“通知读取”", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                Toast.makeText(this, "已忽略电池优化", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openXiaomiAutoStartSettings() {
        try {
            val intent = Intent()
            intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                Toast.makeText(this, "请在设置中手动开启自启动", Toast.LENGTH_LONG).show()
            } catch (ex: Exception) {
                Toast.makeText(this, "无法打开自启动设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshLogs() {
        val logs = dbHelper.getAllLogs()
        if (logs.isEmpty()) {
            logText.text = "暂无转发记录..."
        } else {
            logText.text = logs.joinToString("\n\n")
        }
    }
}
