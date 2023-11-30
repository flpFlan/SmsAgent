package com.flpflan.smsagent

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import com.flpflan.smsagent.ui.Config
import java.util.logging.SimpleFormatter

private val logger = Logger.getLogger("smsAgent")


class MainActivity : AppCompatActivity() {
    private var _hook: SmsHook = SmsHook()
    private var _worker: OneTimeWorkRequest = OneTimeWorkRequest.from(SmsAgent::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askForPermission()

        logger.level = Level.INFO
        val handler =
            FileHandler("${getExternalFilesDir("logs")}/smsAgent%g.log", 1024 * 1024 * 10, 1, false)
        handler.formatter = SimpleFormatter()
        logger.addHandler(handler)

        logger.info("SmsAgent initializing")
        WorkManager
            .getInstance(this)
            .enqueue(_worker)
        _hook.doHook(this)

        setContent {
            Config(context = this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _hook.undoHook(this)
        WorkManager.getInstance(this).cancelAllWork()
    }

    private fun askForPermission() {
        if (this.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
            this.checkSelfPermission(android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
        ) {
            this.requestPermissions(
                arrayOf(
                    android.Manifest.permission.RECEIVE_SMS,
                    android.Manifest.permission.INTERNET
                ), PackageManager.PERMISSION_GRANTED
            )
        }
    }
}