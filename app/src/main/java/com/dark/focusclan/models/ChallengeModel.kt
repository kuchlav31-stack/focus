package com.dark.focusclan.models
data class ChallengeModel(
    val id: String = "",
    val title: String = "",
    val hostId: String = "",
    val duration: Int = 25,
    val entryFee: Int = 0,
    val participants: List<String> = emptyList(),
    val maxParticipants: Int = 5,
    val status: String = "lobby",
    val career: String = ""
)