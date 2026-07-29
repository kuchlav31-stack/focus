package com.dark.focusclan.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dark.focusclan.models.BattleModel
import com.dark.focusclan.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    friendUid: String,
    friendName: String,
    viewModel: ChatViewModel = viewModel()
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val myUid = auth.currentUser?.uid ?: ""
    val context = LocalContext.current

    // UI States
    var text by remember { mutableStateOf("") }
    var showChallengeDialog by remember { mutableStateOf(false) }
    var activeBattleWithFriend by remember { mutableStateOf<BattleModel?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 1. Real-time Message Listener & Battle Listener
    LaunchedEffect(friendUid) {
        viewModel.listenToMessages(friendUid)

        // Listen if there is an active/invited battle between these two
        db.collection("battles")
            .whereIn("status", listOf("invited", "active"))
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val battles = snap.toObjects(BattleModel::class.java)
                    activeBattleWithFriend = battles.find {
                        (it.challengerId == myUid && it.receiverId == friendUid) ||
                                (it.challengerId == friendUid && it.receiverId == myUid)
                    }

                    // IF BATTLE IS ACTIVE -> START LOCKDOWN SIMULTANEOUSLY
                    if (activeBattleWithFriend?.status == "active") {
                        navController.navigate("focus_mode/${activeBattleWithFriend!!.battleId}/${activeBattleWithFriend!!.duration}/3")
                    }
                }
            }
    }

    // 2. Auto-scroll to bottom when new message arrives
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // FIX: Content keyboard ke upar shift hoga
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(friendName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Online • Warrior", fontSize = 11.sp, color = Color(0xFF00E676))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showChallengeDialog = true }) {
                        Icon(Icons.Default.FlashOn, "Duel", tint = Color(0xFF00E676))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White)
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // 3. Persistent Battle Status Card (Top of messages)
            AnimatedVisibility(visible = activeBattleWithFriend != null) {
                BattleStatusBanner(
                    battle = activeBattleWithFriend,
                    myUid = myUid,
                    onAccept = {
                        db.collection("battles").document(activeBattleWithFriend!!.battleId).update("status", "active")
                    },
                    onCancel = {
                        db.collection("battles").document(activeBattleWithFriend!!.battleId).delete()
                    }
                )
            }

            // 4. Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(viewModel.messages) { msg ->
                    ChatBubble(msg.text, msg.senderId == myUid)
                }
            }

            // 5. Bottom Input Area
            Surface(
                color = Color(0xFF1E1E1E),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Message...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF121212),
                            unfocusedContainerColor = Color(0xFF121212),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                viewModel.sendMessage(friendUid, text)
                                text = ""
                            }
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

        // --- Duel Invitation Dialog ---
        if (showChallengeDialog) {
            AlertDialog(
                onDismissRequest = { showChallengeDialog = false },
                containerColor = Color(0xFF1E1E1E),
                title = { Text("Initiate Duel", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Challenge $friendName to a 25-minute synchronized focus session?", color = Color.Gray) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.sendBattleInvite(friendUid, friendName)
                            showChallengeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) { Text("SEND INVITE", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            )
        }
    }
}

@Composable
fun BattleStatusBanner(battle: BattleModel?, myUid: String, onAccept: () -> Unit, onCancel: () -> Unit) {
    if (battle == null) return
    val isChallenger = battle.challengerId == myUid

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00E676).copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, null, tint = Color(0xFF00E676))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isChallenger) "Waiting for Warrior..." else "Focus Duel Invitation!",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                Text("${battle.duration} Mins • Nuclear Mode", color = Color.Gray, fontSize = 11.sp)
            }
            if (!isChallenger) {
                IconButton(onClick = onAccept) { Icon(Icons.Default.CheckCircle, null, tint = Color.Green) }
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Cancel, null, tint = Color.Red) }
        }
    }
}

@Composable
fun ChatBubble(message: String, isMe: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isMe) Color(0xFF00E676) else Color(0xFF2C2C2C),
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (isMe) Color.Black else Color.White,
                fontSize = 15.sp
            )
        }
    }
}