package com.example.fitnessquest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * UI state your Compose screens can observe.
 */
data class AuthUiState(
    val uid: String? = null,         // non-null means logged in
    val loading: Boolean = false,
    val error: String? = null
)

/**
 * AuthViewModel:
 * - Signs up / signs in via Firebase Auth
 * - Creates initial Firestore docs on sign-up
 */
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        // If user is already signed in (app restart), reflect that immediately.
        _uiState.update { it.copy(uid = auth.currentUser?.uid) }
    }

    fun signOut() {
        auth.signOut()
        _uiState.value = AuthUiState(uid = null, loading = false, error = null)
    }

    /**
     * Email + password sign-up.
     * Creates Firestore:
     *  - users/{uid}
     *  - userStats/{uid}
     */
    fun signUpWithEmail(
        email: String,
        password: String,
        username: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(uid = null, loading = true, error = null)

            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid = result.user?.uid ?: error("Auth returned null user")

                createInitialUserDocs(uid = uid, username = username.trim(), email = email.trim())

                _uiState.value = AuthUiState(uid = uid, loading = false, error = null)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(uid = null, loading = false, error = e.message)
            }
        }
    }

    /**
     * Email + password sign-in.
     */
    fun signInWithEmailOrUsername(
        input: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(uid = null, loading = true, error = null)

            try {
                val email = if (input.contains("@")) {
                    input.trim()
                } else {
                    // lookup username → email
                    val snap = db.collection("Users")
                        .whereEqualTo("username", input.trim())
                        .limit(1)
                        .get()
                        .await()

                    if (snap.isEmpty) {
                        throw Exception("Account not found.")
                    }

                    snap.documents.first().getString("email")
                        ?: throw Exception("User email missing.")
                }

                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: error("Auth returned null user")

                _uiState.value = AuthUiState(uid = uid, loading = false, error = null)

            } catch (e: Exception) {
                _uiState.value = AuthUiState(uid = null, loading = false, error = e.message)
            }
        }
    }

    /**
     * Creates initial Firestore docs for a new account.
     * Uses UID as the document ID to keep identity consistent.
     */
    private suspend fun createInitialUserDocs(uid: String, username: String, email: String) {
        val now = FieldValue.serverTimestamp()

        val userRef = db.collection("Users").document(uid)
        val userDataRef = db.collection("UserData").document(uid)
        val leaderboardRef = db.collection("Leaderboard").document(uid)
        val userDoc = hashMapOf(
            "Username" to username,
            "DisplayName" to username,
            "CreatedAt" to now,
            "Email" to email
        )

        val statsDoc = hashMapOf(
            "totalSteps" to 0,
            "currentXp" to 0,
            "workoutsCompleted" to 0,
            "level" to 1,
            //easier query
            "displayName" to username,
            "updatedAt" to now
        )

        // Batch write so it's all-or-nothing
        db.runBatch { batch ->
            batch.set(userRef, userDoc)
            batch.set(userDataRef, statsDoc)
        }.await()
    }
}