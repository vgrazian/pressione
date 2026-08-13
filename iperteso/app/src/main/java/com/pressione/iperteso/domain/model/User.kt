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
    val skipProfilePrompt: Boolean = false,
    val firstName: String? = null,
    val lastName: String? = null,
    val fiscalCode: String? = null,
    val phone: String? = null,
    val street: String? = null,
    val streetNumber: String? = null,
    val city: String? = null,
    val postalCode: String? = null
)
