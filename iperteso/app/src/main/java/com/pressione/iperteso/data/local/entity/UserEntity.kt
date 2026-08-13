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
    val skipProfilePrompt: Boolean = false,

    @ColumnInfo(name = "first_name")
    val firstName: String? = null,

    @ColumnInfo(name = "last_name")
    val lastName: String? = null,

    @ColumnInfo(name = "fiscal_code")
    val fiscalCode: String? = null,

    @ColumnInfo(name = "phone")
    val phone: String? = null,

    @ColumnInfo(name = "street")
    val street: String? = null,

    @ColumnInfo(name = "street_number")
    val streetNumber: String? = null,

    @ColumnInfo(name = "city")
    val city: String? = null,

    @ColumnInfo(name = "postal_code")
    val postalCode: String? = null
)
