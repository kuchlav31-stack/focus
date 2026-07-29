package com.dark.focusclan.ui.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Data States
    var fullName by remember { mutableStateOf("") }
    val emailPrefix = auth.currentUser?.email?.substringBefore("@") ?: "warrior"
    var focusId by remember { mutableStateOf("${emailPrefix}_${(100..999).random()}") }
    var careerField by remember { mutableStateOf("Select Your Clan") }
    var userBio by remember { mutableStateOf("") } // Extra detail for social profile

    // UI States
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val careers = listOf("UPSC Aspirant", "Coder", "Student", "Business", "Fitness", "Creative Artist")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0F0F), Color(0xFF121212))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            // 1. Step Indicator & Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Step 2 of 2", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "Finalizing Identity", color = Color.Gray, fontSize = 12.sp)
            }
            LinearProgressIndicator(
                progress = 1f,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(4.dp).clip(CircleShape),
                color = Color(0xFF00E676),
                trackColor = Color(0xFF1E1E1E)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 2. Avatar Preview (Dynamic Initials)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                if (fullName.isNotEmpty()) {
                    Text(text = fullName.take(1).uppercase(), fontSize = 40.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.DarkGray)
                }
                // Edit Icon Overlay
                Box(modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xFF00E676), CircleShape).padding(4.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 3. Inputs
            SetupInput(value = fullName, onValueChange = { fullName = it }, label = "Your Display Name", icon = Icons.Default.Badge)

            SetupInput(
                value = focusId,
                onValueChange = { focusId = it },
                label = "Unique Focus ID",
                icon = Icons.Default.AlternateEmail,
                trailing = {
                    IconButton(onClick = { focusId = "${emailPrefix}_${(100..999).random()}" }) {
                        Icon(Icons.Default.Autorenew, contentDescription = null, tint = Color(0xFF00E676))
                    }
                }
            )

            // 4. Detailed Career Picker
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = careerField,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Choose Your Clan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E676),
                        unfocusedBorderColor = Color(0xFF1E1E1E),
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                    careers.forEach { field ->
                        DropdownMenuItem(
                            text = { Text(field, color = Color.White) },
                            onClick = {
                                careerField = field
                                expanded = false
                            }
                        )
                    }
                }
            }

            // 5. Short Bio/Motto (Optional)
            OutlinedTextField(
                value = userBio,
                onValueChange = { if (it.length <= 50) userBio = it },
                label = { Text("Focus Motto (e.g. Can't Stop)") },
                placeholder = { Text("I will conquer my goals", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E676),
                    unfocusedBorderColor = Color(0xFF1E1E1E),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 6. Final Action Button
            Button(
                onClick = {
                    val userId = auth.currentUser?.uid
                    if (userId != null && careerField != "Select Your Clan" && fullName.isNotEmpty()) {
                        isLoading = true
                        val userMap = hashMapOf(
                            "fullName" to fullName,
                            "username" to focusId.lowercase(),
                            "career" to careerField,
                            "motto" to userBio,
                            "coins" to 100,
                            "totalHours" to 0,
                            "streak" to 0,
                            "isProfileComplete" to true,
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(userId).set(userMap)
                            .addOnSuccessListener {
                                isLoading = false
                                navController.navigate("permission_setup") {
                                    popUpTo("profile_setup") { inclusive = true }
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Cloud Sync Failed. Try again.", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Full Name & Clan are required!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else {
                    Text("START YOUR LEGACY", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
                }
            }

            TextButton(
                onClick = { navController.navigate("home") },
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Text("Later, let me explore first", color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailing: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
        trailingIcon = trailing,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00E676),
            unfocusedBorderColor = Color(0xFF1E1E1E),
            focusedContainerColor = Color(0xFF1E1E1E),
            unfocusedContainerColor = Color(0xFF1E1E1E),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFF00E676)
        )
    )
}