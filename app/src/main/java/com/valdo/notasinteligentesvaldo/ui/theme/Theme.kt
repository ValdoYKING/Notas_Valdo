package com.valdo.notasinteligentesvaldo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Esquema especial para "Oscuro+" (OLED) con surface negro puro y ahorro de energía en pantallas OLED.
private val DarkPlusColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color.Black, // Mayor contraste en negro puro
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color.Black, // Fondo totalmente negro para OLED
    surface = Color.Black,    // Superficie negra para minimizar emisión de luz
    surfaceVariant = Color.Black,
    primaryContainer = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.85f)
)
// Nota para el usuario: El modo "Oscuro+ (OLED)" usa negro puro para reducir el consumo de batería en pantallas OLED.

val LocalOledDarkMode = staticCompositionLocalOf { false }

@Composable
fun NotasInteligentesValdoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    darkPlus: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val useDynamic = dynamicColor && !darkPlus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamic -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme && darkPlus -> DarkPlusColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = (view.context as Activity)
        val window = activity.window
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)

        SideEffect {
            // Iconos: claros en oscuro, oscuros en claro
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme

            // Colores de barras: usar el tono del tema para que siempre haya fondo
            // (soluciona barras "invisibles" cuando la ventana dibuja edge-to-edge).
            val barColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.statusBarColor = barColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = barColor

            // Comportamiento moderno para mostrar/ocultar system bars
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }

    CompositionLocalProvider(LocalOledDarkMode provides (darkTheme && darkPlus)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
