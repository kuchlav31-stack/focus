package com.dark.focusclan.utils

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.provider.Settings

object PermissionUtils {

    /**
     * 1. Check Overlay Permission
     * System ke upar lock screen dikhane ke liye zaroori hai.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 2. Check Usage Access Permission
     * User ne koi aur app (Instagram, etc.) khola hai ya nahi, ye check karne ke liye.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * 3. Check Device Admin Permission
     * App ko uninstall hone se rokne ke liye.
     */
    fun hasDeviceAdminPermission(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        // Yahan FocusAdminReceiver wahi class hai jo humne Receiver ke liye banayi thi
        val adminComponent = ComponentName(context, FocusAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    /**
     * 4. Check All Permissions
     * Splash screen aur setup screen par ye check karne ke liye ki kya sab allow hai.
     */
    fun allPermissionsGranted(context: Context): Boolean {
        return hasOverlayPermission(context) &&
                hasUsageStatsPermission(context) &&
                hasDeviceAdminPermission(context)
    }
}