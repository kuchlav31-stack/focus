package com.dark.focusclan.ui.setup

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun PermissionScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // States for Permissions
    var overlayGranted by remember { mutableStateOf(PermissionUtils.hasOverlayPermission(context)) }
    var usageGranted by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    var adminGranted by remember { mutableStateOf(PermissionUtils.hasDeviceAdminPermission(context)) }

    // Automatic Re-check Logic: Jab user settings se wapis app mein aaye
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Settings se wapis aate hi check karo
                overlayGranted = PermissionUtils.hasOverlayPermission(context)
                usageGranted = PermissionUtils.hasUsageStatsPermission(context)
                adminGranted = PermissionUtils.hasDeviceAdminPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allDone = overlayGranted && usageGranted && adminGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Nuclear Setup",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Grant permissions to activate the lock",
            color = Color.Gray,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 1. Overlay Card
        PermissionRow(
            title = "Display Over Other Apps",
            description = "Allows showing the lock screen timer.",
            isGranted = overlayGranted,
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )

        // 2. Usage Stats Card
        PermissionRow(
            title = "Usage Access",
            description = "Detects if you try to open other apps.",
            isGranted = usageGranted,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        )

        // 3. Device Admin Card
        PermissionRow(
            title = "Device Administrator",
            description = "Prevents app uninstallation during focus.",
            isGranted = adminGranted,
            onClick = {
                val componentName = ComponentName(context, FocusAdminReceiver::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "FocusClan needs this to protect your focus session.")
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Continue Button
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
            enabled = allDone, // Jab tak sab allow na ho, button kaam nahi karega
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allDone) Color(0xFF00E676) else Color.DarkGray,
                disabledContainerColor = Color(0xFF1E1E1E)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (allDone) "ENTER CLAN" else "GRANT ALL TO CONTINUE",
                color = if (allDone) Color.Black else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
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
            containerColor = if (isGranted) Color(0xFF00E676).copy(alpha = 0.1f) else Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isGranted) null else androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
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
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onClick,
                enabled = !isGranted, // Allow hone ke baad click nahi hoga
                modifier = Modifier
                    .background(
                        if (isGranted) Color.Transparent else Color(0xFF333333),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF00E676) else Color.Yellow
                )
            }
        }
    }
}