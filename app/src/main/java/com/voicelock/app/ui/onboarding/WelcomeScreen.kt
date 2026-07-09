package com.voicelock.app.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.voicelock.app.R

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    OnboardingScaffold(
        title = stringResource(R.string.onboarding_welcome_title),
        body = stringResource(R.string.onboarding_welcome_body),
        primaryLabel = stringResource(R.string.onboarding_get_started),
        onPrimary = onNext
    )
}
