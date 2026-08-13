package com.pressione.iperteso.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pressione.iperteso.data.local.entity.UserEntity

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUser(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND active = 1")
    suspend fun getActiveUser(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY username ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUser(username: String)
}
