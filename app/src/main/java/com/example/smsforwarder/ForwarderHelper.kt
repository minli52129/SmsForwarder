package com.example.smsforwarder

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ForwarderHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // 用于防止同一条消息由于短信广播和通知监听器同时触发而重复转发
    private val recentMessages = mutableMapOf<String, Long>()

    fun processMessage(context: Context, sender: String, messageBody: String) {
        val now = System.currentTimeMillis()
        // 清理 10 秒前的缓存
        recentMessages.entries.removeIf { now - it.value > 10000 }

        val msgKey = "$sender:$messageBody"
        if (recentMessages.containsKey(msgKey)) {
            return // 短时间内已经处理过该消息，直接忽略
        }
        recentMessages[msgKey] = now

        val prefs = context.getSharedPreferences("SmsSettings", Context.MODE_PRIVATE)
        val keyword = prefs.getString("keyword", "") ?: ""
        val ntfyUrl = prefs.getString("ntfy_url", "https://minli52129.onrender.com/duanxin") ?: ""
        val feishuUrl = prefs.getString("feishu_url", "") ?: ""

        if (keyword.isEmpty() || messageBody.contains(keyword)) {
            val dbHelper = LogDatabaseHelper(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val content = "收到来自 $sender 的消息:\n$messageBody"
                    var statusStr = ""

                    // 1. 发送到 NTFY
                    if (ntfyUrl.isNotEmpty()) {
                        val ntfySuccess = sendToNtfy(ntfyUrl, content)
                        statusStr += if (ntfySuccess) "NTFY:成功 " else "NTFY:失败 "
                    } else {
                        statusStr += "NTFY:未配置 "
                    }

                    // 2. 发送到飞书
                    if (feishuUrl.isNotEmpty()) {
                        val feishuSuccess = sendToFeishu(feishuUrl, content)
                        statusStr += if (feishuSuccess) "飞书:成功" else "飞书:失败"
                    } else {
                        statusStr += "飞书:未配置"
                    }

                    dbHelper.addLog("来自: $sender\n内容: $messageBody", statusStr)
                } catch (e: Exception) {
                    dbHelper.addLog("转发异常: ${e.message}", "失败")
                }
            }
        }
    }

    private fun sendToNtfy(url: String, message: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .post(message.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun sendToFeishu(url: String, message: String): Boolean {
        return try {
            val json = JSONObject()
            json.put("msg_type", "text")
            val contentObj = JSONObject()
            contentObj.put("text", message)
            json.put("content", contentObj)

            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
