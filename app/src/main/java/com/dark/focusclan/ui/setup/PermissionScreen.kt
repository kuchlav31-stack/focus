package com.dark.focusclan.ui.setup

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.dark.focusclan.utils.FocusAdminReceiver
import com.dark.focusclan.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    // --- State Management for 5 Core Permissions ---
    var overlayGranted by remember { mutableStateOf(PermissionUtils.hasOverlayPermission(context)) }
    var usageGranted by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    var adminGranted by remember { mutableStateOf(PermissionUtils.hasDeviceAdminPermission(context)) }

    // Nayi Permissions: Battery aur DND
    var batteryGranted by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var dndGranted by remember { mutableStateOf(isDndPermissionGranted(context)) }

    // Re-check logic jab user Settings se wapis aaye
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = PermissionUtils.hasOverlayPermission(context)
                usageGranted = PermissionUtils.hasUsageStatsPermission(context)
                adminGranted = PermissionUtils.hasDeviceAdminPermission(context)
                batteryGranted = isBatteryOptimizationIgnored(context)
                dndGranted = isDndPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allDone = overlayGranted && usageGranted && adminGranted && batteryGranted && dndGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // --- Header Section ---
        Text(
            text = "Nuclear Setup",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Activate these shields for an unbreakable lock.",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- Permission Cards List ---

        // 1. Overlay (Lock Screen UI)
        PermissionRow(
            title = "Display Over Apps",
            description = "Crucial for showing the lockdown timer.",
            isGranted = overlayGranted,
            onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            }
        )

        // 2. Usage Stats (Detect Distractions)
        PermissionRow(
            title = "Usage Access",
            description = "Helps the clan detect if you open other apps.",
            isGranted = usageGranted,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        )

        // 3. Device Admin (Uninstall Protection)
        PermissionRow(
            title = "Device Administrator",
            description = "Prevents stopping or deleting the app during battle.",
            isGranted = adminGranted,
            onClick = {
                val componentName = ComponentName(context, FocusAdminReceiver::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "FocusClan needs this to prevent bypass.")
                }
                context.startActivity(intent)
            }
        )

        // 4. Battery Optimization (Immortal Service)
        PermissionRow(
            title = "Disable Battery Saver",
            description = "Keeps the lock active even in the background.",
            isGranted = batteryGranted,
            onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        )

        // 5. DND Access (Pure Silence)
        PermissionRow(
            title = "Notification Policy",
            description = "Mutes all sounds to ensure deep work focus.",
            isGranted = dndGranted,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- Final Action Button ---
        Button(
            onClick = {
                if (allDone) {
                    navController.navigate("home") {
                        popUpTo("permission_setup") { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            enabled = allDone,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allDone) Color(0xFF00E676) else Color(0xFF1E1E1E),
                disabledContainerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (allDone) "ENTER CLAN HUB" else "GRANT ALL SHIELDS",
                color = if (allDone) Color.Black else Color.DarkGray,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFF00E676).copy(alpha = 0.05f) else Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (isGranted) BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
        else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isGranted) Color(0xFF00E676) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            IconButton(
                onClick = onClick,
                enabled = !isGranted,
                modifier = Modifier
                    .background(
                        if (isGranted) Color.Transparent else Color(0xFF252525),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF00E676) else Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// --- Internal Helper Functions ---

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun isDndPermissionGranted(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.isNotificationPolicyAccessGranted
}