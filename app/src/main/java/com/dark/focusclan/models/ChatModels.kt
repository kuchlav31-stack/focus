package com.dark.focusclan.models

data class ChatRoom(
    val chatId: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L,
    val participants: List<String> = emptyList()
)

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)