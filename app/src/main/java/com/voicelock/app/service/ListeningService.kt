package com.voicelock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import com.voicelock.app.MainActivity
import com.voicelock.app.R
import com.voicelock.app.admin.LockController
import com.voicelock.app.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * FR-1: the always-on foreground service. Delegates the actual keyword spotting to Porcupine
 * (ai.picovoice:porcupine-android), which owns its own AudioRecord internally — this class is
 * mostly responsible for lifecycle, the persistent notification, and pausing capture while a
 * call is active so it doesn't fight the dialer for the mic.
 */
class ListeningService : LifecycleService() {

    private var porcupineManager: PorcupineManager? = null
    private var engineRunning = false
    private lateinit var prefs: AppPreferences
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification(paused = false))
        _isRunning.value = true

        lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                syncEngineWithCallState()
                kotlinx.coroutines.delay(CALL_STATE_POLL_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    private suspend fun syncEngineWithCallState() {
        val now = System.currentTimeMillis()
        val callActive = audioManager.mode == AudioManager.MODE_IN_CALL ||
            audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
        val cooldownActive = now < prefs.listeningPausedUntil
        val shouldRun = !callActive && !cooldownActive

        if (shouldRun && !engineRunning) {
            startEngine()
        } else if (!shouldRun && engineRunning) {
            stopEngine()
            withContext(Dispatchers.Main) {
                updateNotification(paused = true)
            }
        }
    }

    private fun startEngine() {
        val keywordFile = WakeWordFiles.keywordFile(this)
        val accessKey = prefs.picovoiceAccessKey
        if (accessKey.isBlank() || !WakeWordFiles.hasKeywordFile(this)) {
            return
        }
        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeywordPath(keywordFile.absolutePath)
                .setSensitivity(prefs.sensitivity)
                .setErrorCallback { e -> onEngineError(e) }
                .build(applicationContext, wakeWordCallback)
            porcupineManager?.start()
            engineRunning = true
            updateNotification(paused = false)
        } catch (e: PorcupineException) {
            engineRunning = false
        }
    }

    private fun stopEngine() {
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
        } catch (_: PorcupineException) {
            // Best-effort teardown; nothing meaningful to recover from here.
        }
        porcupineManager = null
        engineRunning = false
    }

    private val wakeWordCallback = PorcupineManagerCallback { _ ->
        onTriggerDetected()
    }

    private fun onTriggerDetected() {
        LockController.triggerLock(applicationContext)
        if (prefs.postUnlockCooldownEnabled) {
            prefs.listeningPausedUntil = System.currentTimeMillis() + TRIGGER_COOLDOWN_MS
        }
    }

    private fun onEngineError(e: PorcupineException) {
        engineRunning = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.listening_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(paused: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ListeningService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.listening_notification_title))
            .setContentText(
                getString(
                    if (paused) R.string.listening_notification_paused_text
                    else R.string.listening_notification_text
                )
            )
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.home_toggle_disarm), stopIntent)
            .build()
    }

    private fun updateNotification(paused: Boolean) {
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(paused))
    }

    override fun onDestroy() {
        stopEngine()
        _isRunning.value = false
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "voicelock_listening"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.voicelock.app.action.STOP_LISTENING"
        private const val CALL_STATE_POLL_INTERVAL_MS = 2000L
        private const val TRIGGER_COOLDOWN_MS = 5000L

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ListeningService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ListeningService::class.java).setAction(ACTION_STOP))
        }
    }
}
