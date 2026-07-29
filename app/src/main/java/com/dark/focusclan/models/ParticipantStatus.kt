package com.dark.focusclan.models

data class ParticipantStatus(
    val uid: String = "",
    val name: String = "",
    val hasQuit: Boolean = false,
    val progress: Float = 0f // 0.0 to 1.0
)