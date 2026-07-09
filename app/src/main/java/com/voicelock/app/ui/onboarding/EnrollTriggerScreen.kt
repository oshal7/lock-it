package com.voicelock.app.ui.onboarding

import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voicelock.app.BuildConfig
import com.voicelock.app.R
import com.voicelock.app.data.AppPreferences
import com.voicelock.app.service.WakeWordFiles

private enum class TestState { IDLE, LISTENING, DETECTED, ERROR }

@Composable
fun EnrollTriggerScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var accessKey by remember {
        mutableStateOf(prefs.picovoiceAccessKey.ifBlank { BuildConfig.PICOVOICE_ACCESS_KEY })
    }
    var keywordImported by remember { mutableStateOf(WakeWordFiles.hasKeywordFile(context)) }
    var testState by remember { mutableStateOf(TestState.IDLE) }
    var testEngine by remember { mutableStateOf<PorcupineManager?>(null) }

    fun stopTest() {
        try {
            testEngine?.stop()
            testEngine?.delete()
        } catch (_: PorcupineException) {
            // Best-effort teardown.
        }
        testEngine = null
        if (testState == TestState.LISTENING) testState = TestState.IDLE
    }

    DisposableEffect(Unit) {
        onDispose { stopTest() }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && WakeWordFiles.importKeywordFile(context, uri)) {
            keywordImported = true
            prefs.keywordEnrolled = true
        }
    }

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_enroll_title),
        body = stringResource(R.string.onboarding_enroll_body),
        primaryLabel = stringResource(R.string.action_next),
        primaryEnabled = keywordImported && accessKey.isNotBlank(),
        onPrimary = {
            prefs.picovoiceAccessKey = accessKey
            onNext()
        },
        extraContent = {
            Column {
                OutlinedTextField(
                    value = accessKey,
                    onValueChange = { accessKey = it },
                    label = { Text("Picovoice AccessKey") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (keywordImported) "Trigger phrase file imported ✓" else "Import trigger phrase (.ppn) file")
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        if (testState == TestState.LISTENING) {
                            stopTest()
                        } else if (keywordImported && accessKey.isNotBlank()) {
                            prefs.picovoiceAccessKey = accessKey
                            try {
                                testEngine = PorcupineManager.Builder()
                                    .setAccessKey(accessKey)
                                    .setKeywordPath(WakeWordFiles.keywordFile(context).absolutePath)
                                    .setSensitivity(prefs.sensitivity)
                                    .setErrorCallback { testState = TestState.ERROR }
                                    .build(
                                        context.applicationContext,
                                        PorcupineManagerCallback { testState = TestState.DETECTED }
                                    )
                                testEngine?.start()
                                testState = TestState.LISTENING
                            } catch (e: PorcupineException) {
                                testState = TestState.ERROR
                            }
                        }
                    },
                    enabled = keywordImported && accessKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (testState) {
                            TestState.LISTENING -> "Listening… say your phrase (tap to stop)"
                            TestState.DETECTED -> "Detected! Tap to test again"
                            TestState.ERROR -> "Couldn't start — check AccessKey. Tap to retry"
                            TestState.IDLE -> stringResource(R.string.onboarding_test_detection)
                        }
                    )
                }
                if (testState == TestState.DETECTED) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Nice — detection is working.",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}
