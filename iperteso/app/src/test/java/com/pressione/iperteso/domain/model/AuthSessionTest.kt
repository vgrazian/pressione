package com.pressione.iperteso.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionTest {

    @Test
    fun `session is not expired when just created`() {
        val session = AuthSession(
            username = "test",
            role = "user",
            email = "test@example.com"
        )
        assertFalse(session.isExpired)
    }

    @Test
    fun `session is admin when role is admin`() {
        val adminSession = AuthSession("admin", "admin", "admin@example.com")
        assertTrue(adminSession.isAdmin)

        val userSession = AuthSession("user", "user", "user@example.com")
        assertFalse(userSession.isAdmin)
    }

    @Test
    fun `session expires after TTL`() {
        val pastTimestamp = System.currentTimeMillis() - (481 * 60 * 1000L) // 481 minutes ago
        val session = AuthSession(
            username = "test",
            role = "user",
            email = "test@example.com",
            loginTimestamp = pastTimestamp,
            ttlMinutes = 480
        )
        assertTrue(session.isExpired)
    }

    @Test
    fun `session properties are correct`() {
        val session = AuthSession(
            username = "valerio",
            role = "admin",
            email = "valerio@example.com",
            ttlMinutes = 480
        )
        assertEquals("valerio", session.username)
        assertEquals("admin", session.role)
        assertEquals("valerio@example.com", session.email)
        assertEquals(480, session.ttlMinutes)
    }

    @Test
    fun `default TTL is 480 minutes`() {
        val session = AuthSession("user", "user", "user@example.com")
        assertEquals(480, session.ttlMinutes)
    }
}
