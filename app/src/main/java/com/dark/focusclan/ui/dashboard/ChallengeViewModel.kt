package com.dark.focusclan.ui.dashboard

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.dark.focusclan.models.ChallengeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.UUID

class ChallengeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- Observable States ---
    val activeChallenges = mutableStateListOf<ChallengeModel>()
    val isLoading = mutableStateOf(false)
    var userCareer = mutableStateOf("General")
    var currentUserName = mutableStateOf("Warrior")

    // --- Clan Stats & Live Count ---
    var totalClanBattles = mutableIntStateOf(0)
    var runningBattlesCount = mutableIntStateOf(0) // Fixed Reference
    var topWarrior = mutableStateOf("None")

    private var challengeListener: ListenerRegistration? = null
    private var statsListener: ListenerRegistration? = null
    var challengeDuration = mutableIntStateOf(25) // Default 25 set rakhein

    /**
     * 1. Sabse pehle User Profile fetch karein.
     */
    fun fetchUserCareerAndChallenges() {
        val uid = auth.currentUser?.uid ?: return
        isLoading.value = true

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val career = doc.getString("career") ?: "General"
            val name = doc.getString("username") ?: "Warrior"

            userCareer.value = career
            currentUserName.value = name

            // Real-time listeners start karein
            observeClanChallenges(career)
            fetchClanGlobalStats(career)
        }.addOnFailureListener {
            isLoading.value = false
        }
    }

    /**
     * 2. Real-time Challenges Listener:
     * - Sirf user ke career clan ke battles dikhao.
     * - Dashboard se tab remove hoga jab Window (StartTime + 1 Min) khatam ho jaye.
     */
    private fun observeClanChallenges(career: String) {
        challengeListener?.remove()

        challengeListener = db.collection("challenges")
            .whereEqualTo("career", career)
            .whereIn("status", listOf("lobby", "active"))
            .addSnapshotListener { snapshot, _ ->
                isLoading.value = false
                if (snapshot != null) {
                    val currentTime = System.currentTimeMillis()
                    val list = snapshot.toObjects(ChallengeModel::class.java)

                    // Update Active Challenges (Sirf joinable ya ongoing)
                    activeChallenges.clear()

                    // Filter: Jo battles StartTime + 1 min cross kar chuki hain, unhe feed se hatao
                    val visibleList = list.filter {
                        it.status == "active" || currentTime <= (it.startTime + 60000)
                    }.sortedBy { it.startTime }

                    activeChallenges.addAll(visibleList)

                    // Update Running Battles Count
                    runningBattlesCount.intValue = list.count { it.status == "active" }
                }
            }
    }

    /**
     * 3. HOST BATTLE: Naya challenge create karke Firestore mein post karna.
     */
    fun createChallenge(
        title: String,
        duration: Int,
        fee: Int,
        startInMins: Int,
        onComplete: () -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        val challengeId = UUID.randomUUID().toString()
        val startTimeMillis = System.currentTimeMillis() + (startInMins * 60 * 1000)

        val newChallenge = ChallengeModel(
            id = challengeId,
            title = title,
            hostId = uid,
            hostName = currentUserName.value,
            career = userCareer.value,
            duration = duration,
            entryFee = fee,
            startTime = startTimeMillis,
            status = "lobby",
            participants = listOf(uid),
            maxParticipants = 5
        )

        db.collection("challenges").document(challengeId)
            .set(newChallenge)
            .addOnSuccessListener { onComplete() }
    }

    /**
     * 4. JOIN BATTLE: Atomic Transaction + Window Check (1 min before/after).
     */
    fun joinChallenge(challengeId: String, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("challenges").document(challengeId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val participants = snapshot.get("participants") as? List<String> ?: emptyList()
            val max = snapshot.getLong("maxParticipants")?.toInt() ?: 5
            val startTime = snapshot.getLong("startTime") ?: 0L
            val currentTime = System.currentTimeMillis()

            // 2 Minute Joining Window Logic
            val windowStart = startTime - 60000
            val windowEnd = startTime + 60000
            val isWindowOpen = currentTime in windowStart..windowEnd

            if (!isWindowOpen) {
                return@runTransaction "WINDOW_CLOSED"
            }

            if (participants.size < max && !participants.contains(uid)) {
                transaction.update(docRef, "participants", FieldValue.arrayUnion(uid))
                "SUCCESS"
            } else if (participants.contains(uid)) {
                "ALREADY_JOINED"
            } else {
                "LOBBY_FULL"
            }
        }.addOnSuccessListener { result ->
            when (result.toString()) {
                "SUCCESS", "ALREADY_JOINED" -> onResult(true, "Successfully Joined!")
                "WINDOW_CLOSED" -> onResult(false, "Joining window is closed.")
                "LOBBY_FULL" -> onResult(false, "The lobby is already full!")
                else -> onResult(false, "Join failed.")
            }
        }.addOnFailureListener { e ->
            onResult(false, "Error: ${e.message}")
        }
    }

    /**
     * 5. Stats Logic: Total Battles aur MVP Warrior fetch karna.
     */
    private fun fetchClanGlobalStats(career: String) {
        // Total Finished Battles
        db.collection("challenges")
            .whereEqualTo("career", career)
            .whereEqualTo("status", "finished")
            .get().addOnSuccessListener { snap ->
                totalClanBattles.intValue = snap.size()
            }

        // MVP Warrior (Most Coins)
        db.collection("users")
            .whereEqualTo("career", career)
            .orderBy("coins", Query.Direction.DESCENDING)
            .limit(1)
            .get().addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    topWarrior.value = snap.documents[0].getString("username") ?: "None"
                }
            }
    }

    fun markResult(challengeId: String, status: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("challenges").document(challengeId).update("results.$uid", status)
    }

    fun completeChallenge(challengeId: String) {
        db.collection("challenges").document(challengeId).update("status", "finished")
    }

    override fun onCleared() {
        challengeListener?.remove()
        statsListener?.remove()
        super.onCleared()
    }
}