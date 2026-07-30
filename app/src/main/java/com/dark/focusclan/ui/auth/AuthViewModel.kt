package com.dark.foodcustomer.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    val isLoading = MutableStateFlow(false)

    // --- 1. Unified Firestore Save Logic ---
    private suspend fun saveUserToFirestore(uid: String, name: String, email: String, phone: String = "") {
        val userRef = db.collection("users").document(uid)
        val snapshot = userRef.get().await()

        if (!snapshot.exists()) {
            val userMap = hashMapOf(
                "uid" to uid,
                "fullName" to name,
                "email" to email,
                "phone" to phone,
                "coins" to 0,
                "isProfileComplete" to false, // Next screen pe details bharne ke liye
                "createdAt" to System.currentTimeMillis()
            )
            userRef.set(userMap).await()
        }
    }

    // --- 2. Email Sign Up ---
    fun signUpWithEmail(name: String, email: String, pass: String, phone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                result.user?.let {
                    saveUserToFirestore(it.uid, name, email, phone)
                    onSuccess()
                }
            } catch (e: Exception) {
                // Handle Error
            } finally {
                isLoading.value = false
            }
        }
    }

    // --- 3. Google Sign In Logic ---
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                result.user?.let {
                    saveUserToFirestore(it.uid, it.displayName ?: "Warrior", it.email ?: "")
                    onSuccess()
                }
            } catch (e: Exception) {
                // Handle Error
            } finally {
                isLoading.value = false
            }
        }
    }
}