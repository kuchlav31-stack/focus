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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
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

    // Fetch Target User Data
    LaunchedEffect(targetUserId) {
        db.collection("users").document(targetUserId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    user = doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Warrior data unavailable", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Warrior Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F0F), titleContentColor = Color.White)
            )
        },
        containerColor = Color(0xFF0A0A0A) // Deeper Dark for Contrast
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
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. Profile Picture with Aura
                    Box(contentAlignment = Alignment.Center) {
                        // Ambient Glow
                        Box(modifier = Modifier.size(140.dp).clip(CircleShape).background(
                            Brush.radialGradient(listOf(Color(0xFF00E676).copy(alpha = 0.15f), Color.Transparent))
                        ))

                        // Picture Frame
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF00E676), CircleShape)
                                .background(Color(0xFF1E1E1E)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.profilePicUrl.isEmpty()) {
                                Text(
                                    text = profile.username.take(1).uppercase(),
                                    fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E676)
                                )
                            } else {
                                AsyncImage(
                                    model = profile.profilePicUrl,
                                    contentDescription = "Warrior Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = profile.fullName, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(text = "@${profile.username}", fontSize = 15.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Clan Tag
                    Surface(color = Color(0xFF1A1A1A), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(0.1f))) {
                        Text(
                            text = "CLAN: ${profile.career.uppercase()}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // 2. Performance Stats Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailedStatCard(Modifier.weight(1f), "Total Hours", String.format("%.1f", profile.totalHours), Icons.Default.Timer)
                        DetailedStatCard(Modifier.weight(1f), "Clan Wealth", profile.coins.toString(), Icons.Default.MonetizationOn)
                        DetailedStatCard(Modifier.weight(1f), "Current Streak", "${profile.streak}d", Icons.Default.LocalFireDepartment)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Warrior Motto Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.05f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("WARRIOR MOTTO", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (profile.motto.isEmpty()) "In the battlefield of goals, silence is my weapon." else profile.motto,
                                color = Color.White, fontSize = 15.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 4. Primary Actions
                    Button(
                        onClick = {
                            if (profile.uid.isNotEmpty()) {
                                navController.navigate("chat/${profile.uid}/${profile.username}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(8.dp)
                    ) {
                        Icon(Icons.Default.Chat, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("MESSAGE WARRIOR", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Destructive Actions
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                db.collection("users").document(myUid).update("friendsList", FieldValue.arrayRemove(targetUserId))
                                db.collection("users").document(targetUserId).update("friendsList", FieldValue.arrayRemove(myUid))
                                Toast.makeText(context, "Removed from Clan", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.DarkGray)
                        ) {
                            Text("REMOVE FRIEND", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { /* Implement Block Logic */ },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFCF6679).copy(alpha = 0.5f))
                        ) {
                            Text("BLOCK", color = Color(0xFFCF6679), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}

@Composable
fun DetailedStatCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Surface(
        modifier = modifier,
        color = Color(0xFF151515),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.03f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = if (label == "Current Streak") Color(0xFFFF5722) else Color(0xFF00E676), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = label.uppercase(), fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}