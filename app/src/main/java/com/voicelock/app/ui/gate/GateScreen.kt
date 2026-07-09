package com.voicelock.app.ui.gate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelock.app.R
import com.voicelock.app.data.AppPreferences
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.sqrt

/** Result of one credential attempt: whether it succeeded, and — if locked out — until when. */
data class GateAttemptResult(val success: Boolean, val lockedUntil: Long)

@Composable
fun GateScreen(
    credentialType: AppPreferences.CredentialType,
    onUnlockAttempt: (String) -> GateAttemptResult
) {
    var wrongAttempt by remember { mutableStateOf(false) }
    var lockedUntil by remember { mutableStateOf(0L) }
    var remainingSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(lockedUntil) {
        while (true) {
            val remainingMs = lockedUntil - System.currentTimeMillis()
            remainingSeconds = max(0, (remainingMs / 1000L).toInt() + if (remainingMs % 1000L > 0) 1 else 0)
            if (remainingSeconds <= 0) break
            delay(500)
        }
    }

    val handleAttempt: (String) -> Boolean = { candidate ->
        val result = onUnlockAttempt(candidate)
        wrongAttempt = !result.success
        lockedUntil = result.lockedUntil
        result.success
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.gate_enter_passcode),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        val isLockedOut = remainingSeconds > 0
        Text(
            text = when {
                isLockedOut -> stringResource(R.string.gate_cooldown, remainingSeconds)
                wrongAttempt -> stringResource(R.string.gate_wrong_passcode)
                else -> ""
            },
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        when (credentialType) {
            AppPreferences.CredentialType.PASSCODE ->
                PasscodeGate(error = wrongAttempt, enabled = !isLockedOut, onSubmit = handleAttempt)
            AppPreferences.CredentialType.PATTERN ->
                PatternGate(error = wrongAttempt, enabled = !isLockedOut, onSubmit = handleAttempt)
        }
    }
}

private const val MIN_PASSCODE_LENGTH = 4
private const val MAX_PASSCODE_LENGTH = 8

@Composable
private fun PasscodeGate(error: Boolean, enabled: Boolean, onSubmit: (String) -> Boolean) {
    var digits by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PasscodeDots(length = digits.length, error = error)
        Spacer(Modifier.height(32.dp))

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "back")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.size(260.dp),
            contentPadding = PaddingValues(4.dp),
            userScrollEnabled = false
        ) {
            items(keys) { key ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        key == "back" -> IconButton(
                            enabled = enabled,
                            onClick = { if (digits.isNotEmpty()) digits = digits.dropLast(1) }
                        ) {
                            Icon(Icons.Filled.Backspace, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                        }
                        key.isEmpty() -> Unit
                        else -> KeypadButton(label = key, enabled = enabled) {
                            if (digits.length < MAX_PASSCODE_LENGTH) digits += key
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (digits.length >= MIN_PASSCODE_LENGTH) {
                    val success = onSubmit(digits)
                    if (!success) digits = ""
                }
            },
            enabled = enabled && digits.length >= MIN_PASSCODE_LENGTH,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.gate_unlock))
        }
    }
}

@Composable
private fun KeypadButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PasscodeDots(length: Int, error: Boolean) {
    Box(modifier = Modifier.height(16.dp)) {
        Text(
            text = "•".repeat(length),
            fontSize = 22.sp,
            letterSpacing = 6.sp,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
        )
    }
}

private const val MIN_PATTERN_LENGTH = 4
private const val PATTERN_NODE_COUNT = 9

@Composable
private fun PatternGate(error: Boolean, enabled: Boolean, onSubmit: (String) -> Boolean) {
    var selected by remember { mutableStateOf(listOf<Int>()) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    var nodeCenters by remember { mutableStateOf(listOf<Offset>()) }

    Box(
        modifier = Modifier
            .size(280.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        selected = listOfNotNull(nearestNode(offset, nodeCenters, radiusPx = size.width / 8f))
                        dragPosition = offset
                    },
                    onDrag = { change, _ ->
                        dragPosition = change.position
                        nearestNode(change.position, nodeCenters, radiusPx = size.width / 8f)?.let { node ->
                            if (node !in selected) selected = selected + node
                        }
                    },
                    onDragEnd = {
                        dragPosition = null
                        if (selected.size >= MIN_PATTERN_LENGTH) {
                            val success = onSubmit(selected.joinToString("-"))
                            if (!success) selected = emptyList()
                        } else {
                            selected = emptyList()
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = size.width / 3f
            val centers = (0 until PATTERN_NODE_COUNT).map { i ->
                Offset(cell * (i % 3) + cell / 2f, cell * (i / 3) + cell / 2f)
            }
            nodeCenters = centers

            val lineColor = if (error) Color(0xFFE05252) else Color(0xFF3DDC97)
            for (i in 1 until selected.size) {
                drawLine(lineColor, centers[selected[i - 1]], centers[selected[i]], strokeWidth = 8f)
            }
            dragPosition?.let { pos ->
                if (selected.isNotEmpty()) drawLine(lineColor, centers[selected.last()], pos, strokeWidth = 8f)
            }
            centers.forEachIndexed { index, center ->
                val active = index in selected
                drawCircle(
                    color = if (active) lineColor else Color(0xFF3A3F47),
                    radius = if (active) 20f else 14f,
                    center = center
                )
            }
        }
    }
}

private fun nearestNode(point: Offset, centers: List<Offset>, radiusPx: Float): Int? {
    var closestIndex: Int? = null
    var closestDistance = Float.MAX_VALUE
    centers.forEachIndexed { index, center ->
        val dx = point.x - center.x
        val dy = point.y - center.y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance < radiusPx && distance < closestDistance) {
            closestDistance = distance
            closestIndex = index
        }
    }
    return closestIndex
}
