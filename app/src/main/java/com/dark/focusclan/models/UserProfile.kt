package com.dark.focusclan.models

import com.google.firebase.firestore.PropertyName

/**
 * Production-ready User Profile Model
 * Default values ensure Firestore can deserialize the data correctly.
 */
data class UserProfile(
    // Identification
    val uid: String = "",
    val username: String = "",    // Unique Focus ID (e.g., @rahul_12)
    val fullName: String = "",    // Display Name
    val friendsList: List<String> = emptyList(), // UIDs of friends

    // Clan / Professional Info
    val career: String = "",      // Career Field (e.g., Coder, UPSC)
    val motto: String = "",       // Profile Motto/Bio

    // Stats & Gamification
    val totalHours: Double = 0.0, // Precision ke liye Double (e.g., 1.5 hours)
    val coins: Int = 0,
    val streak: Int = 0,          // Daily focus streak counter

    // Live Social Status
    // @get:PropertyName aur @set:PropertyName isliye taaki Firebase
    // "isFocusing" field ko sahi se map kare (boolean naming convention)
    @get:PropertyName("isFocusing")
    @set:PropertyName("isFocusing")
    var isFocusing: Boolean = false,

    // Gatekeeping
    @get:PropertyName("isProfileComplete")
    @set:PropertyName("isProfileComplete")
    var isProfileComplete: Boolean = false,

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),

    // UI Helper State (This is usually not stored in the user doc itself
    // but used for Discovery Screen logic)
    var requestStatus: String = "none" // values: "none", "sent", "friends"
)