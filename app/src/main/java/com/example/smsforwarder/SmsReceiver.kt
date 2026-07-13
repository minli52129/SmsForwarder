package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
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
            
            // Keep the broadcast receiver alive slightly longer to process
            val pendingResult = goAsync()
            
            ForwarderHelper.processMessage(context, sender, fullMessage)
            
            pendingResult.finish()
        }
    }
}
