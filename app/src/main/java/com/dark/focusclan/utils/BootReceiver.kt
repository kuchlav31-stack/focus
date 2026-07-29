package com.dark.focusclan.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dark.focusclan.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val prefs = context.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
            val isLocked = prefs.getBoolean("isChallengeActive", false)
            val endTime = prefs.getLong("endTime", 0L)

            // Agar timer bacha hai toh app kholo
            if (isLocked && System.currentTimeMillis() < endTime) {
                val i = Intent(context, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            }
        }
    }
}