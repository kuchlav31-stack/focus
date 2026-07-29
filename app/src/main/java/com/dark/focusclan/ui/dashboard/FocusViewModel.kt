package com.dark.focusclan.ui.dashboard

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.focusclan.models.ParticipantStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FocusViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val timeLeft = mutableStateOf(0L) // In seconds
    val participants = mutableStateListOf<ParticipantStatus>()
    val isSessionComplete = mutableStateOf(false)

    fun startFocusSession(challengeId: String, durationMins: Int) {
        timeLeft.value = durationMins * 60L
        listenToParticipants(challengeId)

        // Timer countdown
        viewModelScope.launch {
            while (timeLeft.value > 0) {
                delay(1000)
                timeLeft.value--
            }
            onSessionSuccess(durationMins)
        }
    }

    private fun listenToParticipants(challengeId: String) {
        db.collection("challenges").document(challengeId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val pList = snapshot.get("participants") as? List<String> ?: emptyList()
                    // Logic to fetch names and quit status of participants
                    // Simplified for this code
                }
            }
    }

    private fun onSessionSuccess(minutes: Int) {
        isSessionComplete.value = true
        val userId = auth.currentUser?.uid ?: return

        // Update Coins and Hours in Firestore
        val updates = hashMapOf(
            "coins" to FieldValue.increment(minutes.toLong()),
            "totalHours" to FieldValue.increment((minutes / 60.0)),
            "streak" to FieldValue.increment(1)
        )
        db.collection("users").document(userId).update(updates as Map<String, Any>)
    }

    fun quitSession(challengeId: String) {
        val userId = auth.currentUser?.uid ?: return
        // Penalty: Coins deduct karna aur "Quit" flag set karna
        db.collection("users").document(userId).update("coins", FieldValue.increment(-50))
        // Isse doston ko notification jayega ki aapne quit kiya
    }
}