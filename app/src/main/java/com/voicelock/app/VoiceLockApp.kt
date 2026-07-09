package com.voicelock.app

import android.app.Application
import com.voicelock.app.service.ServiceWatchdogWorker

class VoiceLockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceWatchdogWorker.schedule(this)
    }
}
