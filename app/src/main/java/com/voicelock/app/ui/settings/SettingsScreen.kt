package com.voicelock.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voicelock.app.data.AppPreferences
import com.voicelock.app.service.ListeningService
import com.voicelock.app.service.WakeWordFiles

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var sensitivity by remember { mutableStateOf(prefs.sensitivity) }
    var autoStart by remember { mutableStateOf(prefs.autoStartOnBoot) }
    var cooldown by remember { mutableStateOf(prefs.postUnlockCooldownEnabled) }
    var keywordImported by remember { mutableStateOf(WakeWordFiles.hasKeywordFile(context)) }
    var changingPasscode by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && WakeWordFiles.importKeywordFile(context, uri)) {
            keywordImported = true
            prefs.keywordEnrolled = true
            if (prefs.listeningArmed) {
                ListeningService.stop(context)
                ListeningService.start(context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(24.dp))

        Text("Detection sensitivity", style = MaterialTheme.typography.titleLarge)
        Text(
            "Higher sensitivity catches your phrase more reliably but may trigger on similar-sounding words.",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = sensitivity,
            onValueChange = {
                sensitivity = it
                prefs.sensitivity = it
            },
            valueRange = 0f..1f
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Trigger phrase", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (keywordImported) "Re-import trigger phrase (.ppn) file" else "Import trigger phrase (.ppn) file")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("VoiceLock credential", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (prefs.credentialType == AppPreferences.CredentialType.PASSCODE) {
            OutlinedButton(onClick = { changingPasscode = !changingPasscode }, modifier = Modifier.fillMaxWidth()) {
                Text("Change VoiceLock passcode")
            }
            if (changingPasscode) {
                Spacer(Modifier.height(8.dp))
                ChangePasscodeForm(
                    prefs = prefs,
                    onDone = { changingPasscode = false }
                )
            }
        } else {
            Text(
                "You're using a pattern credential. Changing a pattern isn't supported from " +
                    "Settings yet — reinstall and complete onboarding again to set a new one.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Start listening on boot", style = MaterialTheme.typography.titleLarge)
            }
            Switch(checked = autoStart, onCheckedChange = {
                autoStart = it
                prefs.autoStartOnBoot = it
            })
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pause briefly after a successful unlock", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Prevents an immediate re-trigger from the same phrase echoing.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(checked = cooldown, onCheckedChange = {
                cooldown = it
                prefs.postUnlockCooldownEnabled = it
            })
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("About & privacy", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "VoiceLock never requests the INTERNET permission and has no analytics, accounts, or " +
                "cloud sync. All wake-word processing happens on-device. Your VoiceLock passcode is " +
                "stored only as a salted PBKDF2 hash, never in plain text.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))
        Text("Recovery", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "If you forget your VoiceLock passcode: boot into Safe Mode (disables third-party apps, " +
                "including VoiceLock, until you reboot normally) or use the recovery question set " +
                "during onboarding. See the README for step-by-step recovery instructions.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ChangePasscodeForm(prefs: AppPreferences, onDone: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column {
        androidx.compose.material3.OutlinedTextField(
            value = current,
            onValueChange = { current = it.filter(Char::isDigit) },
            label = { Text("Current passcode") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedTextField(
            value = next,
            onValueChange = { next = it.filter(Char::isDigit) },
            label = { Text("New passcode") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it.filter(Char::isDigit) },
            label = { Text("Confirm new passcode") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                when {
                    !prefs.verifyCredential(current) -> error = "Current passcode is incorrect"
                    next.length !in 4..8 -> error = "New passcode must be 4-8 digits"
                    next != confirm -> error = "Passcodes don't match"
                    else -> {
                        prefs.setCredential(next)
                        error = null
                        onDone()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save new passcode")
        }
    }
}
