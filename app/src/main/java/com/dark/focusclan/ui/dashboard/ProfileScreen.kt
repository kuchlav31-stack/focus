package com.dark.focusclan.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dark.focusclan.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    // Real-time State
    var userData by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).addSnapshotListener { snap, _ ->
            userData = snap?.toObject(UserProfile::class.java)
            isLoading = false
        }
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = Color(0xFF121212)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00E676))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Action Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { /* Edit Logic */ }) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color.Gray)
                        }
                        IconButton(onClick = {
                            auth.signOut()
                            navController.navigate("login") { popUpTo(0) }
                        }) {
                            Icon(Icons.Default.Logout, "Logout", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }

                // 2. Profile Header
                item {
                    Box(contentAlignment = Alignment.Center) {
                        // Avatar Glow
                        Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(
                            Brush.radialGradient(listOf(Color(0xFF00E676).copy(alpha = 0.2f), Color.Transparent))
                        ))
                        // Real Avatar
                        Box(
                            modifier = Modifier.size(90.dp).clip(CircleShape).background(Color(0xFF1E1E1E))
                                .border(2.dp, Color(0xFF00E676), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (userData?.fullName?.take(1) ?: "W").uppercase(),
                                fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = userData?.fullName ?: "Warrior", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "@${userData?.username ?: "username"}", fontSize = 14.sp, color = Color(0xFF00E676))

                    Surface(
                        modifier = Modifier.padding(top = 12.dp),
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = userData?.career ?: "General",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Stats Grid
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Modifier.weight(1f), "Focus Hours", String.format("%.1f", userData?.totalHours ?: 0.0), Icons.Default.Timer)
                        StatCard(Modifier.weight(1f), "Coins", (userData?.coins ?: 0).toString(), Icons.Default.MonetizationOn)
                        StatCard(Modifier.weight(1f), "Streak", "${userData?.streak ?: 0}d", Icons.Default.LocalFireDepartment)
                    }
                }

                // 4. Motto Section
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Text("Warrior Motto", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (userData?.motto.isNullOrEmpty()) "Build your legacy, one focus at a time." else userData?.motto!!,
                            color = Color.White, fontSize = 15.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                // 5. Clan Performance Card
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Clan Standing", modifier = Modifier.fillMaxWidth(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF00E676).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFF00E676))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Elite Warrior", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("You focused more than 85% of ${userData?.career}", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.DarkGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = if (label == "Streak") Color(0xFFFF5722) else Color(0xFF00E676), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}