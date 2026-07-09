# VoiceLock

A privacy-first, fully offline Android app: say your trigger phrase and the phone locks
instantly, then demands a second VoiceLock passcode/pattern — separate from your system PIN —
before it's usable again. Full product spec: see the PRD this repo implements.

VoiceLock never requests the `INTERNET` permission (the manifest explicitly strips it even if a
dependency tried to add it back), has no accounts, no analytics, no cloud sync. Everything —
wake-word detection, credential storage — happens on-device.

Built entirely through GitHub Actions. You do not need Android Studio installed anywhere.

## One-time setup

Do these once, before your first build.

### 1. Generate a release keystore

This is the single most important step. **Every build, forever, must be signed with the same
keystore.** Android treats a new APK with the same package name (`com.voicelock.app`) and the
same signing certificate as an *update* — it installs over the old one, keeps all your app data
(your enrolled trigger phrase, your VoiceLock passcode, your settings), and needs no uninstall.
If you ever sign with a different keystore, Android will refuse to install the new build at all
until you uninstall the old one first — which wipes everything. So: generate this once, and
never lose it or regenerate it.

```bash
keytool -genkeypair -v -storetype PKCS12 \
  -keystore release.keystore -alias voicelock \
  -keyalg RSA -keysize 2048 -validity 10000
```

You'll be prompted for a store password, a key password, and identity details (any values are
fine — this never leaves your control).

### 2. Add GitHub Actions secrets

In your repo: **Settings → Secrets and variables → Actions → New repository secret**. Add:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 release.keystore` (the whole output) |
| `RELEASE_KEYSTORE_PASSWORD` | the store password you chose |
| `RELEASE_KEY_ALIAS` | `voicelock` (or whatever alias you used) |
| `RELEASE_KEY_PASSWORD` | the key password you chose |
| `PICOVOICE_ACCESS_KEY` | optional — see below |

The workflow (`.github/workflows/build.yml`) will hard-fail with a clear error if
`RELEASE_KEYSTORE_BASE64` is missing, rather than silently signing with a throwaway key — that
guardrail is deliberate, see above.

Delete your local `release.keystore` file after step 2, or keep it somewhere safe *outside* the
repo (never commit it — `.gitignore` already excludes `*.keystore` and `*.jks`).

### 3. Get a Picovoice AccessKey (free)

Wake-word detection uses [Picovoice Porcupine](https://picovoice.ai/), which runs fully on-device
at runtime — the only online step is a one-time visit to the Picovoice Console in a browser to
generate your personal AccessKey and train your custom trigger phrase:

1. Sign up free at [console.picovoice.ai](https://console.picovoice.ai/).
2. Copy your **AccessKey** from the console home page.
3. Under **Porcupine → Create Wake Word**, type your phrase (e.g. "lock lock lock"), pick
   **Android** as the target platform, and download the generated `.ppn` file.

You'll enter the AccessKey and import the `.ppn` file *inside the app* during onboarding (or
later in Settings) — no rebuild needed to change your phrase. If you'd rather have it pre-filled
at build time, set the `PICOVOICE_ACCESS_KEY` secret above; it becomes the default but stays
editable in-app.

## Building

Every push to `main` or any `claude/**` branch, every tag, and manual runs (Actions tab → *Build
signed APK* → **Run workflow**) all produce a signed, versioned APK.

- **Versioning:** `versionCode` is the GitHub Actions run number — it only ever goes up, across
  every build this repo ever produces, which is what Android requires to accept a new APK as an
  update. `versionName` is `<VERSION file>+<run number>`, e.g. `1.0.0+42`. Bump the `VERSION`
  file for a real release; every CI run gets a unique build number regardless.
- **Artifact:** download from the workflow run's **Artifacts** section — named
  `VoiceLock-<versionName>.apk`.
- **Tagged releases:** pushing a tag like `v1.0.0` additionally attaches the APK to a GitHub
  Release.

## Installing on your phone

1. Download the APK artifact (Actions tab → latest run → Artifacts) onto your phone, or the
   Release asset if you tagged one.
2. Android will prompt to allow installs from that source the first time — allow it.
3. Tap the APK to install. **Installing a new build over an old one just updates it in place —
   your enrolled phrase, passcode, and settings are preserved.** You never need to uninstall
   first, as long as every build came from this same CI pipeline (same keystore).
4. Open VoiceLock and complete onboarding (mic permission → device admin → accessibility service
   → battery exemption → record/import trigger phrase → set VoiceLock passcode → arm).

### Manufacturer-specific battery settings

Some OEMs have an extra, more aggressive background-app killer on top of stock Android's battery
optimization, and the in-app "battery exemption" request doesn't cover it:

- **Samsung:** Settings → Apps → VoiceLock → Battery → set to *Unrestricted*. Also add VoiceLock
  to Settings → Device care → Battery → Background usage limits → *Never sleeping apps*.
- **Xiaomi (MIUI):** Settings → Apps → Manage apps → VoiceLock → Autostart → enable. Also Battery
  saver → VoiceLock → *No restrictions*.
- **OnePlus (OxygenOS):** Settings → Battery → Battery optimization → VoiceLock → *Don't
  optimize*. Also enable it under Advanced → App auto-launch.

If listening stops unreliably after a while despite these, the app's background watchdog
(`ServiceWatchdogWorker`) restarts the listening service every 15 minutes as a fallback.

## About "block uninstall" — what's actually possible

You can't fully prevent uninstalling an Android app you sideload onto an already-set-up phone —
that's a deliberate platform restriction, not something any app configuration can bypass. What
this repo *does* guarantee, without any extra step from you, is that a new build **updates in
place** instead of requiring an uninstall (see the keystore section above) — that alone covers
"install a new test build without losing my data," which is normally the actual goal.

If you specifically want VoiceLock to also block itself from being uninstalled at all (short of a
factory reset), that requires making it the device's **Device Owner**, which Android only allows
on a phone with no accounts configured yet (or via a fresh factory reset):

```bash
adb shell dpm set-device-owner com.voicelock.app/.admin.VoiceLockDeviceAdminReceiver
```

`LockController.setUninstallBlocked()` already checks for device-owner status and calls
`DevicePolicyManager.setUninstallBlocked()` when available — flip it on from Settings once you've
provisioned device owner, if you go this route. This is optional and not required for normal use.

## Recovery

If you forget your VoiceLock passcode:

- **Safe Mode:** hold the power button → long-press "Power off" → confirm "Reboot to Safe Mode".
  Safe Mode disables all third-party apps, including VoiceLock, so the gate won't appear — you
  can then uninstall or reconfigure it, and reboot normally afterward.
- **Recovery question:** if you set one during onboarding, it's available as a fallback (not yet
  wired into the gate UI itself in this build — currently a Settings-only reference; extending
  the gate screen with a "forgot passcode" flow that checks it is a natural next step).

## Architecture

```
Foreground Service (ListeningService)
  └── Porcupine wake-word engine (owns its own AudioRecord)
        └── on detection → LockController.triggerLock()
LockController
  ├── DevicePolicyManager.lockNow()
  └── persists the "hardened" flag (EncryptedSharedPreferences)
GateAccessibilityService
  └── on ACTION_USER_PRESENT / window-state change + hardened flag
        └── launches GateActivity (screen pinning / kiosk)
GateActivity
  └── passcode or pattern check → clears hardened flag
```

- Kotlin, Jetpack Compose, min SDK 26 / target SDK 35.
- Credentials are stored only as salted PBKDF2 hashes (`CredentialHasher`), never in plaintext.
- All persisted state lives in `EncryptedSharedPreferences` (AES-256); backups are disabled
  entirely (`allowBackup="false"`) so nothing leaves the device even via a phone-to-phone
  transfer.

## Known limitations (v1)

- Changing an already-set **pattern** credential isn't wired up in Settings yet (passcode change
  is). Work around it by completing onboarding again.
- The gate's "forgot passcode" flow doesn't yet check the recovery answer — Safe Mode is the
  reliable fallback today.
- No speaker verification: anyone saying your phrase can trigger the *lock*, by design — it only
  ever locks, never unlocks, so this is considered acceptable per the original spec.
- Not published to Play Store; sideload only, per the original design decision to avoid Device
  Admin/Accessibility policy review friction.
