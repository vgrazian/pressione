package com.pressione.iperteso.domain.model

/**
 * Auth session state, matching the web app's 8-hour TTL session.
 */
data class AuthSession(
    val username: String,
    val role: String,
    val email: String,
    val loginTimestamp: Long = System.currentTimeMillis(),
    val ttlMinutes: Int = 480 // 8 hours
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() - loginTimestamp > ttlMinutes * 60_000L

    val isAdmin: Boolean
        get() = role == "admin"
}
