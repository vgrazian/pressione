package com.pressione.iperteso.ui.screens.auth

import com.pressione.iperteso.data.repository.AuthError
import com.pressione.iperteso.data.repository.AuthRepository
import com.pressione.iperteso.domain.model.AuthSession
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not logged in`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertNull(state.session)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `login success sets session`() = runTest {
        val session = AuthSession("test", "user", "test@example.com")
        coEvery { authRepository.login("test", "password") } returns Result.success(session)

        viewModel.login("test", "password")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertNotNull(state.session)
        assertEquals("test", state.session?.username)
        assertEquals("user", state.session?.role)
        assertNull(state.error)
    }

    @Test
    fun `login failure sets error`() = runTest {
        coEvery { authRepository.login("bad", "wrong") } returns
            Result.failure(AuthError.InvalidCredentials())

        viewModel.login("bad", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("non validi"))
    }

    @Test
    fun `login sets loading state`() = runTest {
        coEvery { authRepository.login("test", "pass") } returns
            Result.success(AuthSession("test", "user", "test@example.com"))

        viewModel.login("test", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        // After coroutine completes, loading should be false (success)
        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertFalse(state.isLoading)
    }

    @Test
    fun `logout clears state`() = runTest {
        // First login
        val session = AuthSession("test", "user", "test@example.com")
        coEvery { authRepository.login("test", "pass") } returns Result.success(session)
        viewModel.login("test", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then logout
        viewModel.logout()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertNull(state.session)
    }

    @Test
    fun `clearError removes error message`() = runTest {
        coEvery { authRepository.login("bad", "wrong") } returns
            Result.failure(AuthError.InvalidCredentials())
        viewModel.login("bad", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }
}
