package com.dark.focusclan.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dark.focusclan.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(navController: NavController, targetUserId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val myUid = auth.currentUser?.uid ?: ""

    var user by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch Target User Profile
    LaunchedEffect(targetUserId) {
        db.collection("users").document(targetUserId).get()
            .addOnSuccessListener { doc ->
                user = doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load warrior profile", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Warrior Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212), titleContentColor = Color.White)
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00E676))
            }
        } else {
            user?.let { profile ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Profile Header
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(
                            Brush.radialGradient(listOf(Color(0xFF00E676).copy(0.2f), Color.Transparent))
                        ))
                        Box(
                            modifier = Modifier.size(90.dp).clip(CircleShape).background(Color(0xFF1E1E1E))
                                .border(2.dp, Color(0xFF00E676), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(profile.username.take(1).uppercase(), fontSize = 40.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "@${profile.username}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(text = profile.career, color = Color(0xFF00E676), fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Motto/Bio
                    Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = if(profile.motto.isEmpty()) "Focused to build a legacy." else profile.motto,
                            color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(16.dp), fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 2. Real Stats Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OtherStatCard(Modifier.weight(1f), "Total Hours", String.format("%.1f", profile.totalHours), Icons.Default.Timer)
                        OtherStatCard(Modifier.weight(1f), "Focus Coins", profile.coins.toString(), Icons.Default.MonetizationOn)
                        OtherStatCard(Modifier.weight(1f), "Current Streak", "${profile.streak}d", Icons.Default.LocalFireDepartment)
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 3. Actions Area
                    Button(
                        onClick = {
                            // CRITICAL FIX: UID Empty check before navigation
                            if (profile.uid.isNotEmpty()) {
                                navController.navigate("chat/${profile.uid}/${profile.username}")
                            } else {
                                Toast.makeText(context, "User ID not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Chat, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("MESSAGE WARRIOR", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                // REMOVE FRIEND LOGIC
                                db.collection("users").document(myUid).update("friendsList", FieldValue.arrayRemove(targetUserId))
                                db.collection("users").document(targetUserId).update("friendsList", FieldValue.arrayRemove(myUid))
                                Toast.makeText(context, "Warrior removed from clan", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("REMOVE", color = Color.Gray)
                        }

                        OutlinedButton(
                            onClick = { /* BLOCK LOGIC */ },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCF6679))
                        ) {
                            Text("BLOCK", color = Color(0xFFCF6679))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtherStatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.DarkGray)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}