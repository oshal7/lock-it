package com.voicelock.app.gate

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.voicelock.app.admin.LockController
import com.voicelock.app.data.AppPreferences
import com.voicelock.app.ui.gate.GateAttemptResult
import com.voicelock.app.ui.gate.GateScreen
import com.voicelock.app.ui.theme.VoiceLockGateTheme

/**
 * FR-3: the kiosk gate. Sits on top of everything once the hardened flag is set — nothing
 * behind it (notification content, other apps, the launcher) is meant to be visible or
 * reachable until the VoiceLock credential is entered correctly.
 */
class GateActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences
    private val relaunchHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        if (!prefs.hardened) {
            finish()
            return
        }

        applyLockScreenFlags()
        hideSystemBars()
        tryEnterLockTask()

        // Back navigation must not dismiss the gate.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Intentionally empty.
            }
        })

        setContent {
            VoiceLockGateTheme {
                GateScreen(
                    credentialType = prefs.credentialType,
                    onUnlockAttempt = { candidate -> attemptUnlock(candidate) }
                )
            }
        }
    }

    private fun applyLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun tryEnterLockTask() {
        try {
            startLockTask()
        } catch (_: SecurityException) {
            // Screen pinning consent dialog was dismissed or is otherwise unavailable on this
            // OEM build — the full-screen overlay + hidden system bars remain as a fallback
            // (PRD §10: "Android 14+ restrictions on lock task without device-owner").
        }
    }

    private fun attemptUnlock(candidate: String): GateAttemptResult {
        val now = System.currentTimeMillis()
        if (now < prefs.gateLockoutUntil) {
            return GateAttemptResult(success = false, lockedUntil = prefs.gateLockoutUntil)
        }

        val success = prefs.verifyCredential(candidate)
        if (success) {
            prefs.failedGateAttempts = 0
            prefs.gateLockoutUntil = 0
            LockController.clearHardened(this)
            try {
                stopLockTask()
            } catch (_: IllegalStateException) {
                // Wasn't in lock task mode (e.g. pinning consent was declined) — fine.
            }
            finish()
            return GateAttemptResult(success = true, lockedUntil = 0)
        }

        val attempts = prefs.failedGateAttempts + 1
        prefs.failedGateAttempts = attempts
        var lockedUntil = 0L
        if (attempts >= FAILED_ATTEMPTS_BEFORE_COOLDOWN) {
            val cooldownIndex = attempts - FAILED_ATTEMPTS_BEFORE_COOLDOWN
            val cooldownMs = BASE_COOLDOWN_MS * (1L shl cooldownIndex.coerceAtMost(MAX_COOLDOWN_SHIFT))
            lockedUntil = now + cooldownMs
            prefs.gateLockoutUntil = lockedUntil
        }
        return GateAttemptResult(success = false, lockedUntil = lockedUntil)
    }

    override fun onPause() {
        super.onPause()
        // Self-heal: if something managed to push this activity to the background while still
        // hardened (a fast app launch race, an OEM gesture that bypasses pinning), immediately
        // bring the gate back rather than leaving the phone briefly usable.
        if (!isFinishing && prefs.hardened) {
            relaunchHandler.postDelayed({
                if (prefs.hardened) {
                    launch(applicationContext)
                }
            }, RELAUNCH_DELAY_MS)
        }
    }

    override fun onDestroy() {
        relaunchHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val FAILED_ATTEMPTS_BEFORE_COOLDOWN = 5
        private const val BASE_COOLDOWN_MS = 30_000L
        private const val MAX_COOLDOWN_SHIFT = 4
        private const val RELAUNCH_DELAY_MS = 250L

        fun launch(context: Context) {
            val intent = Intent(context, GateActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }
    }
}
