package com.dark.focusclan.ui.dashboard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dark.focusclan.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var userData by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            isUploading = true
            val ref = storage.reference.child("profile_pics/$uid.jpg")
            ref.putFile(it).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    db.collection("users").document(uid).update("profilePicUrl", downloadUrl.toString())
                    isUploading = false
                    Toast.makeText(context, "Identity Updated!", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { isUploading = false }
        }
    }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).addSnapshotListener { snap, _ ->
            userData = snap?.toObject(UserProfile::class.java)
            isLoading = false
        }
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = Color(0xFF0A0A0A) // Deeper Black
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
                        IconButton(onClick = { /* Settings Logic */ }) {
                            Icon(Icons.Default.Settings, "Settings", tint = Color.Gray)
                        }
                        IconButton(onClick = {
                            auth.signOut()
                            navController.navigate("login") { popUpTo(0) }
                        }) {
                            Icon(Icons.Default.PowerSettingsNew, "Logout", tint = Color.Red.copy(0.7f))
                        }
                    }
                }

                // 2. Profile Image & Identity
                item {
                    Box(contentAlignment = Alignment.Center) {
                        // Glowing Background
                        Box(modifier = Modifier.size(130.dp).clip(CircleShape).background(
                            Brush.radialGradient(listOf(Color(0xFF00E676).copy(0.15f), Color.Transparent))
                        ))

                        // User Image
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF00E676), CircleShape)
                                .background(Color(0xFF1E1E1E))
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (userData?.profilePicUrl.isNullOrEmpty()) {
                                Text(
                                    text = (userData?.fullName?.take(1) ?: "W").uppercase(),
                                    fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E676)
                                )
                            } else {
                                AsyncImage(
                                    model = userData?.profilePicUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (isUploading) {
                                CircularProgressIndicator(color = Color(0xFF00E676), modifier = Modifier.size(30.dp))
                            }
                        }

                        // Edit Icon Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-5).dp, y = (-5).dp)
                                .size(32.dp)
                                .background(Color(0xFF00E676), CircleShape)
                                .border(3.dp, Color(0xFF0A0A0A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = userData?.fullName ?: "Warrior", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(text = "@${userData?.username ?: "clan_warrior"}", fontSize = 14.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Medium)

                    Surface(
                        modifier = Modifier.padding(top = 12.dp),
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.05f))
                    ) {
                        Text(
                            text = userData?.career ?: "General Clan",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Stats Grid (Modern Glass Cards)
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileStatCard(Modifier.weight(1f), "Hours", String.format("%.1f", userData?.totalHours ?: 0.0), Icons.Default.TrendingUp)
                        ProfileStatCard(Modifier.weight(1f), "Wealth", (userData?.coins ?: 0).toString(), Icons.Default.AccountBalanceWallet)
                        ProfileStatCard(Modifier.weight(1f), "Streak", "${userData?.streak ?: 0}d", Icons.Default.LocalFireDepartment)
                    }
                }

                // 4. Motto Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
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
                                text = if (userData?.motto.isNullOrEmpty()) "Every second counts in the battlefield of life." else userData?.motto!!,
                                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }

                // 5. Achievement/Performance
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Achievements", modifier = Modifier.fillMaxWidth().padding(start = 4.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    PerformanceCard(userData?.career ?: "Clan")
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
fun ProfileStatCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
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
            Icon(icon, null, tint = if (label == "Streak") Color(0xFFFF5722) else Color(0xFF00E676), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = label.uppercase(), fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun PerformanceCard(career: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).background(Color(0xFF00E676).copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Stars, null, tint = Color(0xFF00E676))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Elite Commander", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Top 5% in $career Field", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}