package com.pressione.iperteso.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `hash produces consistent output`() {
        val hash1 = PasswordHasher.hash("test123")
        val hash2 = PasswordHasher.hash("test123")
        assertEquals(hash1, hash2)
    }

    @Test
    fun `hash produces 64-char hex string`() {
        val hash = PasswordHasher.hash("password")
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[0-9a-f]+$")))
    }

    @Test
    fun `different passwords produce different hashes`() {
        val hash1 = PasswordHasher.hash("password1")
        val hash2 = PasswordHasher.hash("password2")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `verify succeeds with correct password`() {
        val hash = PasswordHasher.hash("mySecret123")
        assertTrue(PasswordHasher.verify("mySecret123", hash))
    }

    @Test
    fun `verify fails with wrong password`() {
        val hash = PasswordHasher.hash("mySecret123")
        assertFalse(PasswordHasher.verify("wrongPassword", hash))
    }

    @Test
    fun `verify is case sensitive`() {
        val hash = PasswordHasher.hash("Password")
        assertFalse(PasswordHasher.verify("password", hash))
    }

    @Test
    fun `empty password produces valid hash`() {
        val hash = PasswordHasher.hash("")
        assertEquals(64, hash.length)
    }

    @Test
    fun `unicode password hashing`() {
        val hash = PasswordHasher.hash("caffè123!")
        assertEquals(64, hash.length)
    }
}
