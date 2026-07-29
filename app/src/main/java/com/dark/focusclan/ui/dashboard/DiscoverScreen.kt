package com.dark.focusclan.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dark.focusclan.models.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(navController: NavController, viewModel: DiscoveryViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) { viewModel.initDiscovery() }

    Scaffold(
        bottomBar = { AppBottomNavigation(navController) },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Discover Clan", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Same career, same mission.", color = Color.Gray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.fetchClanWarriors(viewModel.currentUserCareer.value, it) },
                placeholder = { Text("Search Focus ID...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00E676), unfocusedContainerColor = Color(0xFF1E1E1E), focusedContainerColor = Color(0xFF1E1E1E), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (viewModel.isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF00E676))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(viewModel.usersList) { user ->
                        UserDiscoveryCard(user) { selectedUser = user; showSheet = true }
                    }
                }
            }
        }

        if (showSheet && selectedUser != null) {
            ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState, containerColor = Color(0xFF1E1E1E)) {
                UserProfileDetailView(selectedUser!!) {
                    viewModel.sendClanRequest(selectedUser!!.uid) { showSheet = false }
                }
            }
        }
    }
}

@Composable
fun UserDiscoveryCard(user: UserProfile, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
                    Text(user.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (user.isFocusing) Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF00E676)).border(2.dp, Color(0xFF1E1E1E), CircleShape))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, color = Color.White, fontWeight = FontWeight.Bold)
                Text(user.career, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.DarkGray)
        }
    }
}

@Composable
fun UserProfileDetailView(user: UserProfile, onAddClick: () -> Unit) {
    var isSent by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("@${user.username}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(user.career, color = Color(0xFF00E676))
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${user.coins}", color = Color.White, fontWeight = FontWeight.Bold); Text("Coins", color = Color.Gray, fontSize = 12.sp) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${user.streak}d", color = Color.White, fontWeight = FontWeight.Bold); Text("Streak", color = Color.Gray, fontSize = 12.sp) }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { isSent = true; onAddClick() },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if(isSent) Color.Gray else Color(0xFF00E676)),
            enabled = !isSent
        ) {
            Text(if(isSent) "REQUEST SENT" else "ADD TO CLAN", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}