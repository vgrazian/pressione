package com.pressione.iperteso.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached user data (for offline profile display).
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "role")
    val role: String = "user",

    @ColumnInfo(name = "active")
    val active: Boolean = true,

    @ColumnInfo(name = "birth_date")
    val birthDate: String? = null,

    @ColumnInfo(name = "gender")
    val gender: String? = null,

    @ColumnInfo(name = "profile_completed")
    val profileCompleted: Boolean = false,

    @ColumnInfo(name = "skip_profile_prompt")
    val skipProfilePrompt: Boolean = false
)
