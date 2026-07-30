package com.dark.focusclan.ui.splash

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
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

    // Animation States for a premium look
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Start Branding Animations
        launch { scale.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
        launch { alpha.animateTo(1f, tween(1200)) }

        delay(2500) // branding display time

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            // --- FIREBASE SECURITY & FLOW CHECK ---
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val cloudEndTime = document.getLong("focusEndTime") ?: 0L
                        val currentTime = System.currentTimeMillis()

                        // A. ANTI-CHEAT CHECK: Agar cloud pe timer active hai
                        if (cloudEndTime > currentTime) {
                            val remainingMins = ((cloudEndTime - currentTime) / 60000).toInt()
                            val savedMode = document.getLong("activeMode")?.toInt() ?: 3
                            val challengeId = document.getString("activeChallengeId") ?: "solo"

                            navController.navigate("focus_mode/$challengeId/${remainingMins + 1}/$savedMode") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        // B. PROFILE COMPLETION CHECK
                        else if (!document.contains("career") || document.getString("career").isNullOrEmpty()) {
                            navController.navigate("profile_setup") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        // C. PERMISSION CHECK (Aapki request ke mutabik)
                        else if (!PermissionUtils.allPermissionsGranted(context)) {
                            // Agar profile complete hai par user ne permissions off kar di hain
                            navController.navigate("permission_setup") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        // D. ALL CLEAR -> GO HOME
                        else {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    } else {
                        // Document nahi mila (Signup incomplete)
                        navController.navigate("profile_setup") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
                .addOnFailureListener {
                    // Internet issues? Stay on Login
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
        } else {
            // User not logged in
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // --- UI DESIGN ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A0A), Color(0xFF121212))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value).alpha(alpha.value)
        ) {
            Text(
                text = "FocusClan",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E676),
                letterSpacing = (-2).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SYNCHRONIZING CLAN...",
                fontSize = 11.sp,
                color = Color.Gray,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom Loading Indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .alpha(alpha.value)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color(0xFF00E676).copy(alpha = 0.4f),
                strokeWidth = 2.dp
            )
        }
    }
}