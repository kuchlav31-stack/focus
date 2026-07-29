package com.dark.focusclan.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.dark.focusclan.utils.PermissionUtils

@Composable
fun ModeSelectionScreen(navController: NavController, time: Int) {
    var selectedMode by remember { mutableIntStateOf(1) } // 1: Basic, 2: Advanced, 3: Nuclear
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp)) {
        Text("Select Severity", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Choose your level of discipline", color = Color.Gray)

        Spacer(modifier = Modifier.height(30.dp))

        // Mode Options
        ModeChoiceCard(1, "Basic Mode", "Visual timer only. No app blocking.", selectedMode == 1) { selectedMode = 1 }
        ModeChoiceCard(2, "Advanced Mode", "Anti-Bypass. Pull-back logic enabled.", selectedMode == 2) { selectedMode = 2 }
        ModeChoiceCard(3, "Nuclear Mode", "Unbreakable. Restart protection enabled.", selectedMode == 3) { selectedMode = 3 }

        Spacer(modifier = Modifier.height(24.dp))

        // Dynamic Disclaimer
        val disclaimerColor = when(selectedMode) {
            1 -> Color(0xFF00E676)
            2 -> Color(0xFFFFA500)
            else -> Color(0xFFCF6679)
        }

        Surface(color = disclaimerColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, disclaimerColor.copy(alpha = 0.3f))) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Warning, null, tint = disclaimerColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when(selectedMode) {
                        1 -> "Good for self-discipline. You can exit anytime."
                        2 -> "App will pull you back if you try to open other apps."
                        else -> "EXTREME: Phone restart will NOT break the lock."
                    },
                    color = disclaimerColor, fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                // Nuclear & Advanced need permissions
                if (selectedMode > 1) {
                    if (PermissionUtils.allPermissionsGranted(context)) {
                        navController.navigate("focus_mode/solo/$time/$selectedMode")
                    } else {
                        navController.navigate("permission_setup")
                    }
                } else {
                    // Basic needs no special permissions
                    navController.navigate("focus_mode/solo/$time/1")
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = disclaimerColor)
        ) {
            Text("ACTIVATE ${getModeName(selectedMode)}", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ModeChoiceCard(id: Int, title: String, desc: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() }
            .border(if (isSelected) 2.dp else 0.dp, Color(0xFF00E676), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E676)))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

fun getModeName(mode: Int) = when(mode) { 1 -> "BASIC" 2 -> "ADVANCED" else -> "NUCLEAR" }