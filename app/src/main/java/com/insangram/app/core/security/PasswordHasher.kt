package com.insangram.app.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class HashedPassword(val hash: String, val salt: String, val iterations: Int)

/**
 * PBKDF2-HMAC-SHA256 hashing for DEMO-MODE ACCOUNTS ONLY.
 *
 * Online mode never touches this class - Firebase Authentication owns real
 * credentials and this app never sees or stores a real password. Demo mode
 * needs an offline sign-in check, so it stores a salted, iterated hash; the
 * plaintext is never written to Room, DataStore, or logs.
 */
@Singleton
class PasswordHasher @Inject constructor() {

    fun hash(password: CharArray, iterations: Int = DEFAULT_ITERATIONS): HashedPassword {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val derived = derive(password, salt, iterations)
        return HashedPassword(
            hash = encoder.encodeToString(derived),
            salt = encoder.encodeToString(salt),
            iterations = iterations,
        )
    }

    /** Constant-time comparison so verification cannot leak timing information. */
    fun verify(password: CharArray, stored: HashedPassword): Boolean {
        val salt = runCatching { decoder.decode(stored.salt) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(stored.hash) }.getOrNull() ?: return false
        val actual = derive(password, salt, stored.iterations)
        if (actual.size != expected.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor expected[i].toInt())
        return diff == 0
    }

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 120_000
        const val SALT_BYTES = 16
        const val KEY_LENGTH_BITS = 256
        val encoder: Base64.Encoder = Base64.getEncoder()
        val decoder: Base64.Decoder = Base64.getDecoder()
    }
}
