package com.pressione.iperteso.data.repository

import com.pressione.iperteso.data.local.dao.SettingsDao
import com.pressione.iperteso.data.local.dao.UserDao
import com.pressione.iperteso.data.local.entity.UserEntity
import com.pressione.iperteso.data.remote.api.AuthApi
import com.pressione.iperteso.data.remote.api.UserResponse
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.domain.model.User
import com.pressione.iperteso.util.PasswordHasher
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeoutException

/**
 * Auth repository — table-based authentication with offline user cache.
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val settingsDao: SettingsDao
) {
    /**
     * Login: verify credentials against Supabase, create session.
     */
    suspend fun login(username: String, password: String): Result<AuthSession> {
        return try {
            val passwordHash = PasswordHasher.hash(password)

            val userResponse = withTimeout(15_000) {
                authApi.login(username, passwordHash)
            } ?: return Result.failure(AuthError.InvalidCredentials())

            // Cache user locally
            userDao.upsertUser(userResponse.toUserEntity())

            val session = AuthSession(
                username = userResponse.username,
                role = userResponse.role,
                email = userResponse.email
            )

            Result.success(session)
        } catch (e: TimeoutException) {
            Result.failure(AuthError.NetworkError("Timeout: verifica la connessione internet"))
        } catch (e: Exception) {
            Result.failure(AuthError.NetworkError(e.message ?: "Connection failed"))
        }
    }

    /**
     * Fetch and cache user profile.
     */
    suspend fun getUserProfile(username: String): User? {
        // Try local cache first
        val cached = userDao.getUser(username)
        if (cached != null) {
            return cached.toDomainUser()
        }

        // Fetch from remote
        val users = authApi.getAllUsers()
        val found = users.find { it.username == username } ?: return null
        userDao.upsertUser(found.toUserEntity())
        return found.toDomainUser()
    }

    /**
     * Fetch all users (admin).
     */
    suspend fun getAllUsers(): List<User> {
        val remote = authApi.getAllUsers()
        userDao.upsertUsers(remote.map { it.toUserEntity() })
        return remote.map { it.toDomainUser() }
    }

    /**
     * Update user profile (birth date, gender, extended anagrafica).
     */
    suspend fun updateProfile(
        username: String,
        birthDate: String?,
        gender: String?,
        profileCompleted: Boolean,
        firstName: String? = null,
        lastName: String? = null,
        fiscalCode: String? = null,
        phone: String? = null,
        street: String? = null,
        streetNumber: String? = null,
        city: String? = null,
        postalCode: String? = null
    ): Result<Unit> {
        return try {
            authApi.updateProfile(
                username, birthDate, gender, profileCompleted,
                firstName, lastName, fiscalCode, phone, street, streetNumber, city, postalCode
            )
            // Update local cache
            val cached = userDao.getUser(username)
            if (cached != null) {
                userDao.upsertUser(
                    cached.copy(
                        birthDate = birthDate,
                        gender = gender,
                        profileCompleted = profileCompleted,
                        firstName = firstName,
                        lastName = lastName,
                        fiscalCode = fiscalCode,
                        phone = phone,
                        street = street,
                        streetNumber = streetNumber,
                        city = city,
                        postalCode = postalCode
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Change password — verifies the current password first (like the web app).
     */
    suspend fun changePassword(username: String, currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val currentHash = PasswordHasher.hash(currentPassword)
            val user = authApi.login(username, currentHash)
            if (user == null) return Result.failure(AuthError.InvalidCredentials())
            val newHash = PasswordHasher.hash(newPassword)
            authApi.changePassword(username, newHash)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user email.
     */
    suspend fun changeEmail(username: String, newEmail: String): Result<Unit> {
        return try {
            authApi.updateEmail(username, newEmail)
            val cached = userDao.getUser(username)
            if (cached != null) {
                userDao.upsertUser(cached.copy(email = newEmail))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Request password reset via email.
     */
    suspend fun requestPasswordReset(email: String, resetBaseUrl: String): Result<String?> {
        return try {
            val response = authApi.requestPasswordReset(email, resetBaseUrl)
            Result.success(response.resetUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Complete password recovery with token.
     */
    suspend fun completePasswordRecovery(token: String, newPassword: String): Result<Unit> {
        return try {
            authApi.completePasswordRecovery(token, newPassword)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: reset another user's password.
     */
    suspend fun adminResetPassword(adminUsername: String, targetUsername: String, newPassword: String): Result<Unit> {
        return try {
            val hash = PasswordHasher.hash(newPassword)
            authApi.adminResetPassword(targetUsername, hash)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: create a new user.
     */
    suspend fun createUser(username: String, email: String, password: String, role: String): Result<User> {
        return try {
            val hash = PasswordHasher.hash(password)
            val created = authApi.createUser(
                username = username.lowercase().trim(),
                email = email.lowercase().trim(),
                passwordHash = hash,
                role = role
            ) ?: return Result.failure(Exception("Creazione utente non riuscita"))
            userDao.upsertUser(created.toUserEntity())
            Result.success(created.toDomainUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: change a user's role.
     */
    suspend fun setUserRole(username: String, role: String): Result<Unit> {
        return try {
            authApi.setUserRole(username, role)
            val cached = userDao.getUser(username)
            if (cached != null) userDao.upsertUser(cached.copy(role = role))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: activate/deactivate a user.
     */
    suspend fun setUserActive(username: String, active: Boolean): Result<Unit> {
        return try {
            authApi.setUserActive(username, active)
            val cached = userDao.getUser(username)
            if (cached != null) userDao.upsertUser(cached.copy(active = active))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin: permanently delete a user.
     */
    suspend fun hardDeleteUser(username: String): Result<Unit> {
        return try {
            authApi.hardDeleteUser(username)
            userDao.deleteUser(username)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

sealed class AuthError(message: String) : Exception(message) {
    class InvalidCredentials : AuthError("Invalid username or password")
    class NetworkError(message: String) : AuthError(message)
}

// ── Mappers ────────────────────────────────────────────────

fun UserResponse.toUserEntity() = UserEntity(
    username = username,
    email = email,
    role = role,
    active = active,
    birthDate = birthDate,
    gender = gender,
    profileCompleted = profileCompleted,
    skipProfilePrompt = skipProfilePrompt,
    firstName = firstName,
    lastName = lastName,
    fiscalCode = fiscalCode,
    phone = phone,
    street = street,
    streetNumber = streetNumber,
    city = city,
    postalCode = postalCode
)

fun UserResponse.toDomainUser() = User(
    username = username,
    email = email,
    role = role,
    active = active,
    birthDate = birthDate,
    gender = gender,
    profileCompleted = profileCompleted,
    skipProfilePrompt = skipProfilePrompt,
    firstName = firstName,
    lastName = lastName,
    fiscalCode = fiscalCode,
    phone = phone,
    street = street,
    streetNumber = streetNumber,
    city = city,
    postalCode = postalCode
)

fun UserEntity.toDomainUser() = User(
    username = username,
    email = email,
    role = role,
    active = active,
    birthDate = birthDate,
    gender = gender,
    profileCompleted = profileCompleted,
    skipProfilePrompt = skipProfilePrompt,
    firstName = firstName,
    lastName = lastName,
    fiscalCode = fiscalCode,
    phone = phone,
    street = street,
    streetNumber = streetNumber,
    city = city,
    postalCode = postalCode
)
