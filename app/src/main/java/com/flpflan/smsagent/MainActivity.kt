package com.flpflan.smsagent

import android.content.IntentFilter
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


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askForPermission()
        val logger = Logger.getLogger("smsAgent")
        logger.level = Level.INFO
        logger.addHandler(
            FileHandler("${getExternalFilesDir("logs")}/smsAgent%g.log", 1024 * 1024 * 10, 1, false)
        )

        logger.info("SmsAgent start")
        WorkManager
            .getInstance(this)
            .enqueue(OneTimeWorkRequest.from(SmsAgent::class.java))

        val hook = SmsHook()
        registerReceiver(
            hook, IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        )

        setContent {
            Config(context = this)
        }
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