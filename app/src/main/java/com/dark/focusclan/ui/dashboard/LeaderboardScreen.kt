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
import androidx.compose.ui.unit.Dp
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

    // REAL-TIME DATA FETCHING (Ranked by COINS)
    LaunchedEffect(selectedTab) {
        isLoading = true
        db.collection("users").document(currentUid).get().addOnSuccessListener { myDoc ->
            currentUserData = myDoc.toObject(UserProfile::class.java)
            val career = currentUserData?.career ?: "General"

            // Logic: Order by "coins" instead of hours for competitive wealth ranking
            val query = if (selectedTab == 0) {
                db.collection("users").whereEqualTo("career", career)
                    .orderBy("coins", Query.Direction.DESCENDING).limit(50)
            } else {
                db.collection("users")
                    .orderBy("coins", Query.Direction.DESCENDING).limit(50)
            }

            query.addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val fetchedUsers = snap.toObjects(UserProfile::class.java)
                    usersList = fetchedUsers
                    // Find My Rank based on Coins
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
                Text(
                    text = "Clan Rankings",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Top earners in the war zone",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Modern Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(26.dp))
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
                    // 2. Podium (Top 3 Coin Holders)
                    if (usersList.size >= 3) {
                        item {
                            PodiumSection(usersList.take(3))
                        }
                    }

                    // 3. Other Ranks (List View)
                    itemsIndexed(usersList.drop(if (usersList.size >= 3) 3 else 0)) { index, user ->
                        val actualRank = if (usersList.size >= 3) index + 4 else index + 1
                        RankRow(rank = actualRank, user = user, isMe = user.uid == currentUid)
                    }

                    item { Spacer(modifier = Modifier.height(110.dp)) }
                }
            }
        }

        // 4. Floating Sticky Rank Card (Real-time update)
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentUserData != null) {
                MyFixedRankCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 95.dp), // Adjust for Bottom Nav
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
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .background(if (isSelected) Color(0xFF00E676) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun PodiumSection(topThree: List<UserProfile>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 10.dp)
            .height(220.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Silver)
        PodiumItem(user = topThree[1], rank = 2, height = 130.dp, color = Color(0xFFC0C0C0))
        // 1st Place (Gold)
        PodiumItem(user = topThree[0], rank = 1, height = 170.dp, color = Color(0xFFFFD700))
        // 3rd Place (Bronze)
        PodiumItem(user = topThree[2], rank = 3, height = 110.dp, color = Color(0xFFCD7F32))
    }
}

@Composable
fun PodiumItem(user: UserProfile, rank: Int, height: Dp, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomCenter) {
            // Avatar with Glowing border
            Box(
                modifier = Modifier
                    .size(if (rank == 1) 75.dp else 60.dp)
                    .clip(CircleShape)
                    .border(2.dp, color, CircleShape)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (rank == 1) 28.sp else 22.sp
                )
            }
            // Crown for Rank 1
            if (rank == 1) {
                Icon(
                    Icons.Default.EmojiEvents,
                    null,
                    tint = color,
                    modifier = Modifier.size(24.dp).offset(y = (-55).dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("${user.coins} 🪙", color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)

        Spacer(modifier = Modifier.height(8.dp))

        // Pillar
        Box(
            modifier = Modifier
                .width(65.dp)
                .height(height)
                .background(
                    Brush.verticalGradient(listOf(color.copy(alpha = 0.4f), Color.Transparent)),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
        )
    }
}

@Composable
fun RankRow(rank: Int, user: UserProfile, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(if (isMe) Color(0xFF00E676).copy(alpha = 0.08f) else Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
            .border(if (isMe) 1.dp else 0.dp, Color(0xFF00E676).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            color = if(rank <= 3) Color(0xFF00E676) else Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(35.dp),
            fontSize = 16.sp
        )

        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
            Text(user.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${user.career} • ${user.totalHours.toInt()}h", color = Color.Gray, fontSize = 11.sp)
        }

        // COIN HIGHLIGHT
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${user.coins}",
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Text("COINS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MyFixedRankCard(modifier: Modifier, rank: Int, user: UserProfile) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = Color(0xFF00E676),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 12.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CURRENT RANK", color = Color.Black.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text("#$rank", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Warrior Portfolio", color = Color.Black.copy(0.6f), fontSize = 12.sp)
            }

            // Real-time Coins display
            Surface(color = Color.Black.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                Text(
                    text = "${user.coins} 🪙",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}