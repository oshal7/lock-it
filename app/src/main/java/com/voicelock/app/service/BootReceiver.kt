package com.voicelock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voicelock.app.data.AppPreferences

/**
 * FR-4: "start listening on boot". Also fires on MY_PACKAGE_REPLACED so a fresh install/update
 * from a CI build resumes listening automatically if it was armed before the update — matching
 * the "update in place, keep working" expectation from an app the user re-installs often while
 * testing new builds.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val prefs = AppPreferences(context)
        if (prefs.autoStartOnBoot && prefs.listeningArmed && prefs.onboardingComplete) {
            ListeningService.start(context)
        }
        ServiceWatchdogWorker.schedule(context)
    }
}
