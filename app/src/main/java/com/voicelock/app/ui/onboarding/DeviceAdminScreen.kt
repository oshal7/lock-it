package com.voicelock.app.ui.onboarding

import android.app.admin.DevicePolicyManager
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
import com.voicelock.app.admin.LockController

@Composable
fun DeviceAdminScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    var active by remember { mutableStateOf(LockController.isAdminActive(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        active = LockController.isAdminActive(context)
    }

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_admin_title),
        body = stringResource(R.string.onboarding_admin_body),
        primaryLabel = if (active) stringResource(R.string.action_next) else stringResource(R.string.onboarding_activate_admin),
        onPrimary = {
            if (active) {
                onNext()
            } else {
                val intent = android.content.Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, LockController.adminComponent(context))
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        context.getString(R.string.onboarding_admin_body)
                    )
                }
                launcher.launch(intent)
            }
        }
    )
}
