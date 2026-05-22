package com.heracles.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heracles.mobile.ui.HeraclesApp
import com.heracles.mobile.ui.theme.HeraclesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeraclesTheme {
                val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(applicationContext))
                HeraclesApp(appViewModel)
            }
        }
    }
}
