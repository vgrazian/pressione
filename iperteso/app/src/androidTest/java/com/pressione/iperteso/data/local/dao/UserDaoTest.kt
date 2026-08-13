package com.pressione.iperteso.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pressione.iperteso.data.local.AppDatabase
import com.pressione.iperteso.data.local.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveUser() = runTest {
        val user = UserEntity(
            username = "testuser",
            email = "test@example.com",
            role = "user",
            active = true,
            birthDate = "1990-01-15",
            gender = "male",
            profileCompleted = true
        )
        userDao.upsertUser(user)

        val result = userDao.getUser("testuser")
        assertNotNull(result)
        assertEquals("test@example.com", result?.email)
        assertEquals("male", result?.gender)
        assertEquals("1990-01-15", result?.birthDate)
    }

    @Test
    fun upsertReplacesExisting() = runTest {
        userDao.upsertUser(UserEntity("user1", "old@example.com", "user", true))
        userDao.upsertUser(UserEntity("user1", "new@example.com", "admin", true))

        val result = userDao.getUser("user1")
        assertEquals("new@example.com", result?.email)
        assertEquals("admin", result?.role)
    }

    @Test
    fun getActiveUserFiltersInactive() = runTest {
        userDao.upsertUser(UserEntity("active", "a@example.com", "user", true))
        userDao.upsertUser(UserEntity("inactive", "i@example.com", "user", false))

        val active = userDao.getActiveUser("active")
        assertNotNull(active)

        val inactive = userDao.getActiveUser("inactive")
        assertNull(inactive)
    }

    @Test
    fun getAllUsersReturnsAll() = runTest {
        userDao.upsertUser(UserEntity("user1", "u1@example.com", "user", true))
        userDao.upsertUser(UserEntity("user2", "u2@example.com", "user", true))
        userDao.upsertUser(UserEntity("admin", "a@example.com", "admin", true))

        val users = userDao.getAllUsers()
        assertEquals(3, users.size)
    }

    @Test
    fun deleteUserRemovesRecord() = runTest {
        userDao.upsertUser(UserEntity("temp", "temp@example.com", "user", true))
        assertNotNull(userDao.getUser("temp"))

        userDao.deleteUser("temp")

        assertNull(userDao.getUser("temp"))
    }
}
