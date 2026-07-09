package com.voicelock.app.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.voicelock.app.data.AppPreferences
import java.util.concurrent.TimeUnit

/**
 * Mitigates the "OEM battery killer stops the service" risk (PRD §10): periodically makes sure
 * the foreground listening service is actually running whenever it's supposed to be armed.
 * Restarting an already-running service is a harmless no-op (ListeningService just re-enters
 * onStartCommand), so this needs no cross-process "is it alive" check.
 */
class ServiceWatchdogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        if (prefs.listeningArmed && prefs.onboardingComplete) {
            ListeningService.start(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "voicelock_service_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
