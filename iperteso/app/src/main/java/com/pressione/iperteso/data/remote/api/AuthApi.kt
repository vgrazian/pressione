package com.pressione.iperteso.data.remote.api

import android.util.Log
import com.pressione.iperteso.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class AuthApi {
    private val client = SupabaseClientProvider.client

    suspend fun login(username: String, passwordHash: String): UserResponse? {
        Log.d("IperTeso/Auth", "login() called for user='$username'")
        return try {
            val result = client.from("users")
                .select {
                    filter { eq("username", username) }
                }
                .decodeList<UserResponse>()
            Log.d("IperTeso/Auth", "Supabase returned ${result.size} users")

            val user = result.firstOrNull { it.active }
            if (user == null) {
                Log.w("IperTeso/Auth", "User not found or inactive: $username")
                return null
            }

            val hashOk = user.passwordHash == passwordHash
            Log.d("IperTeso/Auth", "Hash match: $hashOk")
            if (!hashOk) return null

            Log.d("IperTeso/Auth", "Login SUCCESS for $username")
            user
        } catch (e: Exception) {
            Log.e("IperTeso/Auth", "Login FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }

    suspend fun getAllUsers(): List<UserResponse> {
        return client.from("users").select().decodeList<UserResponse>()
    }

    suspend fun updateProfile(username: String, birthDate: String?, gender: String?, profileCompleted: Boolean) {
        client.from("users").update({
            set("birth_date", birthDate); set("gender", gender); set("profile_completed", profileCompleted)
        }) { filter { eq("username", username) } }
    }

    suspend fun changePassword(username: String, newPasswordHash: String) {
        client.from("users").update({ set("password_hash", newPasswordHash) }) { filter { eq("username", username) } }
    }

    suspend fun updateEmail(username: String, newEmail: String) {
        client.from("update_user_email").select()
    }

    suspend fun requestPasswordReset(email: String, resetBaseUrl: String): ResetResponse {
        return ResetResponse(email = email, resetUrl = null, expiresAt = null)
    }

    suspend fun completePasswordRecovery(token: String, newPassword: String) { }
    suspend fun adminResetPassword(adminUsername: String, targetUsername: String, newPasswordHash: String) { }
}

@Serializable
data class UserResponse(
    val username: String, val email: String,
    @SerialName("password_hash") val passwordHash: String,
    val role: String = "user", val active: Boolean = true,
    @SerialName("birth_date") val birthDate: String? = null,
    val gender: String? = null,
    @SerialName("profile_completed") val profileCompleted: Boolean = false,
    @SerialName("skip_profile_prompt") val skipProfilePrompt: Boolean = false
)

@Serializable
data class ResetResponse(
    val email: String,
    @SerialName("reset_url") val resetUrl: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
)
