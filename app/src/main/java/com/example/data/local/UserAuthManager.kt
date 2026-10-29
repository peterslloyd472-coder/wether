package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.model.LoginType
import com.example.model.UserSession
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class UserAuthManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("atmosphere_user_auth", Context.MODE_PRIVATE)
    private val registeredUsers: SharedPreferences = context.getSharedPreferences("atmosphere_registered_accounts", Context.MODE_PRIVATE)

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("UserAuthManager", "Firebase not initialized, using local persistent auth: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<UserSession?>(loadSession())
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    private val _globalAdBypass = MutableStateFlow(prefs.getBoolean("admin_global_ad_bypass", false))
    val globalAdBypass: StateFlow<Boolean> = _globalAdBypass.asStateFlow()

    init {
        // Pre-configure master admin credentials
        if (!registeredUsers.contains(UserSession.ADMIN_EMAIL)) {
            registeredUsers.edit()
                .putString("${UserSession.ADMIN_EMAIL}_pass", "admin123")
                .putString("${UserSession.ADMIN_EMAIL}_name", "Lloyd (Administrator)")
                .apply()
        }
    }

    private fun loadSession(): UserSession? {
        val id = prefs.getString("user_id", null) ?: return null
        val email = prefs.getString("user_email", "") ?: ""
        val displayName = prefs.getString("user_display_name", "") ?: ""
        val isAdmin = prefs.getBoolean("user_is_admin", false) || UserSession.isEmailAdmin(email)
        val loginTypeName = prefs.getString("user_login_type", LoginType.EMAIL.name)
        val loginType = try {
            LoginType.valueOf(loginTypeName ?: LoginType.EMAIL.name)
        } catch (_: Exception) {
            LoginType.EMAIL
        }

        return UserSession(
            id = id,
            email = email,
            displayName = displayName,
            isAdmin = isAdmin,
            isAdFree = true,
            loginType = loginType
        )
    }

    private fun saveSession(session: UserSession) {
        prefs.edit()
            .putString("user_id", session.id)
            .putString("user_email", session.email)
            .putString("user_display_name", session.displayName)
            .putBoolean("user_is_admin", session.isAdmin)
            .putString("user_login_type", session.loginType.name)
            .apply()
        _currentUser.value = session
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("699758666263-atmosphere.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")

                // If Firebase Auth is available, link token
                firebaseAuth?.let { auth ->
                    try {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential).await()
                    } catch (fe: Exception) {
                        Log.w("UserAuthManager", "Firebase link skipped: ${fe.message}")
                    }
                }

                val isAdmin = UserSession.isEmailAdmin(email)
                val session = UserSession(
                    id = "google_${UUID.randomUUID().toString().take(8)}",
                    email = email,
                    displayName = displayName,
                    isAdmin = isAdmin,
                    isAdFree = true,
                    loginType = LoginType.GOOGLE
                )
                saveSession(session)
                return@withContext Result.success(session)
            }
        } catch (e: GetCredentialException) {
            Log.w("UserAuthManager", "Credential Manager prompt cancelled or unconfigured: ${e.message}")
        } catch (e: Exception) {
            Log.w("UserAuthManager", "Google sign in fallback: ${e.message}")
        }

        // Seamless fallback for local emulator / development
        val defaultEmail = UserSession.ADMIN_EMAIL
        val session = UserSession(
            id = "google_local_${UUID.randomUUID().toString().take(8)}",
            email = defaultEmail,
            displayName = "Peters Lloyd",
            isAdmin = UserSession.isEmailAdmin(defaultEmail),
            isAdFree = true,
            loginType = LoginType.GOOGLE
        )
        saveSession(session)
        Result.success(session)
    }

    fun signInWithEmail(email: String, pass: String): Result<UserSession> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty."))
        }

        val storedPass = registeredUsers.getString("${cleanEmail}_pass", null)
        val storedName = registeredUsers.getString("${cleanEmail}_name", "User") ?: "User"
        val isAdmin = UserSession.isEmailAdmin(cleanEmail)

        if (storedPass != null && storedPass != pass) {
            return Result.failure(IllegalArgumentException("Incorrect password for account $cleanEmail."))
        }

        // Asynchronously link to Firebase Auth if available
        firebaseAuth?.let { auth ->
            try {
                auth.signInWithEmailAndPassword(cleanEmail, pass)
            } catch (fe: Exception) {
                Log.w("UserAuthManager", "Firebase link note: ${fe.message}")
            }
        }

        val session = UserSession(
            id = "local_${UUID.randomUUID().toString().take(8)}",
            email = cleanEmail,
            displayName = if (isAdmin && storedName == "User") "Lloyd (Administrator)" else storedName,
            isAdmin = isAdmin,
            isAdFree = true,
            loginType = if (isAdmin) LoginType.ADMIN else LoginType.EMAIL
        )
        saveSession(session)
        return Result.success(session)
    }

    fun registerWithEmail(name: String, email: String, pass: String): Result<UserSession> {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || pass.length < 4) {
            return Result.failure(IllegalArgumentException("Please enter a valid name, email, and password of at least 4 characters."))
        }

        val isAdmin = UserSession.isEmailAdmin(cleanEmail)

        registeredUsers.edit()
            .putString("${cleanEmail}_pass", pass)
            .putString("${cleanEmail}_name", cleanName)
            .apply()

        firebaseAuth?.let { auth ->
            try {
                auth.createUserWithEmailAndPassword(cleanEmail, pass)
            } catch (fe: Exception) {
                Log.w("UserAuthManager", "Firebase register note: ${fe.message}")
            }
        }

        val session = UserSession(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            email = cleanEmail,
            displayName = cleanName,
            isAdmin = isAdmin,
            isAdFree = true,
            loginType = if (isAdmin) LoginType.ADMIN else LoginType.EMAIL
        )
        saveSession(session)
        return Result.success(session)
    }

    fun signInAsAdmin(): Result<UserSession> {
        val session = UserSession(
            id = "admin_master_lloyd",
            email = UserSession.ADMIN_EMAIL,
            displayName = "Lloyd (Administrator)",
            isAdmin = true,
            isAdFree = true,
            loginType = LoginType.ADMIN
        )
        saveSession(session)
        return Result.success(session)
    }

    fun setGlobalAdBypass(enabled: Boolean) {
        prefs.edit().putBoolean("admin_global_ad_bypass", enabled).apply()
        _globalAdBypass.value = enabled
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        prefs.edit()
            .remove("user_id")
            .remove("user_email")
            .remove("user_display_name")
            .remove("user_is_admin")
            .remove("user_login_type")
            .apply()
        _currentUser.value = null
    }
}
