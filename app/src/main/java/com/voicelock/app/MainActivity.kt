package com.voicelock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.voicelock.app.data.AppPreferences
import com.voicelock.app.ui.home.HomeScreen
import com.voicelock.app.ui.onboarding.AccessibilityScreen
import com.voicelock.app.ui.onboarding.ArmScreen
import com.voicelock.app.ui.onboarding.BatteryScreen
import com.voicelock.app.ui.onboarding.DeviceAdminScreen
import com.voicelock.app.ui.onboarding.EnrollTriggerScreen
import com.voicelock.app.ui.onboarding.MicPermissionScreen
import com.voicelock.app.ui.onboarding.SetCredentialScreen
import com.voicelock.app.ui.onboarding.WelcomeScreen
import com.voicelock.app.ui.settings.SettingsScreen
import com.voicelock.app.ui.theme.VoiceLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPreferences(this)

        setContent {
            VoiceLockTheme {
                val navController = rememberNavController()
                val startDestination = if (prefs.onboardingComplete) Routes.HOME else Routes.ONBOARDING_WELCOME

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.ONBOARDING_WELCOME) {
                        WelcomeScreen(onNext = { navController.navigate(Routes.ONBOARDING_MIC) })
                    }
                    composable(Routes.ONBOARDING_MIC) {
                        MicPermissionScreen(onNext = { navController.navigate(Routes.ONBOARDING_ADMIN) })
                    }
                    composable(Routes.ONBOARDING_ADMIN) {
                        DeviceAdminScreen(onNext = { navController.navigate(Routes.ONBOARDING_ACCESSIBILITY) })
                    }
                    composable(Routes.ONBOARDING_ACCESSIBILITY) {
                        AccessibilityScreen(onNext = { navController.navigate(Routes.ONBOARDING_BATTERY) })
                    }
                    composable(Routes.ONBOARDING_BATTERY) {
                        BatteryScreen(onNext = { navController.navigate(Routes.ONBOARDING_ENROLL) })
                    }
                    composable(Routes.ONBOARDING_ENROLL) {
                        EnrollTriggerScreen(onNext = { navController.navigate(Routes.ONBOARDING_CREDENTIAL) })
                    }
                    composable(Routes.ONBOARDING_CREDENTIAL) {
                        SetCredentialScreen(onNext = { navController.navigate(Routes.ONBOARDING_ARM) })
                    }
                    composable(Routes.ONBOARDING_ARM) {
                        ArmScreen(onFinish = {
                            prefs.onboardingComplete = true
                            navController.navigate(Routes.HOME) {
                                popUpTo(0)
                            }
                        })
                    }
                    composable(Routes.HOME) {
                        HomeScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
