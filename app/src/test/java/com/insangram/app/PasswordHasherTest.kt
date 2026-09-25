package com.insangram.app

import com.insangram.app.core.security.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Demo passwords are stored as PBKDF2 hashes only. These tests assert that no
 * plaintext survives and that verification behaves correctly.
 */
class PasswordHasherTest {

    private val hasher = PasswordHasher()

    @Test
    fun `hashing the same password twice yields different hashes`() {
        val first = hasher.hash("Demo@12345".toCharArray())
        val second = hasher.hash("Demo@12345".toCharArray())
        assertNotEquals(first.hash, second.hash)
        assertNotEquals(first.salt, second.salt)
    }

    @Test
    fun `the correct password verifies and an incorrect one does not`() {
        val hashed = hasher.hash("Demo@12345".toCharArray())
        assertTrue(hasher.verify("Demo@12345".toCharArray(), hashed))
        assertFalse(hasher.verify("Demo@54321".toCharArray(), hashed))
    }

    @Test
    fun `the stored hash never contains the plaintext`() {
        val hashed = hasher.hash("Demo@12345".toCharArray())
        assertFalse(hashed.hash.contains("Demo@12345"))
        assertFalse(hashed.salt.contains("Demo@12345"))
    }
}
