package com.zhuanshi.feixing.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Scheme = darkColorScheme(
    primary = Gold,
    onPrimary = NightBg,
    background = NightBg,
    onBackground = TextPrimary,
    surface = NightBg,
    onSurface = TextPrimary
)

@Composable
fun FocusFlightTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
