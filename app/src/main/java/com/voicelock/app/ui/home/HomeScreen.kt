package com.voicelock.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.voicelock.app.R
import com.voicelock.app.admin.LockController
import com.voicelock.app.data.AppPreferences
import com.voicelock.app.service.ListeningService
import com.voicelock.app.service.WakeWordFiles
import com.voicelock.app.util.isGateAccessibilityServiceEnabled

@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var armed by remember { mutableStateOf(prefs.listeningArmed) }
    var refreshTick by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Keyed on refreshTick so these re-check on every return from Settings, not just once.
    val micGranted = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val adminActive = remember(refreshTick) { LockController.isAdminActive(context) }
    val accessibilityEnabled = remember(refreshTick) { isGateAccessibilityServiceEnabled(context) }
    val batteryExempt = remember(refreshTick) {
        (context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)
    }
    val ready = remember(refreshTick) {
        WakeWordFiles.hasKeywordFile(context) && prefs.picovoiceAccessKey.isNotBlank() && prefs.hasCredential
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (armed) stringResource(R.string.home_status_armed) else stringResource(R.string.home_status_disarmed),
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (!ready) {
                        Text(
                            stringResource(R.string.home_setup_incomplete),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Switch(
                    checked = armed,
                    enabled = ready,
                    onCheckedChange = { checked ->
                        armed = checked
                        prefs.listeningArmed = checked
                        if (checked) ListeningService.start(context) else ListeningService.stop(context)
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Health check", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        StatusRow("Microphone permission", micGranted) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            )
        }
        StatusRow("Device admin", adminActive) {
            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
        StatusRow("Accessibility gate service", accessibilityEnabled) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        StatusRow("Battery optimization exemption", batteryExempt) {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = { LockController.triggerLock(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.home_lock_now))
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(stringResource(R.string.home_settings), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, onFix: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.height(20.dp)
            )
            Text(label, modifier = Modifier.padding(start = 8.dp))
        }
        if (!ok) {
            TextButton(onClick = onFix) { Text("Fix") }
        }
    }
}
