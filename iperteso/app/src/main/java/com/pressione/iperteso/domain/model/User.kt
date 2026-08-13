package com.pressione.iperteso.domain.model

/**
 * Domain model for a user, matching the public.users table schema.
 */
data class User(
    val username: String,
    val email: String,
    val role: String = "user",          // "admin" or "user"
    val active: Boolean = true,
    val birthDate: String? = null,      // ISO date string
    val gender: String? = null,         // "male", "female", "other"
    val profileCompleted: Boolean = false,
    val skipProfilePrompt: Boolean = false
)
