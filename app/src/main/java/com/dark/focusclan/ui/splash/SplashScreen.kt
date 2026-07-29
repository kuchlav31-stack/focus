package com.dark.focusclan.ui.splash

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dark.focusclan.utils.PermissionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)

    // Animation States
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animation Start
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200)
            )
        }

        delay(3000) // Branding Time + Background Checks

        // 1. RESTART LOOPHOLE CHECK (Sabse Pehle)
        val isLocked = prefs.getBoolean("isChallengeActive", false)
        val endTime = prefs.getLong("endTime", 0L)

        if (isLocked && System.currentTimeMillis() < endTime) {
            val remainingMins = ((endTime - System.currentTimeMillis()) / 60000).toInt()
            val challengeId = prefs.getString("activeChallengeId", "solo") ?: "solo"
            navController.navigate("focus_mode/$challengeId/${remainingMins + 1}") {
                popUpTo("splash") { inclusive = true }
            }
            return@LaunchedEffect
        }

        // 2. AUTH & PROFILE CHECK
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Check if Profile (Career/Username) exists in Firestore
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.contains("career")) {
                        // User Profile is complete, now check Permissions
                        if (PermissionUtils.allPermissionsGranted(context)) {
                            navController.navigate("home") { popUpTo("splash") { inclusive = true } }
                        } else {
                            navController.navigate("permission_setup") { popUpTo("splash") { inclusive = true } }
                        }
                    } else {
                        // Logged in but profile not setup (Incomplete Signup)
                        navController.navigate("profile_setup") { popUpTo("splash") { inclusive = true } }
                    }
                }
                .addOnFailureListener {
                    // Internet issue or error, stay on Login
                    navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                }
        } else {
            // No User Logged in
            navController.navigate("login") { popUpTo("splash") { inclusive = true } }
        }
    }

    // UI: Premium Dark Design with Gradient Logo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F0F), Color(0xFF121212))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Visual Logo (Text for now, can be Image)
            Text(
                text = "FocusClan",
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF00E676), // Neon Green
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "BUILD YOUR LEGACY",
                fontSize = 14.sp,
                color = Color.Gray,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Light
            )
        }

        // Bottom Loading/Version Indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(alpha.value)
        ) {
            Text(
                text = "Syncing with Clan...",
                color = Color(0xFF00E676).copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}