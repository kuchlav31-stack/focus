package com.dark.focusclan.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dark.focusclan.models.ChallengeModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeFeedScreen(navController: NavController, viewModel: ChallengeViewModel = viewModel()) {
    val context = LocalContext.current
    var showHostSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Live UI Refresh Timer (1 second interval)
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Fetch data when screen opens
    LaunchedEffect(Unit) {
        viewModel.fetchUserCareerAndChallenges()
        // Local ticker to update "Starts in X seconds" every second
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showHostSheet = true },
                containerColor = Color(0xFF00E676),
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, "Host") },
                text = { Text("HOST BATTLE", fontWeight = FontWeight.ExtraBold) }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. CLAN STATISTICS HEADER (With Live Running Count)
            ClanStatsHeader(
                totalBattles = viewModel.totalClanBattles.intValue,
                runningBattles = viewModel.runningBattlesCount.intValue,
                mvpName = viewModel.topWarrior.value,
                career = viewModel.userCareer.value
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Live Arena",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Join 1m before or after start time.",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. CHALLENGE LIST LOGIC
            if (viewModel.isLoading.value && viewModel.activeChallenges.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E676))
                }
            } else if (viewModel.activeChallenges.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TimerOff, null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                        Text("No upcoming battles. Host one!", color = Color.DarkGray, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(viewModel.activeChallenges) { challenge ->
                        ChallengeCard(
                            challenge = challenge,
                            now = currentTime,
                            onJoin = {
                                viewModel.joinChallenge(challenge.id) { success, message ->
                                    if (success) {
                                        navController.navigate("lobby/${challenge.id}")
                                    } else {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- HOST BATTLE BOTTOM SHEET ---
        if (showHostSheet) {
            ModalBottomSheet(
                onDismissRequest = { showHostSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E)
            ) {
                HostBattleSheetContent(
                    onPost = { title, time, fee, startIn ->
                        viewModel.createChallenge(title, time, fee, startIn) {
                            showHostSheet = false
                            Toast.makeText(context, "Battle Scheduled!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ClanStatsHeader(totalBattles: Int, runningBattles: Int, mvpName: String, career: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(career.uppercase(), color = Color(0xFF00E676), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                Text("Total Clan Battles: $totalBattles", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                    Text(" MVP: @$mvpName", color = Color.Gray, fontSize = 13.sp)
                }
            }

            // Running Battles Badge
            Surface(
                color = Color.Red.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Red.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$runningBattles", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("LIVE NOW", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HostBattleSheetContent(onPost: (String, Int, Int, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableFloatStateOf(25f) }
    var startInMins by remember { mutableFloatStateOf(5f) }

    Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
        Text("Schedule Clan Battle", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Battle Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00E676))
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Duration: ${duration.toInt()} mins", color = Color.White, fontWeight = FontWeight.Bold)
        Slider(value = duration, onValueChange = { duration = it }, valueRange = 10f..180f, colors = SliderDefaults.colors(thumbColor = Color(0xFF00E676), activeTrackColor = Color(0xFF00E676)))

        Text("Starts in: ${startInMins.toInt()} mins", color = Color.White, fontWeight = FontWeight.Bold)
        Slider(value = startInMins, onValueChange = { startInMins = it }, valueRange = 2f..30f, colors = SliderDefaults.colors(thumbColor = Color(0xFF00E676), activeTrackColor = Color(0xFF00E676)))

        Button(
            onClick = { if(title.isNotBlank()) onPost(title, duration.toInt(), 0, startInMins.toInt()) },
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("POST TO CLAN FEED", color = Color.Black, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ChallengeCard(challenge: ChallengeModel, now: Long, onJoin: () -> Unit) {
    val timeDiff = challenge.startTime - now
    val windowStart = challenge.startTime - 60000 // 1 min before
    val windowEnd = challenge.startTime + 60000   // 1 min after

    val isWindowOpen = now in windowStart..windowEnd
    val isExpired = now > windowEnd
    val isWaiting = now < windowStart

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if(isWindowOpen) 2.dp else 0.dp, Color(0xFF00E676))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Host: @${challenge.hostName}", color = Color(0xFF00E676), fontSize = 12.sp)
                }

                // 5K Bounty Badge (No logic change, just UI)
                if (challenge.participants.size >= 2) {
                    Surface(color = Color(0xFFFFD700).copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("5K BOUNTY", color = Color(0xFFFFD700), modifier = Modifier.padding(6.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text("  ${challenge.duration}m Focus", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Groups, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text("  ${challenge.participants.size}/5 Joined", color = Color.Gray, fontSize = 13.sp)
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(0.05f))

            // Dynamic Status & Action
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    val statusText = when {
                        isWaiting -> "Starts in ${timeDiff / 1000}s"
                        isWindowOpen -> "WINDOW OPEN"
                        else -> "Window Closed"
                    }
                    val statusColor = when {
                        isWaiting -> Color(0xFFFFA500)
                        isWindowOpen -> Color(0xFF00E676)
                        else -> Color.Red
                    }
                    Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Nuclear Mode Only", color = Color.DarkGray, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onJoin,
                    enabled = isWindowOpen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(isWindowOpen) Color(0xFF00E676) else Color(0xFF333333)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if(isWindowOpen) "JOIN BATTLE" else "LOCKED",
                        color = if(isWindowOpen) Color.Black else Color.Gray,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                }
            }
        }
    }
}