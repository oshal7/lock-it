package com.voicelock.app.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.voicelock.app.data.AppPreferences

/**
 * FR-2: the single place that actually locks the phone and flips the hardened flag. Everything
 * else — the listening service on trigger, the debug "lock now" button, tests — goes through
 * here so there's exactly one lock path to reason about.
 */
object LockController {

    fun adminComponent(context: Context): ComponentName =
        ComponentName(context.applicationContext, VoiceLockDeviceAdminReceiver::class.java)

    private fun devicePolicyManager(context: Context): DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isAdminActive(context: Context): Boolean =
        devicePolicyManager(context).isAdminActive(adminComponent(context))

    fun isDeviceOwner(context: Context): Boolean =
        devicePolicyManager(context).isDeviceOwnerApp(context.packageName)

    /**
     * Locks the screen immediately and marks the phone "hardened". The flag is persisted to
     * encrypted storage (not just held in memory) so GateAccessibilityService keeps demanding
     * the VoiceLock credential even if the phone reboots before it's entered.
     */
    fun triggerLock(context: Context) {
        AppPreferences(context).hardened = true
        if (isAdminActive(context)) {
            devicePolicyManager(context).lockNow()
        }
    }

    fun clearHardened(context: Context) {
        AppPreferences(context).hardened = false
    }

    /**
     * Best-effort "block uninstall". This only takes effect if VoiceLock has separately been
     * made the device owner — see README. A plain Device Admin app (the only kind that can be
     * granted on an already-set-up personal phone without a factory reset) is not permitted by
     * Android to block uninstall; this is a platform restriction, not something a signing key
     * or manifest flag can bypass. Safe to call unconditionally: no-ops when not device owner.
     */
    fun setUninstallBlocked(context: Context, blocked: Boolean) {
        if (!isDeviceOwner(context)) return
        devicePolicyManager(context).setUninstallBlocked(adminComponent(context), context.packageName, blocked)
    }
}
