package com.example.spartangymapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val SpartanDefaultRed = Color(0xFFE10613)
val LocalSpartanAccent = staticCompositionLocalOf { SpartanDefaultRed }

private fun darkSpartanColorScheme(accentColor: Color) = darkColorScheme(
    primary = accentColor,
    secondary = accentColor,
    tertiary = accentColor,
    background = Fondo,
    surface = Superficie,
    surfaceContainer = SuperficieElevada,
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = TextoSecundario,
    outline = Color(0xFF767676),
    outlineVariant = Borde,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private fun lightSpartanColorScheme(accentColor: Color) = lightColorScheme(
    primary = accentColor,
    secondary = accentColor,
    tertiary = accentColor,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

/** Tres radios para toda la app: controles pequeños, campos/botones y tarjetas. */
val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun SpartanGymAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    accentColor: Color = SpartanDefaultRed,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkSpartanColorScheme(accentColor)
        else -> lightSpartanColorScheme(accentColor)
    }

    CompositionLocalProvider(LocalSpartanAccent provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Formas,
            content = content
        )
    }
}
