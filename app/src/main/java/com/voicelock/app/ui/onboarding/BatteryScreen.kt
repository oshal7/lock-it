package com.voicelock.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.voicelock.app.R

@Composable
fun BatteryScreen(onNext: () -> Unit) {
    val context = LocalContext.current

    fun isExempt(): Boolean {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    var exempt by remember { mutableStateOf(isExempt()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        exempt = isExempt()
    }

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_battery_title),
        body = stringResource(R.string.onboarding_battery_body),
        primaryLabel = if (exempt) stringResource(R.string.action_next) else stringResource(R.string.onboarding_request_exemption),
        onPrimary = {
            if (exempt) {
                onNext()
            } else {
                val direct = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
                try {
                    launcher.launch(direct)
                } catch (_: android.content.ActivityNotFoundException) {
                    launcher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }
    )
}
