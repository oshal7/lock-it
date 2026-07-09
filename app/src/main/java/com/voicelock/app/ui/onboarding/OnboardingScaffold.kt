package com.voicelock.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared layout for every onboarding step: icon-free title + body copy, extra content, one CTA. */
@Composable
fun OnboardingScaffold(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    extraContent: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(text = body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(24.dp))
        extraContent?.invoke()
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPrimary,
            enabled = primaryEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(primaryLabel)
        }
        footer?.let {
            Spacer(Modifier.height(12.dp))
            it()
        }
    }
}
