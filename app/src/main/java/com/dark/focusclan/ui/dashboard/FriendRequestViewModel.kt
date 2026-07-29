package com.dark.focusclan.ui.dashboard

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.dark.focusclan.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val incomingRequests = mutableStateListOf<UserProfile>()
    val isLoading = mutableStateOf(false)

    fun listenToRequests() {
        val myUid = auth.currentUser?.uid ?: return
        if (isLoading.value) return // Prevent multiple simultaneous calls

        isLoading.value = true

        // Step 1: Listen for requests sent TO ME
        db.collection("friend_requests")
            .whereEqualTo("to", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    isLoading.value = false
                    return@addSnapshotListener
                }

                if (snap != null && !snap.isEmpty) {
                    val senderIds = snap.documents.mapNotNull { it.getString("from") }
                    // Step 2: Fetch profiles for these IDs
                    fetchUsersIndividually(senderIds)
                } else {
                    incomingRequests.clear()
                    isLoading.value = false
                }
            }
    }

    private fun fetchUsersIndividually(ids: List<String>) {
        val fetchedProfiles = mutableListOf<UserProfile>()
        var processedCount = 0

        if (ids.isEmpty()) {
            incomingRequests.clear()
            isLoading.value = false
            return
        }

        for (id in ids) {
            db.collection("users").document(id).get()
                .addOnSuccessListener { doc ->
                    val profile = doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                    if (profile != null) {
                        fetchedProfiles.add(profile)
                    }
                    processedCount++

                    // Jab saari IDs check ho jayein, tab list update karo
                    if (processedCount == ids.size) {
                        incomingRequests.clear()
                        incomingRequests.addAll(fetchedProfiles)
                        isLoading.value = false
                    }
                }
                .addOnFailureListener {
                    processedCount++
                    if (processedCount == ids.size) isLoading.value = false
                }
        }
    }

    fun acceptRequest(sender: UserProfile) {
        val myUid = auth.currentUser?.uid ?: return
        // Format: SenderID_ReceiverID (Jaise DiscoveryViewModel me set kiya tha)
        val requestDocId = "${sender.uid}_$myUid"

        db.runTransaction { transaction ->
            val myRef = db.collection("users").document(myUid)
            val senderRef = db.collection("users").document(sender.uid)
            val requestRef = db.collection("friend_requests").document(requestDocId)

            // Update both users friend lists
            transaction.update(myRef, "friendsList", FieldValue.arrayUnion(sender.uid))
            transaction.update(senderRef, "friendsList", FieldValue.arrayUnion(myUid))

            // Delete the request record
            transaction.delete(requestRef)
            null
        }.addOnSuccessListener {
            Log.d("Chat", "Friend Request Accepted")
        }
    }

    fun declineRequest(senderUid: String) {
        val myUid = auth.currentUser?.uid ?: return
        db.collection("friend_requests").document("${senderUid}_$myUid").delete()
    }
}