package com.voicelock.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.voicelock.app.R
import com.voicelock.app.data.AppPreferences

@Composable
fun SetCredentialScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var credentialType by remember { mutableStateOf(AppPreferences.CredentialType.PASSCODE) }
    var passcode by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var recoveryQuestion by remember { mutableStateOf("") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    val validLength = passcode.length in 4..8
    val canProceed = validLength && passcode == confirm

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_credential_title),
        body = stringResource(R.string.onboarding_credential_body),
        primaryLabel = stringResource(R.string.action_next),
        primaryEnabled = true,
        onPrimary = {
            if (!canProceed) {
                mismatch = true
            } else {
                prefs.credentialType = credentialType
                prefs.setCredential(passcode)
                if (recoveryQuestion.isNotBlank() && recoveryAnswer.isNotBlank()) {
                    prefs.recoveryQuestion = recoveryQuestion
                    prefs.setRecoveryAnswer(recoveryAnswer)
                }
                onNext()
            }
        },
        extraContent = {
            Column {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = credentialType == AppPreferences.CredentialType.PASSCODE,
                        onClick = { credentialType = AppPreferences.CredentialType.PASSCODE },
                        label = { Text("Passcode") }
                    )
                    FilterChip(
                        selected = credentialType == AppPreferences.CredentialType.PATTERN,
                        onClick = { credentialType = AppPreferences.CredentialType.PATTERN },
                        label = { Text("Pattern") }
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { if (it.length <= 8) passcode = it.filter(Char::isDigit) },
                    label = { Text("New VoiceLock passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 8) confirm = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.onboarding_credential_confirm)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (mismatch && !canProceed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.onboarding_credential_mismatch),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.onboarding_recovery_question_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.onboarding_recovery_question_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = recoveryQuestion,
                    onValueChange = { recoveryQuestion = it },
                    label = { Text("Recovery question") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = recoveryAnswer,
                    onValueChange = { recoveryAnswer = it },
                    label = { Text("Answer") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
