package com.heracles.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.heracles.mobile.ui.HeraclesApp
import com.heracles.mobile.ui.theme.HeraclesTheme
import com.heracles.mobile.model.SystemUiMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(applicationContext))
                // support launching to a specific screen via intent extra for debugging/navigation
                val openScreen = intent?.getStringExtra("openScreen")
                val setPremiumPresent = intent?.extras?.containsKey("setPremiumUi") == true
                val setPremiumValue = intent?.getBooleanExtra("setPremiumUi", false) ?: false
                LaunchedEffect(openScreen, setPremiumPresent, setPremiumValue) {
                    if (setPremiumPresent) {
                        // debug bridge: true=dark, false=light
                        val mode = if (setPremiumValue) SystemUiMode.DARK else SystemUiMode.LIGHT
                        appViewModel.setSystemUiMode(mode)
                    }
                    if (!openScreen.isNullOrBlank()) {
                        appViewModel.switchScreen(openScreen)
                        // re-apply after a short delay in case startup hydration resets the screen
                        delay(300)
                        appViewModel.switchScreen(openScreen)
                    }
                }
            val activeMod = appViewModel.activeThemeMod()
            val resolvedDarkTheme = when (appViewModel.settings.systemUiMode) {
                SystemUiMode.DARK -> true
                SystemUiMode.LIGHT -> false
                SystemUiMode.AUTO -> isSystemInDarkTheme()
            }
            HeraclesTheme(
                darkTheme = resolvedDarkTheme,
                uiScale = appViewModel.settings.uiScale,
                currentScreen = appViewModel.currentScreen,
                themeMod = activeMod,
                activeLightSchemeId = appViewModel.settings.activeLightSchemeId,
                activeDarkSchemeId = appViewModel.settings.activeDarkSchemeId,
                uiFidelity = appViewModel.settings.uiFidelity,
            ) {
                HeraclesApp(appViewModel)
            }
        }
    }
}
