package com.flpflan.smsagent

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.logging.Logger

private val logger = Logger.getLogger("smsAgent")

class Request(context: Context) {
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
        val req = StringRequest(
            Request.Method.GET, url,
            { resp ->
                logger.info(resp)
            },
            { err -> logger.warning(err.toString()) })
        _queue.add(req)
    }
}