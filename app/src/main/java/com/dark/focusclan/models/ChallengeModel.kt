package com.dark.focusclan.models

/**
 * Challenge Model for Multiplayer Battles
 */
data class ChallengeModel(
    val id: String = "",
    val title: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val career: String = "",
    val duration: Int = 25,
    val entryFee: Int = 0,
    val startTime: Long = 0L,         // Timestamp jab battle shuru hogi
    val status: String = "lobby",      // "lobby" (open), "active" (ongoing), "finished"
    val participants: List<String> = emptyList(),

    // YAHAN FIX HAI: Is field ko add kiya gaya hai
    val maxParticipants: Int = 5,

    val results: Map<String, String> = emptyMap() // Map of UserID to status ("completed"/"quit")
)