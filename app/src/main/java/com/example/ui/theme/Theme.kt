package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val InvexxColorScheme = lightColorScheme(
    primary = PrimaryGold,
    secondary = SecondaryGold,
    background = SoftBackground,
    surface = PureWhite,
    onPrimary = DarkCharcoal,
    onSecondary = DarkCharcoal,
    onBackground = DarkCharcoal,
    onSurface = DarkCharcoal,
    primaryContainer = GoldenLight,
    onPrimaryContainer = DarkCharcoal,
    surfaceVariant = SoftBackground,
    onSurfaceVariant = MediumGray
)

@Composable
fun InvexxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We enforce the premium light-gold look as requested by the branding guidelines
    val colorScheme = InvexxColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep MyApplicationTheme for compatibility with MainActivity and test configurations
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    InvexxTheme(darkTheme = darkTheme, content = content)
}
