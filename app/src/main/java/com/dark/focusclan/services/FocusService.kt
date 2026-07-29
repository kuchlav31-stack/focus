package com.dark.focusclan.services

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dark.focusclan.MainActivity
import com.dark.focusclan.R

class FocusService : Service() {

    private var isLocked = true
    private val CHANNEL_ID = "NuclearLockChannel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nuclear Lockdown Active")
            .setContentText("Finish your goal to unlock your phone.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true) // User isey swip karke hata nahi sakta
            .build()

        startForeground(101, notification)
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        Thread {
            while (isLocked) {
                val currentApp = getForegroundApp()

                // WHITELIST: In apps ke khulne par app wapis nahi kheechega
                val whitelistedApps = listOf(
                    packageName,
                    "com.android.dialer",
                    "com.google.android.dialer",
                    "com.android.incallui",
                    "com.android.contacts",
                    "com.samsung.android.incallui" // Samsung calls ke liye
                )

                if (currentApp != null && currentApp !in whitelistedApps) {
                    // PULL BACK LOGIC: User ne koi aur app khola, wapis lao!
                    val lockIntent = Intent(this, MainActivity::class.java)
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(lockIntent)
                }
                Thread.sleep(500) // Har 0.5 second mein check karo
            }
        }.start()
    }

    private fun getForegroundApp(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Focus Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isLocked = false // Timer khatam hone par hi band hoga
        super.onDestroy()
    }
}