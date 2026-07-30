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
    duration: Int, // User ne jitne minute ka timer lagaya hai
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

    // UI Theme colors
    val themeColor = when (mode) {
        1 -> Color(0xFF00E676) // Basic (Neon Green)
        2 -> Color(0xFFFFA500) // Advanced (Orange)
        else -> Color.Red      // Nuclear (Deep Red)
    }

    // 1. BACK BUTTON LOCKDOWN (For Advanced & Nuclear)
    BackHandler(enabled = !isFinished && mode > 1) {
        // Unbreakable: Back button disabled
    }

    // --- CORE LOGIC BLOCK ---
    LaunchedEffect(Unit) {
        // A. Online Focus Status (For friends to see green dot)
        db.collection("users").document(uid).update("isFocusing", true)

        // B. Nuclear Restart Protection Setup
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

        // C. Start Blocking Service (Pull-back logic)
        if (mode >= 2) {
            val intent = Intent(context, FocusService::class.java)
            context.startForegroundService(intent)
        }

        // D. Timer Loop (Runs every 1 second)
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }

        // --- SESSION SUCCESS LOGIC ---
        isFinished = true

        // 1. CALCULATE TIERED REWARDS (Aapka Coin Logic)
        val earnedCoins: Long = when {
            duration >= 240 -> 10000L // 4 Hours+
            duration >= 180 -> 5000L  // 3 Hours
            duration >= 120 -> 1000L  // 2 Hours
            duration >= 60  -> 500L   // 1 Hour
            duration >= 30  -> 100L   // 30 Mins
            duration >= 1   -> 10L    // 1-30 Mins ke beech
            else -> 0L
        }

        // 2. Update Firestore Stats (Atomic Increment)
        val updates = hashMapOf<String, Any>(
            "isFocusing" to false,
            "coins" to FieldValue.increment(earnedCoins), // Har session ke rewards add honge
            "totalHours" to FieldValue.increment(duration / 60.0), // Precision hours
            "streak" to FieldValue.increment(1) // Daily streak increase
        )
        db.collection("users").document(uid).update(updates)

        // 3. CLEANUP: Disable Restart Protection & Stop Blocking Service
        if (mode == 3) {
            prefs.edit().putBoolean("isChallengeActive", false).apply()
        }
        if (mode >= 2) {
            context.stopService(Intent(context, FocusService::class.java))
        }
    }

    // --- UI LAYOUT ---
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF000000)), // Pitch Black for focus
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mode Badge
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
                    imageVector = if (mode == 3) Icons.Default.Shield else Icons.Default.Lock,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFinished) "SESSION SUCCESS" else "${internalGetModeName(mode)} LOCKDOWN",
                    color = themeColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
            }
        }

        // Massive Timer Display
        val mins = timeLeft / 60
        val secs = timeLeft % 60
        Text(
            text = "%02d:%02d".format(mins, secs),
            fontSize = 100.sp,
            fontWeight = FontWeight.Thin,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(60.dp))

        // Success State
        if (isFinished) {
            val coinsWon: Long = when {
                duration >= 240 -> 10000L
                duration >= 180 -> 5000L
                duration >= 120 -> 1000L
                duration >= 60 -> 500L
                duration >= 30 -> 100L
                else -> 10L
            }

            Text(text = "You earned $coinsWon War Coins!", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(32.dp))

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
                Text("COLLECT REWARDS", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            // Active Lock State message
            Text(
                text = if (mode == 1) "Focusing in Safe Mode" else "Unbreakable Lockdown Active",
                color = Color.DarkGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Helper function with internal name to avoid naming conflicts
 */
private fun internalGetModeName(mode: Int): String {
    return when (mode) {
        1 -> "BASIC"
        2 -> "ADVANCED"
        3 -> "NUCLEAR"
        else -> "UNKNOWN"
    }
}