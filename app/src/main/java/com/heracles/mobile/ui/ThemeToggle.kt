/*
 File: ui/ThemeToggle.kt
 What it does: Provides UI controls for switching theme state or mode selection in the app.
 Main inputs: current theme-related values from the ViewModel or settings.
 Main outputs: updated theme selection and visual toggle state.
 Key functions/classes: theme toggle composables.
*/

package com.heracles.mobile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable

@Composable
fun SunMoonThemeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
            )
        }
    )
}
