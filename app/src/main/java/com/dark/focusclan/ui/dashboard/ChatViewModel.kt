package com.dark.focusclan.ui.dashboard

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.dark.focusclan.models.Message
import com.dark.focusclan.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val myUid = auth.currentUser?.uid ?: ""

    val friendsList = mutableStateListOf<UserProfile>()
    val messages = mutableStateListOf<Message>()

    // 1. Doston ki list fetch karna
    fun fetchFriends() {
        db.collection("users").document(myUid).get().addOnSuccessListener { doc ->
            val friendIds = doc.get("friendsList") as? List<String> ?: emptyList()
            if (friendIds.isNotEmpty()) {
                db.collection("users").whereIn("uid", friendIds).addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        friendsList.clear()
                        friendsList.addAll(snap.toObjects(UserProfile::class.java))
                    }
                }
            }
        }
    }

    // 2. Real-time Messages sun-na
    fun listenToMessages(friendUid: String) {
        val chatId = if (myUid < friendUid) "${myUid}_$friendUid" else "${friendUid}_$myUid"

        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    messages.clear()
                    messages.addAll(snap.toObjects(Message::class.java))
                }
            }
    }

    // 3. Message bhejna
    fun sendMessage(friendUid: String, text: String) {
        if (text.isBlank()) return
        val chatId = if (myUid < friendUid) "${myUid}_$friendUid" else "${friendUid}_$myUid"
        val msg = Message(senderId = myUid, text = text, timestamp = System.currentTimeMillis())

        db.collection("chats").document(chatId).collection("messages").add(msg)
        db.collection("chats").document(chatId).set(
            mapOf("lastMessage" to text, "timestamp" to System.currentTimeMillis(), "participants" to listOf(myUid, friendUid))
        )
    }
    // ChatViewModel.kt mein ye function add karein:

    fun sendBattleInvite(friendUid: String, friendName: String) {
        val myUid = auth.currentUser?.uid ?: return
        val battleId = UUID.randomUUID().toString()

        db.collection("users").document(myUid).get().addOnSuccessListener { myDoc ->
            val myName = myDoc.getString("username") ?: "Warrior"

            val battleData = hashMapOf(
                "battleId" to battleId,
                "challengerId" to myUid,
                "challengerName" to myName,
                "receiverId" to friendUid,
                "receiverName" to friendName,
                "duration" to 25,
                "status" to "invited", // Status: invited -> active
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("battles").document(battleId).set(battleData)
        }
    }}