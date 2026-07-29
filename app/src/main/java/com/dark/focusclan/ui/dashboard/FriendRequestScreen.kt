package com.dark.focusclan.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dark.focusclan.models.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestScreen(navController: NavController, viewModel: FriendRequestViewModel = viewModel()) {

    // Start listening when screen opens
    LaunchedEffect(Unit) {
        viewModel.listenToRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clan Invitations", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {

            if (viewModel.isLoading.value && viewModel.incomingRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E676))
                }
            } else if (viewModel.incomingRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.GroupOff, null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No pending invites", color = Color.Gray, fontSize = 16.sp)
                        Text("Your clan is quiet for now.", color = Color.DarkGray, fontSize = 12.sp)
                    }
                }
            } else {
                Text(
                    text = "${viewModel.incomingRequests.size} warriors want to connect",
                    color = Color(0xFF00E676),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(viewModel.incomingRequests) { sender ->
                        IncomingRequestCard(
                            user = sender,
                            onAccept = { viewModel.acceptRequest(sender) },
                            onDecline = { viewModel.declineRequest(sender.uid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingRequestCard(user: UserProfile, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                Text(user.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(user.career, color = Color.Gray, fontSize = 12.sp)
            }

            Row {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2C2C2C))
                ) {
                    Icon(Icons.Default.Close, "Decline", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onAccept,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF00E676))
                ) {
                    Icon(Icons.Default.Check, "Accept", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}