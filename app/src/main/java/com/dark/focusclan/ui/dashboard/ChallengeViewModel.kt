package com.dark.focusclan.ui.dashboard

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.dark.focusclan.models.ChallengeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ChallengeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val activeChallenges = mutableStateListOf<ChallengeModel>()
    val isLoading = mutableStateOf(false)
    var userCareer = mutableStateOf("")

    private var listener: ListenerRegistration? = null

    // 1. Pehle User ka Career fetch karo
    fun fetchUserCareerAndChallenges() {
        val uid = auth.currentUser?.uid ?: return
        isLoading.value = true

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val career = doc.getString("career") ?: "General"
            userCareer.value = career
            observeChallenges(career) // Career milte hi challenges observe karo
        }
    }

    // 2. Real-time Challenges fetch karo jo uske Clan (Career) ke hon
    private fun observeChallenges(career: String) {
        listener?.remove() // Purana listener hatao agar hai toh

        listener = db.collection("challenges")
            .whereEqualTo("career", career)
            .whereEqualTo("status", "lobby") // Sirf wahi jo abhi shuru nahi hue
            .addSnapshotListener { snapshot, error ->
                isLoading.value = false
                if (snapshot != null) {
                    val list = snapshot.toObjects(ChallengeModel::class.java)
                    activeChallenges.clear()
                    activeChallenges.addAll(list)
                }
            }
    }

    // 3. Challenge Join karne ka Atomic Logic
    fun joinChallenge(challengeId: String, onJoinSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("challenges").document(challengeId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val participants = snapshot.get("participants") as? List<String> ?: emptyList()
            val max = snapshot.getLong("maxParticipants") ?: 5

            if (participants.size < max && !participants.contains(uid)) {
                transaction.update(docRef, "participants", FieldValue.arrayUnion(uid))
                true
            } else {
                false
            }
        }.addOnSuccessListener { success ->
            if (success as Boolean) onJoinSuccess()
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}