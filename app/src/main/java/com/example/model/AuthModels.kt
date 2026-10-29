package com.example.model

enum class LoginType {
    GOOGLE,
    EMAIL,
    ADMIN
}

data class UserSession(
    val id: String,
    val email: String,
    val displayName: String,
    val isAdmin: Boolean = false,
    val isAdFree: Boolean = true,
    val loginType: LoginType = LoginType.EMAIL,
    val signedInAtMillis: Long = System.currentTimeMillis()
) {
    val displayInitials: String
        get() {
            val parts = displayName.trim().split(" ")
            return if (parts.size >= 2) {
                "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
            } else {
                displayName.take(2).uppercase()
            }
        }

    companion object {
        const val ADMIN_EMAIL = "peterslloyd472@gmail.com"

        fun isEmailAdmin(email: String): Boolean {
            return email.trim().equals(ADMIN_EMAIL, ignoreCase = true)
        }
    }
}
