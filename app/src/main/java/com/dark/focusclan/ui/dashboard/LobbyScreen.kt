package com.dark.focusclan.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import com.dark.focusclan.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(navController: NavController, challengeId: String, viewModel: LobbyViewModel = viewModel()) {
    var messageText by remember { mutableStateOf("") }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(challengeId) {
        viewModel.listenToLobby(challengeId)
    }

    // SIMULTANEOUS LOCKDOWN TRIGGER
    LaunchedEffect(viewModel.challengeStatus.value) {
        if (viewModel.challengeStatus.value == "active") {
            navController.navigate("focus_mode/$challengeId/${viewModel.challengeDuration.value}") {
                popUpTo("lobby/$challengeId") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battle Lobby", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // 1. Warriors List (Real-time Avatars)
            Text(
                "Warriors Joined",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.participants) { warrior ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).border(1.dp, Color(0xFF00E676), CircleShape), contentAlignment = Alignment.Center) {
                            Text(warrior.username.take(1).uppercase(), color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                        }
                        Text(warrior.username, color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }

            Divider(modifier = Modifier.padding(top = 16.dp), color = Color(0xFF1E1E1E))

            // 2. Chat Area
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.messages) { msg ->
                    ChatBubble(msg, msg.senderId == myUid)
                }
            }

            // 3. Action Area
            Column(modifier = Modifier.background(Color(0xFF1E1E1E)).padding(16.dp)) {
                if (viewModel.isHost.value) {
                    Button(
                        onClick = { viewModel.startLockdown(challengeId) },
                        modifier = Modifier.fillMaxWidth().height(55.dp).padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("START NUCLEAR LOCKDOWN", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Chat with warriors...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF121212),
                            unfocusedContainerColor = Color(0xFF121212),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            viewModel.sendMessage(challengeId, messageText)
                            messageText = ""
                        },
                        containerColor = Color(0xFF00E676),
                        contentColor = Color.Black,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(msg.senderName, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        }
        Surface(
            color = if (isMe) Color(0xFF00E676) else Color(0xFF2C2C2C),
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            )
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isMe) Color.Black else Color.White,
                fontSize = 14.sp
            )
        }
    }
}