package com.voicelock.app.data

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 hashing for the VoiceLock passcode/pattern and the recovery answer. Only the
 * hash + salt are ever persisted (in SecurePrefs) — the raw credential never is.
 */
object CredentialHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val SALT_BYTES = 16

    data class Hashed(val hash: String, val salt: String)

    fun hash(credential: String): Hashed {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = derive(credential, salt)
        return Hashed(
            hash = Base64.getEncoder().encodeToString(digest),
            salt = Base64.getEncoder().encodeToString(salt)
        )
    }

    fun matches(credential: String, storedHash: String, storedSalt: String): Boolean {
        val salt = Base64.getDecoder().decode(storedSalt)
        val candidate = derive(credential, salt)
        val expected = Base64.getDecoder().decode(storedHash)
        return constantTimeEquals(candidate, expected)
    }

    private fun derive(credential: String, salt: ByteArray): ByteArray {
        val spec: KeySpec = PBEKeySpec(credential.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
