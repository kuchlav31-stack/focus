package com.dark.focusclan.ui.dashboard

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.dark.focusclan.services.FocusService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
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

    // --- Timer & Logic States ---
    var timeLeft by remember { mutableLongStateOf(duration * 60L) }
    var isFinished by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(true) }
    val totalSeconds = duration * 60f

    // --- UI Animation States ---
    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) timeLeft / totalSeconds else 0f,
        animationSpec = tween(1000, easing = LinearEasing), label = ""
    )

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = ""
    )

    val themeColor = when (mode) {
        1 -> Color(0xFF00E676)
        2 -> Color(0xFFFFA500)
        else -> Color(0xFFF44336)
    }

    // --- Back Button Lockdown (Strict) ---
    BackHandler(enabled = !isFinished && mode > 1) { }

    // --- Core Timer & Cloud Sync Logic ---
    LaunchedEffect(Unit) {
        val userRef = db.collection("users").document(uid)

        // 1. CLOUD SYNC: Check if a session is already running on server
        userRef.get().addOnSuccessListener { doc ->
            val cloudEndTime = doc.getLong("focusEndTime") ?: 0L
            val now = System.currentTimeMillis()

            if (cloudEndTime > now) {
                // Resume existing session
                timeLeft = (cloudEndTime - now) / 1000
            } else {
                // Start NEW session and push to cloud
                val newEndTime = now + (duration * 60 * 1000)
                userRef.update("focusEndTime", newEndTime, "isFocusing", true)
                timeLeft = (duration * 60).toLong()

                // Local save for Restart Protection (Nuclear)
                if (mode == 3) {
                    prefs.edit().apply {
                        putBoolean("isChallengeActive", true)
                        putLong("endTime", newEndTime)
                        putInt("activeMode", mode)
                        putString("activeChallengeId", challengeId)
                        apply()
                    }
                }
            }
            isSyncing = false
        }

        // 2. DND & Pull-back Service
        if (mode >= 2) {
            toggleDND(context, true)
            context.startForegroundService(Intent(context, FocusService::class.java))
        }

        // 3. Main Countdown Loop
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--

            // Sync Multiplayer progress if needed
            if (challengeId != "solo" && timeLeft % 10 == 0L) { // Every 10 sec
                db.collection("challenges").document(challengeId).update("results.$uid", "focusing")
            }
        }

        // --- SUCCESS SEQUENCE ---
        isFinished = true
        toggleDND(context, false)

        val earnedCoins = calculateFinalCoins(duration)
        val updates = hashMapOf<String, Any>(
            "isFocusing" to false,
            "focusEndTime" to 0L, // Reset cloud timer
            "coins" to FieldValue.increment(earnedCoins),
            "totalHours" to FieldValue.increment(duration / 60.0),
            "streak" to FieldValue.increment(1)
        )
        userRef.update(updates)

        if (mode == 3) prefs.edit().putBoolean("isChallengeActive", false).apply()
        context.stopService(Intent(context, FocusService::class.java))
    }

    // --- UI Layout ---
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF050505)),
        contentAlignment = Alignment.Center
    ) {
        if (isSyncing) {
            CircularProgressIndicator(color = Color(0xFF00E676))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // 1. Status Badge
                AnimatedContent(targetState = isFinished, label = "") { finished ->
                    if (finished) {
                        BadgeUI("MISSION ACCOMPLISHED", Color(0xFF00E676), Icons.Default.Shield)
                    } else {
                        BadgeUI("${internalGetModeName(mode)} LOCKDOWN", themeColor, if(mode==3) Icons.Default.Shield else Icons.Default.Lock)
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))

                // 2. Visual Timer Ring
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(280.dp)) {
                        drawCircle(color = Color.White.copy(0.05f), style = Stroke(width = 4.dp.toPx()))
                    }
                    Canvas(modifier = Modifier.size(280.dp)) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(themeColor.copy(0.2f), themeColor)),
                            startAngle = -90f,
                            sweepAngle = 360 * progress,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%02d:%02d".format(timeLeft / 60, timeLeft % 60),
                            fontSize = 85.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = Color.White,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            text = if (isFinished) "WARRIOR" else "STAY FOCUSED",
                            color = themeColor.copy(pulseAlpha),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))

                // 3. Success / Motivation
                if (isFinished) {
                    VictorySection(duration) {
                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                    }
                } else {
                    Text(
                        "Device is under Nuclear Control.",
                        color = Color.DarkGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "No Exit Allowed.",
                        color = themeColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeUI(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = color.copy(0.1f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(0.3f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = color, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun VictorySection(duration: Int, onCollect: () -> Unit) {
    val coins = calculateFinalCoins(duration)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Reward: $coins War Coins", color = Color(0xFFFFD700), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCollect,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
            modifier = Modifier.fillMaxWidth(0.7f).height(58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("CLAIM & RETURN", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

// --- UTILS ---

private fun calculateFinalCoins(duration: Int): Long {
    return when {
        duration >= 240 -> 10000L
        duration >= 180 -> 5000L
        duration >= 120 -> 1000L
        duration >= 60 -> 500L
        duration >= 30 -> 100L
        else -> 10L
    }
}

private fun toggleDND(context: Context, enable: Boolean) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    try {
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(if (enable) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    } catch (e: Exception) {}
}

private fun internalGetModeName(mode: Int) = when (mode) {
    1 -> "BASIC" 2 -> "ADVANCED" 3 -> "NUCLEAR" else -> "SAFE"
}