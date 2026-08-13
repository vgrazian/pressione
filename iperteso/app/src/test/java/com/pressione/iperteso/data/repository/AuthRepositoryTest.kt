package com.pressione.iperteso.data.repository

import com.pressione.iperteso.data.local.dao.ReadingDao
import com.pressione.iperteso.data.local.dao.UserDao
import com.pressione.iperteso.data.local.entity.ReadingEntity
import com.pressione.iperteso.data.local.entity.UserEntity
import com.pressione.iperteso.data.remote.api.AuthApi
import com.pressione.iperteso.data.remote.api.ReadingsApi
import com.pressione.iperteso.data.remote.api.UserResponse
import com.pressione.iperteso.domain.model.AuthSession
import com.pressione.iperteso.util.PasswordHasher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var authApi: AuthApi
    private lateinit var userDao: UserDao
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        authApi = mockk()
        userDao = mockk(relaxed = true)
        val settingsDao = mockk<com.pressione.iperteso.data.local.dao.SettingsDao>(relaxed = true)
        repository = AuthRepository(authApi, userDao, settingsDao)
    }

    @Test
    fun `login succeeds with valid credentials`() = runTest {
        val passwordHash = PasswordHasher.hash("password123")
        val userResponse = UserResponse(
            username = "testuser",
            email = "test@example.com",
            passwordHash = passwordHash,
            role = "user",
            active = true
        )

        coEvery { authApi.login("testuser", passwordHash) } returns userResponse
        coEvery { userDao.upsertUser(any()) } returns Unit

        val result = repository.login("testuser", "password123")

        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertEquals("testuser", session?.username)
        assertEquals("user", session?.role)
    }

    @Test
    fun `login fails with invalid password`() = runTest {
        val correctHash = PasswordHasher.hash("correctPassword")
        val wrongHash = PasswordHasher.hash("wrongPassword")
        val userResponse = UserResponse(
            username = "testuser",
            email = "test@example.com",
            passwordHash = correctHash,
            role = "user",
            active = true
        )

        // AuthApi returns user only for the correct hash
        coEvery { authApi.login("testuser", correctHash) } returns userResponse
        coEvery { authApi.login("testuser", wrongHash) } returns null

        val result = repository.login("testuser", "wrongPassword")

        assertTrue("Login should fail when AuthApi returns null", result.isFailure)
    }

    @Test
    fun `login fails when user not found`() = runTest {
        coEvery {
            authApi.login("unknown", PasswordHasher.hash("password"))
        } returns null

        val result = repository.login("unknown", "password")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getUserProfile returns cached user`() = runTest {
        val cached = UserEntity(
            username = "testuser",
            email = "test@example.com",
            role = "user",
            active = true
        )
        coEvery { userDao.getUser("testuser") } returns cached

        val user = repository.getUserProfile("testuser")

        assertEquals("testuser", user?.username)
    }

    @Test
    fun `getAllUsers fetches and caches`() = runTest {
        val responses = listOf(
            UserResponse("user1", "u1@example.com", "hash1", "user", true),
            UserResponse("admin1", "a1@example.com", "hash2", "admin", true)
        )
        coEvery { authApi.getAllUsers() } returns responses
        coEvery { userDao.upsertUsers(any()) } returns Unit

        val users = repository.getAllUsers()

        assertEquals(2, users.size)
        assertEquals("user1", users[0].username)
        assertEquals("admin1", users[1].username)
    }
}
