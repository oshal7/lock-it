# Porcupine uses JNI; keep its classes intact.
-keep class ai.picovoice.porcupine.** { *; }

# Keep DeviceAdminReceiver / AccessibilityService subclasses referenced only from XML/manifest.
-keep class com.voicelock.app.admin.VoiceLockDeviceAdminReceiver { *; }
-keep class com.voicelock.app.accessibility.GateAccessibilityService { *; }
-keep class com.voicelock.app.service.BootReceiver { *; }
