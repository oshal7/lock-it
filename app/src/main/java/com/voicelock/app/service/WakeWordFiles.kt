package com.voicelock.app.service

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Where the user's trained wake-word model lives. Rather than baking one person's trigger
 * phrase into the APK at build time (which would mean every phrase change needs a CI rebuild),
 * the .ppn keyword file generated at console.picovoice.ai is imported at runtime via a file
 * picker (see EnrollTriggerScreen) and copied here, into app-private storage. It never leaves
 * the device and is never bundled into the app itself.
 */
object WakeWordFiles {
    private const val KEYWORD_FILE_NAME = "trigger.ppn"

    fun keywordFile(context: Context): File =
        File(context.filesDir, KEYWORD_FILE_NAME)

    fun hasKeywordFile(context: Context): Boolean =
        keywordFile(context).exists() && keywordFile(context).length() > 0

    /** Copies the picked .ppn file into app-private storage. Returns true on success. */
    fun importKeywordFile(context: Context, source: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(source)?.use { input ->
                keywordFile(context).outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteKeywordFile(context: Context) {
        keywordFile(context).delete()
    }
}
