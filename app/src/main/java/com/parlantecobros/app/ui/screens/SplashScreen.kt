package com.parlantecobros.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parlantecobros.app.R
import com.parlantecobros.app.data.SessionManager
import com.parlantecobros.app.ui.theme.EmeraldGreen
import com.parlantecobros.app.ui.theme.ObsidianDark
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val session by SessionManager.sessionState.collectAsState()

    // Animación suave de escala y desvanecimiento
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
        delay(1800) // 1.8 segundos de presentación
        if (session.isLoggedIn) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }

    val transition = updateTransition(transitionState, label = "SplashTransition")
    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 1000, easing = FastOutSlowInEasing) },
        label = "LogoScale"
    ) { state -> if (state) 1f else 0.85f }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800) },
        label = "LogoAlpha"
    ) { state -> if (state) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo completo de Punk
            Image(
                painter = painterResource(id = R.drawable.logo_punk),
                contentDescription = "Punk Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, Color(0xFF262C36), RoundedCornerShape(32.dp))
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Punk Cobros",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Inteligencia de Cobros & Audio POS",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Barra de carga discreta
            LinearProgressIndicator(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = EmeraldGreen,
                trackColor = Color(0xFF1F242C)
            )
        }

        // Pie de página de versión
        Text(
            text = "Versión 1.0.8 • Enterprise",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }
}
