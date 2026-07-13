package com.example.smsforwarder

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class RcsNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification

        // 常见短信应用的包名
        val smsPackages = listOf(
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.miui.smsextra",
            "com.samsung.android.messaging"
        )

        // 只有短信应用的通知才处理，或者类别是 CATEGORY_MESSAGE 的
        if (smsPackages.contains(packageName) || notification.category == Notification.CATEGORY_MESSAGE) {
            val extras = notification.extras
            
            // 尝试获取发送者和内容
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // 去除可能为空的通知
            if (title.isNotEmpty() && text.isNotEmpty()) {
                // 如果是正在发送、发送失败之类的状态栏通知，最好过滤掉。这里简单判断一下。
                if (!text.contains("正在发送") && !text.contains("发送失败")) {
                    // 防止同一条消息重复处理。简单去重可以基于缓存，由于这里只是触发转发，我们直接转发。
                    // 真实的场景可能需要记录最近发送的 message 内容避免短时间内重复转发
                    ForwarderHelper.processMessage(applicationContext, title, text)
                }
            }
        }
    }
}
