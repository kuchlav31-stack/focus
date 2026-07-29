package com.dark.focusclan.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dark.focusclan.models.ChallengeModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeFeedScreen(navController: NavController, viewModel: ChallengeViewModel = viewModel()) {

    // Fetch data when screen opens
    LaunchedEffect(Unit) {
        viewModel.fetchUserCareerAndChallenges()
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) }, // Using your defined BottomNav
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Navigate to Create Challenge Screen */ },
                containerColor = Color(0xFF00E676),
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Battle")
            }
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

            // Header
            Text(
                text = "${viewModel.userCareer.value} Clan Battles",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Join active sessions and build your legacy.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Loading / Content / Empty Logic
            if (viewModel.isLoading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E676))
                }
            } else if (viewModel.activeChallenges.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No active battles in your clan.", color = Color.DarkGray)
                        TextButton(onClick = { viewModel.fetchUserCareerAndChallenges() }) {
                            Text("Refresh", color = Color(0xFF00E676))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(viewModel.activeChallenges) { challenge ->
                        ChallengeCard(challenge) {
                            viewModel.joinChallenge(challenge.id) {
                                navController.navigate("lobby/${challenge.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeCard(challenge: ChallengeModel, onJoin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Title & Entry Fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = Color(0xFF00E676).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${challenge.entryFee} 🪙",
                        color = Color(0xFF00E676),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats (Time & Participants)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${challenge.duration}m Focus", color = Color.Gray, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val full = challenge.participants.size >= challenge.maxParticipants
                    Text(
                        text = "${challenge.participants.size}/${challenge.maxParticipants} Warriors",
                        color = if (full) Color.Red else Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Join Button
            Button(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (challenge.participants.size >= challenge.maxParticipants) Color.DarkGray else Color(0xFF00E676)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = challenge.participants.size < challenge.maxParticipants
            ) {
                Text(
                    text = if (challenge.participants.size >= challenge.maxParticipants) "Battle Full" else "Join Battle",
                    color = if (challenge.participants.size >= challenge.maxParticipants) Color.Gray else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}