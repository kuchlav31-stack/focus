package com.dark.focusclan.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dark.focusclan.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun LeaderboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUid = auth.currentUser?.uid ?: ""

    // State Variables
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Clan, 1: Global
    var usersList by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var currentUserData by remember { mutableStateOf<UserProfile?>(null) }
    var myRank by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Real-time Data Fetching
    LaunchedEffect(selectedTab) {
        isLoading = true
        // Get current user details first to know their career
        db.collection("users").document(currentUid).get().addOnSuccessListener { myDoc ->
            currentUserData = myDoc.toObject(UserProfile::class.java)
            val career = currentUserData?.career ?: "General"

            // Build Query based on Tab
            val query = if (selectedTab == 0) {
                db.collection("users").whereEqualTo("career", career)
                    .orderBy("totalHours", Query.Direction.DESCENDING).limit(50)
            } else {
                db.collection("users").orderBy("totalHours", Query.Direction.DESCENDING).limit(50)
            }

            query.addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val fetchedUsers = snap.toObjects(UserProfile::class.java)
                    usersList = fetchedUsers
                    // Find My Rank
                    val index = fetchedUsers.indexOfFirst { it.uid == currentUid }
                    myRank = if (index != -1) index + 1 else 0
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 1. Header & Tabs
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Leaderboard", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Tab Switcher
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                    LeaderboardTab(
                        label = "My Clan",
                        isSelected = selectedTab == 0,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 0 }
                    LeaderboardTab(
                        label = "Global",
                        isSelected = selectedTab == 1,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 1 }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E676))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // 2. Podium (Top 3 Warriors)
                    if (usersList.size >= 3) {
                        item {
                            PodiumSection(usersList.take(3))
                        }
                    }

                    // 3. Other Ranks (4th and beyond)
                    itemsIndexed(usersList.drop(if (usersList.size >= 3) 3 else 0)) { index, user ->
                        val actualRank = if (usersList.size >= 3) index + 4 else index + 1
                        RankRow(rank = actualRank, user = user, isMe = user.uid == currentUid)
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }

        // 4. Current User Floating Rank Card
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentUserData != null) {
                MyFixedRankCard(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp),
                    rank = myRank,
                    user = currentUserData!!
                )
            }
        }
    }
}

@Composable
fun LeaderboardTab(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) Color(0xFF00E676) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isSelected) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun PodiumSection(topThree: List<UserProfile>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp).height(200.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place
        PodiumItem(user = topThree[1], rank = 2, height = 140.dp, color = Color(0xFFC0C0C0))
        // 1st Place
        PodiumItem(user = topThree[0], rank = 1, height = 180.dp, color = Color(0xFFFFD700))
        // 3rd Place
        PodiumItem(user = topThree[2], rank = 3, height = 120.dp, color = Color(0xFFCD7F32))
    }
}

@Composable
fun PodiumItem(user: UserProfile, rank: Int, height: androidx.compose.ui.unit.Dp, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomCenter) {
            // Avatar
            Box(modifier = Modifier.size(60.dp).clip(CircleShape).border(2.dp, color, CircleShape).background(Color(0xFF1E1E1E)), contentAlignment = Alignment.Center) {
                Text(user.username.take(1).uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            // Rank Badge
            Surface(modifier = Modifier.offset(y = 10.dp), color = color, shape = CircleShape) {
                Text("#$rank", modifier = Modifier.padding(horizontal = 8.dp), color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("${user.totalHours.toInt()}h", color = color, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(8.dp))
        // Podium Pillar
        Box(modifier = Modifier.width(60.dp).height(height).background(
            Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)),
            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ))
    }
}

@Composable
fun RankRow(rank: Int, user: UserProfile, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .background(if (isMe) Color(0xFF00E676).copy(alpha = 0.05f) else Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
            .border(if (isMe) 1.dp else 0.dp, Color(0xFF00E676).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = rank.toString(), color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))

        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
            Text(user.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(user.career, color = Color.Gray, fontSize = 11.sp)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("${user.totalHours.toInt()}h", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("${user.coins} 🪙", color = Color(0xFFFFD700), fontSize = 11.sp)
        }
    }
}

@Composable
fun MyFixedRankCard(modifier: Modifier, rank: Int, user: UserProfile) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = Color(0xFF00E676),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 8.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR RANK", color = Color.Black.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("#$rank", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(user.username, color = Color.Black, fontWeight = FontWeight.Bold)
                Text("${user.totalHours.toInt()} Hours Focused", color = Color.Black.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}