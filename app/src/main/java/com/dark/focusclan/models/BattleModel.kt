package com.dark.focusclan.models
data class BattleModel(
    val battleId: String = "",
    val challengerId: String = "",
    val receiverId: String = "",
    val type: String = "timer", // "timer" or "survival"
    val duration: Int = 60,      // Minutes for timer mode
    val status: String = "invited", // "invited", "active", "finished"
    val challengerStatus: String = "ready", // "focusing", "quit", "completed"
    val receiverStatus: String = "ready",
    val startTime: Long = 0L
)