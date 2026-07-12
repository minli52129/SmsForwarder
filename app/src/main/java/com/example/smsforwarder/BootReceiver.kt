package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            // 这里仅仅是让系统在开机时唤醒一下应用进程
            // 具体的短信监听由注册在 AndroidManifest.xml 的 SmsReceiver 自动处理
        }
    }
}
