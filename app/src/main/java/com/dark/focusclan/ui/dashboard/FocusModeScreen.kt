package com.dark.focusclan.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.dark.focusclan.services.FocusService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun FocusModeScreen(
    navController: NavController,
    challengeId: String,
    duration: Int,
    mode: Int
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val prefs = remember { context.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE) }

    // --- State Management ---
    var timeLeft by remember { mutableLongStateOf(duration * 60L) }
    var isFinished by remember { mutableStateOf(false) }

    // Mode based colors
    val themeColor = when (mode) {
        1 -> Color(0xFF00E676) // Basic
        2 -> Color(0xFFFFA500) // Advanced
        else -> Color.Red      // Nuclear
    }

    // 1. Back Button Block (Only for Advanced and Nuclear)
    BackHandler(enabled = !isFinished && mode > 1) {
        // Lockdown: Do nothing
    }

    // --- Core Logic ---
    LaunchedEffect(Unit) {
        // A. Online Status Update
        db.collection("users").document(uid).update("isFocusing", true)

        // B. Nuclear Mode Restart Protection
        if (mode == 3) {
            val endTime = System.currentTimeMillis() + (timeLeft * 1000)
            prefs.edit().apply {
                putBoolean("isChallengeActive", true)
                putLong("endTime", endTime)
                putInt("activeMode", 3)
                putString("activeChallengeId", challengeId)
                apply()
            }
        }

        // C. Start Blocking Service
        if (mode >= 2) {
            val intent = Intent(context, FocusService::class.java)
            context.startForegroundService(intent)
        }

        // D. Timer Loop
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }

        // --- Session Success ---
        isFinished = true

        // Update Firestore Stats
        val updates = hashMapOf<String, Any>(
            "isFocusing" to false,
            "coins" to FieldValue.increment(duration.toLong()),
            "totalHours" to FieldValue.increment(duration / 60.0),
            "streak" to FieldValue.increment(1)
        )
        db.collection("users").document(uid).update(updates)

        // Cleanup
        if (mode == 3) prefs.edit().putBoolean("isChallengeActive", false).apply()
        if (mode >= 2) context.stopService(Intent(context, FocusService::class.java))
    }

    // --- UI Layout ---
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mode Header Label
        Surface(
            color = themeColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (mode == 3) Icons.Default.Shield else Icons.Default.Lock,
                    null, tint = themeColor, modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFinished) "GOAL REACHED" else "${internalGetModeName(mode)} LOCKDOWN",
                    color = themeColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
            }
        }

        val mins = timeLeft / 60
        val secs = timeLeft % 60
        Text(
            text = "%02d:%02d".format(mins, secs),
            fontSize = 100.sp,
            fontWeight = FontWeight.Thin,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(60.dp))

        if (isFinished) {
            Button(
                onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("COLLECT COINS", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = if (mode == 1) "Focusing (Safe Mode)" else "Unbreakable Lockdown Active",
                color = Color.DarkGray,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Maine function ka naam badal kar 'internalGetModeName' kar diya hai
 * taaki agar kisi aur file mein 'getModeName' ho toh conflict na ho.
 */
private fun internalGetModeName(mode: Int): String {
    return when (mode) {
        1 -> "BASIC"
        2 -> "ADVANCED"
        3 -> "NUCLEAR"
        else -> "UNKNOWN"
    }
}