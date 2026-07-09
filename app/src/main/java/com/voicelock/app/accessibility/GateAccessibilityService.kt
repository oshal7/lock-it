package com.voicelock.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.view.accessibility.AccessibilityEvent
import com.voicelock.app.data.AppPreferences
import com.voicelock.app.gate.GateActivity

/**
 * FR-3: watches for the phone being unlocked (system PIN passed) and, if the hardened flag is
 * set, throws the VoiceLock gate up immediately — before whatever was on screen, or the home
 * launcher, is interactable.
 *
 * Two independent triggers, per PRD §10's "accessibility service can't preempt fast app
 * launches" risk:
 *  1. ACTION_USER_PRESENT broadcast — fires right as the keyguard is dismissed.
 *  2. TYPE_WINDOW_STATE_CHANGED accessibility events — catches whatever window comes to the
 *     foreground immediately after, in case something briefly beat the gate to the screen.
 */
class GateAccessibilityService : AccessibilityService() {

    private lateinit var prefs: AppPreferences
    private var receiverRegistered = false

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                showGateIfHardened()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = AppPreferences(this)
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                userPresentReceiver,
                IntentFilter(Intent.ACTION_USER_PRESENT),
                ContextCompat.RECEIVER_EXPORTED
            )
            receiverRegistered = true
        }
        showGateIfHardened()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            showGateIfHardened()
        }
    }

    private fun showGateIfHardened() {
        if (!::prefs.isInitialized) return
        if (prefs.hardened) {
            GateActivity.launch(applicationContext)
        }
    }

    override fun onInterrupt() {
        // No cleanup needed; showGateIfHardened() is idempotent and re-runs on next event.
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(userPresentReceiver)
            receiverRegistered = false
        }
        super.onDestroy()
    }
}
