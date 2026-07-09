package com.voicelock.app.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.voicelock.app.R

@Composable
fun MicPermissionScreen(onNext: () -> Unit) {
    val context = LocalContext.current

    fun micGranted() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(micGranted()) }

    val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        granted = micGranted()
    }

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_mic_title),
        body = stringResource(R.string.onboarding_mic_body),
        primaryLabel = if (granted) stringResource(R.string.action_next) else stringResource(R.string.onboarding_grant_permission),
        onPrimary = {
            if (granted) onNext() else launcher.launch(permissions.toTypedArray())
        }
    )
}
