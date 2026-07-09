package com.voicelock.app.data

import android.content.Context
import androidx.core.content.edit

/**
 * Typed façade over [SecurePrefs] for every piece of VoiceLock state: onboarding progress,
 * arm/hardened flags, settings, and hashed credentials.
 */
class AppPreferences(context: Context) {
    private val prefs = SecurePrefs.get(context.applicationContext)

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETE, value) }

    var listeningArmed: Boolean
        get() = prefs.getBoolean(KEY_ARMED, false)
        set(value) = prefs.edit { putBoolean(KEY_ARMED, value) }

    /**
     * Set the instant the trigger fires; cleared only once the gate credential is entered
     * correctly. Persisted (not just in-memory) so a reboot mid-hardened-state doesn't leave
     * the phone unprotected — FR-2.
     */
    var hardened: Boolean
        get() = prefs.getBoolean(KEY_HARDENED, false)
        set(value) = prefs.edit { putBoolean(KEY_HARDENED, value) }

    var autoStartOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_START, value) }

    var postUnlockCooldownEnabled: Boolean
        get() = prefs.getBoolean(KEY_POST_UNLOCK_COOLDOWN, true)
        set(value) = prefs.edit { putBoolean(KEY_POST_UNLOCK_COOLDOWN, value) }

    /** Epoch millis until which the listening service should ignore audio (calls, cooldown). */
    var listeningPausedUntil: Long
        get() = prefs.getLong(KEY_LISTENING_PAUSED_UNTIL, 0L)
        set(value) = prefs.edit { putLong(KEY_LISTENING_PAUSED_UNTIL, value) }

    /** 0f (least sensitive) .. 1f (most sensitive); maps to the Porcupine detection threshold. */
    var sensitivity: Float
        get() = prefs.getFloat(KEY_SENSITIVITY, 0.5f)
        set(value) = prefs.edit { putFloat(KEY_SENSITIVITY, value) }

    var keywordEnrolled: Boolean
        get() = prefs.getBoolean(KEY_KEYWORD_ENROLLED, false)
        set(value) = prefs.edit { putBoolean(KEY_KEYWORD_ENROLLED, value) }

    /**
     * Picovoice Console AccessKey (free personal tier). Pre-filled from BuildConfig if CI baked
     * one in via the PICOVOICE_ACCESS_KEY secret, but always editable in-app so the phrase and
     * key can be changed without a rebuild — see README.
     */
    var picovoiceAccessKey: String
        get() = prefs.getString(KEY_PICOVOICE_ACCESS_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PICOVOICE_ACCESS_KEY, value) }

    var credentialType: CredentialType
        get() = CredentialType.valueOf(
            prefs.getString(KEY_CREDENTIAL_TYPE, CredentialType.PASSCODE.name) ?: CredentialType.PASSCODE.name
        )
        set(value) = prefs.edit { putString(KEY_CREDENTIAL_TYPE, value.name) }

    fun setCredential(rawCredential: String) {
        val hashed = CredentialHasher.hash(rawCredential)
        prefs.edit {
            putString(KEY_CREDENTIAL_HASH, hashed.hash)
            putString(KEY_CREDENTIAL_SALT, hashed.salt)
        }
    }

    fun verifyCredential(rawCredential: String): Boolean {
        val hash = prefs.getString(KEY_CREDENTIAL_HASH, null) ?: return false
        val salt = prefs.getString(KEY_CREDENTIAL_SALT, null) ?: return false
        return CredentialHasher.matches(rawCredential, hash, salt)
    }

    val hasCredential: Boolean
        get() = prefs.contains(KEY_CREDENTIAL_HASH)

    var recoveryQuestion: String?
        get() = prefs.getString(KEY_RECOVERY_QUESTION, null)
        set(value) = prefs.edit { putString(KEY_RECOVERY_QUESTION, value) }

    fun setRecoveryAnswer(rawAnswer: String) {
        val hashed = CredentialHasher.hash(normalizeAnswer(rawAnswer))
        prefs.edit {
            putString(KEY_RECOVERY_HASH, hashed.hash)
            putString(KEY_RECOVERY_SALT, hashed.salt)
        }
    }

    fun verifyRecoveryAnswer(rawAnswer: String): Boolean {
        val hash = prefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        val salt = prefs.getString(KEY_RECOVERY_SALT, null) ?: return false
        return CredentialHasher.matches(normalizeAnswer(rawAnswer), hash, salt)
    }

    val hasRecoveryAnswer: Boolean
        get() = prefs.contains(KEY_RECOVERY_HASH)

    private fun normalizeAnswer(raw: String) = raw.trim().lowercase()

    var failedGateAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit { putInt(KEY_FAILED_ATTEMPTS, value) }

    /** Epoch millis until which the gate rejects attempts outright (escalating cooldown). */
    var gateLockoutUntil: Long
        get() = prefs.getLong(KEY_GATE_LOCKOUT_UNTIL, 0L)
        set(value) = prefs.edit { putLong(KEY_GATE_LOCKOUT_UNTIL, value) }

    enum class CredentialType { PASSCODE, PATTERN }

    private companion object {
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_ARMED = "listening_armed"
        const val KEY_HARDENED = "hardened"
        const val KEY_AUTO_START = "auto_start_on_boot"
        const val KEY_POST_UNLOCK_COOLDOWN = "post_unlock_cooldown_enabled"
        const val KEY_LISTENING_PAUSED_UNTIL = "listening_paused_until"
        const val KEY_SENSITIVITY = "sensitivity"
        const val KEY_KEYWORD_ENROLLED = "keyword_enrolled"
        const val KEY_PICOVOICE_ACCESS_KEY = "picovoice_access_key"
        const val KEY_CREDENTIAL_TYPE = "credential_type"
        const val KEY_CREDENTIAL_HASH = "credential_hash"
        const val KEY_CREDENTIAL_SALT = "credential_salt"
        const val KEY_RECOVERY_QUESTION = "recovery_question"
        const val KEY_RECOVERY_HASH = "recovery_hash"
        const val KEY_RECOVERY_SALT = "recovery_salt"
        const val KEY_FAILED_ATTEMPTS = "failed_gate_attempts"
        const val KEY_GATE_LOCKOUT_UNTIL = "gate_lockout_until"
    }
}
