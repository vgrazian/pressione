package com.pressione.iperteso.util

import java.security.MessageDigest

/**
 * SHA-256 password hashing — matches the web app's auth mechanism.
 */
object PasswordHasher {

    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, hash: String): Boolean {
        return hash(password) == hash
    }
}
