package com.dark.focusclan.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dark.focusclan.models.BattleModel
import com.dark.focusclan.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    // --- State Management ---
    var userData by remember { mutableStateOf<UserProfile?>(null) }
    var liveWarriorsCount by remember { mutableIntStateOf(0) }
    var incomingRequestsCount by remember { mutableIntStateOf(0) }
    val clanFriends = remember { mutableStateListOf<UserProfile>() }
    val pendingBattles = remember { mutableStateListOf<BattleModel>() }

    // Timer States
    var timerValue by remember { mutableIntStateOf(25) }
    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var customTimeInput by remember { mutableStateOf("") }

    // --- Firebase Real-time Listeners ---
    LaunchedEffect(uid) {
        if (uid.isEmpty()) return@LaunchedEffect

        // 1. Current User & Friends List
        db.collection("users").document(uid).addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                userData = snap.toObject(UserProfile::class.java)?.copy(uid = snap.id)
                val friendsIds = snap.get("friendsList") as? List<String> ?: emptyList()

                if (friendsIds.isNotEmpty()) {
                    db.collection("users").whereIn(FieldPath.documentId(), friendsIds)
                        .addSnapshotListener { fSnap, _ ->
                            if (fSnap != null) {
                                val fetchedFriends = fSnap.documents.mapNotNull { doc ->
                                    doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                                }
                                clanFriends.clear()
                                clanFriends.addAll(fetchedFriends)
                            }
                        }
                } else clanFriends.clear()
            }
        }

        // 2. Global Focusing Count
        db.collection("users").whereEqualTo("isFocusing", true).addSnapshotListener { snap, _ ->
            liveWarriorsCount = snap?.size() ?: 0
        }

        // 3. Friend Requests Badge
        db.collection("friend_requests").whereEqualTo("to", uid).whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                incomingRequestsCount = snap?.size() ?: 0
            }

        // 4. Battle Invitations (Challenges)
        db.collection("battles")
            .whereIn("status", listOf("invited", "active"))
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val list = snap.toObjects(BattleModel::class.java)
                    val myBattles = list.filter { it.challengerId == uid || it.receiverId == uid }
                    pendingBattles.clear()
                    pendingBattles.addAll(myBattles)

                    // AUTO-START: If a battle I'm in becomes active
                    myBattles.find { it.status == "active" }?.let { active ->
                        navController.navigate("focus_mode/${active.battleId}/${active.duration}/3")
                    }
                }
            }
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // --- Header ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.clickable { navController.navigate("profile") }) {
                    Text("Welcome Warrior,", color = Color.Gray, fontSize = 12.sp)
                    Text(userData?.username ?: "Warrior", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFFFD700).copy(0.2f))) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${userData?.coins ?: 0}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp).padding(start = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Persistent Battle Invites ---
            if (pendingBattles.isNotEmpty()) {
                pendingBattles.forEach { battle ->
                    BattleStatusCard(battle, uid,
                        onAccept = { db.collection("battles").document(battle.battleId).update("status", "active") },
                        onDecline = { db.collection("battles").document(battle.battleId).delete() }
                    )
                }
            }

            // --- Friend Request Alert ---
            if (incomingRequestsCount > 0) {
                Card(
                    onClick = { navController.navigate("friend_requests") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00E676))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GroupAdd, null, tint = Color.Black)
                        Text("  $incomingRequestsCount clan invitations waiting", color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Black)
                    }
                }
            }

            // --- Global Stats ---
            Surface(color = Color(0xFF1E1E1E).copy(0.5f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                    Text("  $liveWarriorsCount warriors focusing now", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Timer Circle ---
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clickable { showCustomTimeDialog = true }
                        .border(4.dp, Brush.sweepGradient(listOf(Color(0xFF00E676), Color(0xFF1E1E1E))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$timerValue", fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("MINUTES", fontSize = 14.sp, color = Color.Gray, letterSpacing = 2.sp)
                        Text("Tap to edit", fontSize = 10.sp, color = Color(0xFF00E676).copy(0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Selection Chips ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                listOf(25, 50, 90).forEach { time ->
                    Surface(
                        modifier = Modifier.padding(horizontal = 6.dp).clickable { timerValue = time },
                        color = if (timerValue == time) Color(0xFF00E676) else Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("${time}m", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (timerValue == time) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Action Button ---
            Button(
                onClick = { navController.navigate("mode_selection/$timerValue") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ENTER WAR ZONE", color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- Clan Friends social Row ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Clan Friends", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = { navController.navigate("discover") }) { Text("Find More", color = Color(0xFF00E676)) }
            }

            if (clanFriends.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text("No friends yet. Add warriors!", color = Color.DarkGray, fontSize = 13.sp)
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(clanFriends) { friend ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                if(friend.uid.isNotEmpty()) navController.navigate("other_profile/${friend.uid}")
                            }
                        ) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).border(1.dp, if(friend.isFocusing) Color(0xFF00E676) else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                                Text(friend.username.take(1).uppercase(), color = Color.White)
                                if (friend.isFocusing) Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF00E676)).align(Alignment.BottomEnd).border(2.dp, Color(0xFF121212), CircleShape))
                            }
                            Text(friend.username, color = if(friend.isFocusing) Color(0xFF00E676) else Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // --- Custom Time Dialog ---
        if (showCustomTimeDialog) {
            AlertDialog(
                onDismissRequest = { showCustomTimeDialog = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text("Custom Focus Goal", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = customTimeInput,
                        onValueChange = { if (it.length <= 3) customTimeInput = it },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00E676))
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val input = customTimeInput.toIntOrNull()
                        if (input != null && input > 0) { timerValue = input; showCustomTimeDialog = false; customTimeInput = "" }
                    }) { Text("SET", color = Color(0xFF00E676)) }
                }
            )
        }
    }
}

@Composable
fun BattleStatusCard(battle: BattleModel, myUid: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    val isChallenger = battle.challengerId == myUid
    if (battle.status == "active") return // Active battles handeled by auto-nav

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(1.dp, Color(0xFF00E676).copy(0.4f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FlashOn, null, tint = Color(0xFF00E676))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(if (isChallenger) "Waiting for Warrior..." else "Focus Duel Challenge!", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${battle.duration}m Duel • Nuclear Mode", color = Color.Gray, fontSize = 11.sp)
            }
            if (!isChallenger) {
                IconButton(onClick = onAccept) { Icon(Icons.Default.CheckCircle, null, tint = Color.Green) }
            }
            IconButton(onClick = onDecline) { Icon(Icons.Default.Cancel, null, tint = Color.Red) }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = Color(0xFF1E1E1E), tonalElevation = 8.dp) {
        val items = listOf(
            Triple("Home", Icons.Default.Home, "home"),
            Triple("Clan", Icons.Default.Groups, "discover"),
            Triple("Battle", Icons.Default.FlashOn, "challenge_feed"),
            Triple("Rank", Icons.Default.Leaderboard, "leaderboard"),
            Triple("Profile", Icons.Default.Person, "profile")
        )

        items.forEach { (label, icon, route) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(icon, null) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00E676),
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent,
                    selectedTextColor = Color(0xFF00E676),
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}