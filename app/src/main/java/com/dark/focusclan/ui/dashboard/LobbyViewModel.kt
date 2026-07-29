package com.dark.focusclan.ui.dashboard

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.dark.focusclan.models.ChatMessage
import com.dark.focusclan.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LobbyViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val messages = mutableStateListOf<ChatMessage>()
    val participants = mutableStateListOf<UserProfile>()
    val challengeStatus = mutableStateOf("lobby")
    val isHost = mutableStateOf(false)
    val currentUserName = mutableStateOf("Warrior")
    var challengeDuration = mutableStateOf(25)

    fun listenToLobby(challengeId: String) {
        val uid = auth.currentUser?.uid ?: return

        // 1. Get My Name
        db.collection("users").document(uid).get().addOnSuccessListener {
            currentUserName.value = it.getString("username") ?: "Warrior"
        }

        // 2. Listen to Challenge & Participants
        db.collection("challenges").document(challengeId).addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                challengeStatus.value = snap.getString("status") ?: "lobby"
                challengeDuration.value = snap.getLong("duration")?.toInt() ?: 25
                isHost.value = snap.getString("hostId") == uid

                val pIds = snap.get("participants") as? List<String> ?: emptyList()
                fetchParticipantDetails(pIds)
            }
        }

        // 3. Listen to Chat
        db.collection("challenges").document(challengeId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    messages.clear()
                    messages.addAll(snap.toObjects(ChatMessage::class.java))
                }
            }
    }

    private fun fetchParticipantDetails(ids: List<String>) {
        if (ids.isEmpty()) return
        db.collection("users").whereIn("uid", ids).get().addOnSuccessListener { snap ->
            participants.clear()
            participants.addAll(snap.toObjects(UserProfile::class.java))
        }
    }

    fun sendMessage(challengeId: String, text: String) {
        if (text.isBlank()) return
        val msg = ChatMessage(
            senderId = auth.currentUser?.uid ?: "",
            senderName = currentUserName.value,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        db.collection("challenges").document(challengeId).collection("messages").add(msg)
    }

    fun startLockdown(challengeId: String) {
        db.collection("challenges").document(challengeId).update("status", "active")
    }
}