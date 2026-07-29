package com.dark.focusclan.ui.dashboard

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.dark.focusclan.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DiscoveryViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val usersList = mutableStateListOf<UserProfile>()
    val isLoading = mutableStateOf(false)
    val currentUserCareer = mutableStateOf("")

    fun initDiscovery() {
        val uid = auth.currentUser?.uid ?: return
        isLoading.value = true
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val career = doc.getString("career") ?: "General"
            currentUserCareer.value = career
            fetchClanWarriors(career)
        }
    }

    fun fetchClanWarriors(career: String, query: String = "") {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users")
            .whereEqualTo("career", career)
            .limit(30)
            .addSnapshotListener { snap, _ ->
                isLoading.value = false
                if (snap != null) {
                    val list = snap.documents.mapNotNull { doc ->
                        doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                    }
                    usersList.clear()
                    usersList.addAll(list.filter { it.uid != uid &&
                            (query.isEmpty() || it.username.contains(query, ignoreCase = true))
                    })
                }
            }
    }

    fun sendClanRequest(targetUid: String, onComplete: (Boolean) -> Unit) {
        val myUid = auth.currentUser?.uid ?: return
        val requestId = "${myUid}_$targetUid"

        val request = hashMapOf(
            "from" to myUid,
            "to" to targetUid,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("friend_requests").document(requestId).set(request)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}