package com.flpflan.smsagent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
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
    val smsc: String,
    val time: LocalDateTime,
    val sender: String,
    val text: String
)

data class CallbackPayload(
    val time: String, val sender: String, val receiver: String, val text: String
) : Serializable {
    fun serialize(): String {
        return """{"sender":$sender,"text":$text}"""
    }
}


class SmsAgent(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val _req = Request(ContextWrapper(context))

    override fun doWork(): Result {
        while (true) {
            val sms = buffer.take()
            handleSMS(sms)
        }
        return Result.success()
    }

    private fun handleSMS(sms: SMS) {
        val payload =
            CallbackPayload(sms.time.toString(), sms.sender, Configuration.Receiver, sms.text)
        logger.info("posting to ${Configuration.Callback}")
        _req.post(Configuration.Callback, payload.serialize())
    }
}

class SmsHook : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        for (msg in Sms.Intents.getMessagesFromIntent(intent)) {
            val sender = msg.originatingAddress
            val millis = msg.timestampMillis
            val status = msg.status
            val text = msg.displayMessageBody

            val time =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            buffer.put(SMS(status.toString(), time, sender.toString(), text))
            logger.info("received sms status:$status time:$time sender:$sender text:$text")
        }
    }

}

private class Request(contextWrapper: ContextWrapper) {
    private val _queue: RequestQueue

    init {
        this._queue = Volley.newRequestQueue(contextWrapper)
    }

    fun post(url: String, content: String) {

        val req = JsonObjectRequest(Request.Method.POST, url, JSONObject(content), { resp ->
            run {
                logger.info("Server response: $resp")
            }
        }, { error ->
            run {
                logger.warning("Error post callback: $error")
            }
        })
        req.headers["Content-Type"] = "application/json"
        _queue.add(req)
    }
}