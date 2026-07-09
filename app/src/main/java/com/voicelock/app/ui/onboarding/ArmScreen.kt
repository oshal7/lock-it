package com.voicelock.app.ui.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.voicelock.app.R
import com.voicelock.app.data.AppPreferences
import com.voicelock.app.service.ListeningService
import com.voicelock.app.service.WakeWordFiles

@Composable
fun ArmScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    val ready = WakeWordFiles.hasKeywordFile(context) &&
        prefs.picovoiceAccessKey.isNotBlank() &&
        prefs.hasCredential

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_arm_title),
        body = if (ready) stringResource(R.string.onboarding_arm_body) else stringResource(R.string.home_setup_incomplete),
        primaryLabel = stringResource(R.string.onboarding_arm_now),
        primaryEnabled = ready,
        onPrimary = {
            prefs.listeningArmed = true
            ListeningService.start(context)
            onFinish()
        },
        extraContent = if (!ready) {
            { Text("Go back and finish importing your trigger phrase and setting a passcode.", color = MaterialTheme.colorScheme.error) }
        } else null
    )
}
