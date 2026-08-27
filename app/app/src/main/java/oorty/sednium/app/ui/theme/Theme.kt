package oorty.sednium.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SedniumLightColors = lightColorScheme(
    primary = SedniumColors.Orange,
    onPrimary = SedniumColors.Milk,
    background = SedniumColors.Milk,
    onBackground = SedniumColors.Orange,
    surface = SedniumColors.Milk,
    onSurface = SedniumColors.Orange,
    surfaceVariant = OrangeAlpha.a05,
    onSurfaceVariant = SedniumColors.Orange,
    outline = OrangeAlpha.a30,
    outlineVariant = OrangeAlpha.a20,
    error = SedniumColors.Red600,
    onError = SedniumColors.White,
    errorContainer = SedniumColors.Red100,
    onErrorContainer = SedniumColors.Red800
)

private val SedniumDarkColors = darkColorScheme(
    primary = SedniumColors.DarkOrange,
    onPrimary = SedniumColors.Milk,
    background = Color(0xFF1E1E1E),
    onBackground = SedniumColors.Gray100,
    surface = Color(0xFF282828),
    onSurface = SedniumColors.Gray100,
    surfaceVariant = SedniumColors.Gray800,
    onSurfaceVariant = SedniumColors.Gray300,
    outline = SedniumColors.Gray700,
    outlineVariant = SedniumColors.Gray800,
    error = SedniumColors.Red500,
    onError = SedniumColors.White,
    errorContainer = SedniumColors.Red900,
    onErrorContainer = SedniumColors.Red100
)

/** Exposes a simple boolean so deep components can branch like the TSX did with `isDark`. */
val LocalSedniumIsDark = staticCompositionLocalOf { false }

@Composable
fun SedniumTheme(
    darkTheme: Boolean = false,
    useSerif: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) SedniumDarkColors else SedniumLightColors
    val typography = getSedniumTypography(useSerif = useSerif)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val statusBarColor = if (darkTheme) android.graphics.Color.parseColor("#1E1E1E") else android.graphics.Color.parseColor("#FDFBF7")
            window.statusBarColor = statusBarColor
            window.navigationBarColor = statusBarColor
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalSedniumIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            shapes = SedniumShapes,
            content = content
        )
    }
}

