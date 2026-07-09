package com.voicelock.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialHasherTest {

    @Test
    fun `correct credential matches its own hash`() {
        val hashed = CredentialHasher.hash("1234")
        assertTrue(CredentialHasher.matches("1234", hashed.hash, hashed.salt))
    }

    @Test
    fun `wrong credential does not match`() {
        val hashed = CredentialHasher.hash("1234")
        assertFalse(CredentialHasher.matches("4321", hashed.hash, hashed.salt))
    }

    @Test
    fun `same credential hashed twice yields different salts and hashes`() {
        val first = CredentialHasher.hash("lock lock lock")
        val second = CredentialHasher.hash("lock lock lock")
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
    }

    @Test
    fun `hash from one salt does not match against a different salt`() {
        val a = CredentialHasher.hash("1234")
        val b = CredentialHasher.hash("1234")
        assertFalse(CredentialHasher.matches("1234", a.hash, b.salt))
    }
}
