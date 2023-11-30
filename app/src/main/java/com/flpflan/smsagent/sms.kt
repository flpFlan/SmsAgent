package com.flpflan.smsagent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.provider.Telephony.Sms
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ArrayBlockingQueue
import java.util.logging.Logger

private val logger = Logger.getLogger("smsAgent")
private val buffer = ArrayBlockingQueue<SMS>(64)

data class SMS(
    val status: String,
    val time: LocalDateTime,
    val sender: String,
    val text: String
)

class SmsHook : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        for (msg in Sms.Intents.getMessagesFromIntent(intent)) {
            val sender = msg.originatingAddress
            val millis = msg.timestampMillis
            val status = msg.status
            val text = msg.displayMessageBody

            val time =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
            buffer.put(SMS(status.toString(), time, sender.toString(), text))
            logger.info("received sms status:$status time:$time sender:$sender text:$text")
        }
    }

    fun doHook(wrapper: ContextWrapper) {
        val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        wrapper.registerReceiver(this, filter)
    }

    fun undoHook(wrapper: ContextWrapper) {
        wrapper.unregisterReceiver(this)
    }
}

data class CallbackPayload(
    val time: String, val sender: String, val receiver: String, val text: String
) : Serializable {
    fun serialize(): String {
        return """{"sender":"$sender","text":"$text"}"""
    }
}


class SmsAgent(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val _req = Request(context.applicationContext)

    override fun doWork(): Result {
        logger.info("SmsAgent start loop")
        while (true) {
            val sms = buffer.take()
            handleSMS(sms)
        }
        return Result.success()
    }

    private fun handleSMS(sms: SMS) {
        val payload =
            CallbackPayload(sms.time.toString(), sms.sender, Configuration.Receiver, sms.text)
        logger.info("posting to ${Configuration.Callback}, content:${payload.serialize()}")
        _req.post(Configuration.Callback, payload.serialize())
    }
}

private class Request(context: Context) {
    private val _queue: RequestQueue

    init {
        this._queue = Volley.newRequestQueue(context)
    }

    fun post(url: String, content: String) {
        val req = object : JsonObjectRequest(
            Method.POST,
            url,
            JSONObject(content),
            { resp ->
                logger.info("Server response: $resp")
            },
            { error ->
                logger.warning("Post error: $error message:${error.message}")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        _queue.add(req)
    }

    fun get(url: String) {
        val req = StringRequest(Request.Method.GET, url,
            { resp ->
                logger.info(resp)
            },
            { err -> logger.warning(err.toString()) })
        _queue.add(req)
    }
}