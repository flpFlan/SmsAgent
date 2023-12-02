package com.flpflan.smsagent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.provider.Telephony.Sms
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ArrayBlockingQueue
import java.util.logging.Logger

private val logger = Logger.getLogger("smsAgent")
private val buffer = ArrayBlockingQueue<SMS>(64)

data class SMS(
    val iccIndex: Int,
    val time: LocalDateTime,
    val sender: String,
    val text: String
)

object SmsHook : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        for (msg in Sms.Intents.getMessagesFromIntent(intent)) {
            val iccIndex = msg.indexOnIcc
            val sender = msg.displayOriginatingAddress
            val millis = msg.timestampMillis
            val text = msg.displayMessageBody

            val time =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
            buffer.put(SMS(iccIndex, time, sender.toString(), text))
            logger.info("received sms, iccIndex:$iccIndex time:$time sender:$sender text:$text")
        }
    }

    fun doHook(wrapper: ContextWrapper) {
        logger.info("Register sms hook")
        val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        wrapper.registerReceiver(this, filter)
    }

    fun undoHook(wrapper: ContextWrapper) {
        logger.info("Unregister sms hook")
        wrapper.unregisterReceiver(this)
    }
}

data class CallbackPayload(
    val time: String,
    val sender: String,
    val receiver: String,
    val text: String
)


class SmsAgent(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val _req = Request(context.applicationContext)
    private val _gson = Gson()

    override fun doWork(): Result {
        logger.info("SmsAgent start loop")
        while (true) {
            val sms = buffer.take()
            handleSMS(sms)
        }
        return Result.success()
    }

    private fun handleSMS(sms: SMS) {
        if (!checkSMSValidity(sms)) return
        val payload =
            CallbackPayload(sms.time.toString(), sms.sender, Configuration.Receiver, sms.text)
        val serialized = _gson.toJson(payload)
        logger.info("posting to ${Configuration.Callback}, content:${serialized}")
        _req.post(Configuration.Callback, serialized)
        deleteSMS(sms)
    }

    private fun checkSMSValidity(sms: SMS): Boolean {
        return sms.text.toULongOrNull(0x10) != null
    }

    private fun deleteSMS(sms: SMS) {
        // NOTE: Something goes wrong with deleting sms on Android...

//        val resolver = applicationContext.contentResolver
//        val date = sms.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
//        val filter = "date=$date and address='${sms.sender}' and body=${sms.text}"
////          "index_on_icc='${sms.iccIndex}'"
//
//        Thread.sleep(500) // allow SmsProvider to store this sms
//        val deletedCount = resolver.delete(Sms.Inbox.CONTENT_URI, filter, null)
//        logger.info(
//            "delete $deletedCount sms, " +
////               "iccIndex:${sms.iccIndex}, " +
//                    "time:${sms.time}, " +
//                    "sender:${sms.sender}, " +
//                    "text:${sms.text}"
//        )
    }
}