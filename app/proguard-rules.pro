# Porcupine uses JNI; keep its classes intact.
-keep class ai.picovoice.porcupine.** { *; }

# Keep DeviceAdminReceiver / AccessibilityService subclasses referenced only from XML/manifest.
-keep class com.voicelock.app.admin.VoiceLockDeviceAdminReceiver { *; }
-keep class com.voicelock.app.accessibility.GateAccessibilityService { *; }
-keep class com.voicelock.app.service.BootReceiver { *; }

# androidx.security:security-crypto pulls in Google Tink, which references annotation-only
# classes (errorprone, checkerframework) that aren't on the runtime classpath and don't need to
# be — they're compile-time-only annotations. R8 fails the build over them unless told they're
# safe to ignore.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn org.checkerframework.checker.nullness.qual.**
