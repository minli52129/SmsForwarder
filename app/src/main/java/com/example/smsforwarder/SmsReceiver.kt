package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            var fullMessage = ""
            var sender = ""

            for (msg in messages) {
                if (msg != null) {
                    sender = msg.originatingAddress ?: "未知号码"
                    fullMessage += msg.messageBody
                }
            }

            if (fullMessage.contains("龙腾")) {
                val dbHelper = LogDatabaseHelper(context)
                
                // Keep the broadcast receiver alive during async work
                val pendingResult = goAsync()
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val content = "收到来自 $sender 的短信:\n$fullMessage"
                        var statusStr = ""

                        // 1. 发送到 NTFY
                        val ntfySuccess = sendToNtfy(content)
                        statusStr += if (ntfySuccess) "NTFY:成功 " else "NTFY:失败 "

                        // 2. 发送到飞书
                        val feishuSuccess = sendToFeishu(content)
                        statusStr += if (feishuSuccess) "飞书:成功" else "飞书:失败"

                        dbHelper.addLog("来自: $sender\n内容: $fullMessage", statusStr)
                    } catch (e: Exception) {
                        dbHelper.addLog("转发异常: ${e.message}", "失败")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun sendToNtfy(message: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://minli52129.onrender.com/sms")
                .post(message.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun sendToFeishu(message: String): Boolean {
        return try {
            val json = JSONObject()
            json.put("msg_type", "text")
            val contentObj = JSONObject()
            contentObj.put("text", message)
            json.put("content", contentObj)

            val request = Request.Builder()
                .url("https://open.feishu.cn/open-apis/bot/v2/hook/b7e0c9a3-453f-49dc-a36a-d350c966aaef")
                .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
