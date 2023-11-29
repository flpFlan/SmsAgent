package com.flpflan.smsagent.ui

import android.content.Context
import android.content.DialogInterface
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flpflan.smsagent.Configuration

@Composable
fun Config(context: Context) {
    Column {
        Callback(context = context)
        Receiver(context = context)
    }
}

@Composable
private fun Callback(context: Context) {
    ListItem(
        headlineContent = { Text("Callback") },
//        overlineContent = { Text("callback") },
        modifier = Modifier.clickable {
            val cbEdit = EditText(context)
            cbEdit.setText(Configuration.Callback)

            val dialog = AlertDialog.Builder(context)
            dialog.setTitle("Callback")
            dialog.setView(cbEdit)
            dialog.setPositiveButton("保存") { _: DialogInterface, _: Int ->
                Configuration.Callback = cbEdit.text.toString()
            }
            dialog.setNegativeButton("取消", null)
            dialog.show()
        }
    )
    HorizontalDivider()
}

@Composable
private fun Receiver(context: Context) {
    ListItem(
        headlineContent = { Text("Receiver") },
//        overlineContent = { Text("receiver") },
        modifier = Modifier.clickable {
            val rcrEdit = EditText(context)
            rcrEdit.setText(Configuration.Receiver)

            val dialog = AlertDialog.Builder(context)
            dialog.setTitle("Receiver")
            dialog.setView(rcrEdit)
            dialog.setPositiveButton("保存") { _: DialogInterface, _: Int ->
                Configuration.Receiver = rcrEdit.text.toString()
            }
            dialog.setNegativeButton("取消", null)
            dialog.show()
        }
    )
    HorizontalDivider()
}