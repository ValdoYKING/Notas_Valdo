package com.valdo.notasinteligentesvaldo.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

/**
 * Pantalla de presentación (Splash Screen) con animación Lottie personalizada
 * utilizando el archivo 'lapiz_notas_relleno.lottie.json'.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startFadeOut by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startFadeOut) 0f else 1f,
        animationSpec = tween(durationMillis = 600),
        finishedListener = {
            if (startFadeOut) {
                onSplashFinished()
            }
        }
    )

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lapiz_notas_relleno.lottie.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 1.2f
    )

    // Cuando la animación Lottie termine o pasen 3 segundos, iniciar el desvanecimiento
    LaunchedEffect(progress) {
        if (progress >= 0.99f) {
            startFadeOut = true
        }
    }

    LaunchedEffect(Unit) {
        delay(3000) // Timeout de respaldo
        startFadeOut = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .alpha(alphaAnim),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "MEMO",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Notas Inteligentes",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
