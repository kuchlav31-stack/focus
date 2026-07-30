package com.dark.focusclan.models

import com.google.firebase.firestore.PropertyName

/**
 * Production-ready User Profile Model.
 * Default values ensure Firestore can deserialize the data correctly even if some fields are missing.
 */
data class UserProfile(
    // Identification & Profile
    val uid: String = "",
    val username: String = "",         // Unique Focus ID (e.g., @warrior_99)
    val fullName: String = "",         // Display Name (e.g., Rahul Gupta)
    val profilePicUrl: String = "",    // NEW: URL for Firebase Storage image
    val friendsList: List<String> = emptyList(), // Array of Friend UIDs

    // Clan / Professional Info
    val career: String = "",           // Career Field (e.g., Coder, UPSC)
    val motto: String = "",            // Focus Motto / Bio

    // Stats & Gamification
    val totalHours: Double = 0.0,      // Total cumulative focus hours
    val coins: Int = 0,                // Total War Coins earned
    val streak: Int = 0,               // Current consecutive days streak

    // Live Social Status
    // @get:PropertyName is necessary for Boolean fields starting with "is"
    // to map correctly between Kotlin and Firebase.
    @get:PropertyName("isFocusing")
    @set:PropertyName("isFocusing")
    var isFocusing: Boolean = false,   // Shows Green Dot to friends

    // Gatekeeping & Onboarding
    @get:PropertyName("isProfileComplete")
    @set:PropertyName("isProfileComplete")
    var isProfileComplete: Boolean = false, // Checks if user finished setup

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),

    // Transient UI State (Not saved in Firestore users collection usually)
    // Used for Discovery Screen to track button states locally
    var requestStatus: String = "none" // "none", "sent", "friends"
)