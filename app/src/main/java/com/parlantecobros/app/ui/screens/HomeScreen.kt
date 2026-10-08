package com.parlantecobros.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parlantecobros.app.data.PaymentRepository
import com.parlantecobros.app.model.AppSource
import com.parlantecobros.app.model.PaymentItem
import com.parlantecobros.app.ui.theme.*

@Composable
fun HomeScreen(
    isNotificationPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRebindService: () -> Unit,
    onTestVoice: (String) -> Unit
) {
    val payments by PaymentRepository.payments.collectAsState()
    val settings by PaymentRepository.settings.collectAsState()

    val totalToday = payments.sumOf { it.amount }
    val countToday = payments.size
    var isAmountVisible by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        // 1. Barra Superior Minimalista
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mi Negocio",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "Caja de Cobros",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }

                // Live Status Pill (Estilo Neo-Fintech)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (settings.speakerEnabled) EmeraldGlow else Color(0xFFE5E7EB))
                        .border(
                            1.dp,
                            if (settings.speakerEnabled) EmeraldGreen.copy(alpha = 0.3f) else Color.Transparent,
                            RoundedCornerShape(30.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (settings.speakerEnabled) EmeraldGreen else TextMuted)
                    )
                    Text(
                        text = if (settings.speakerEnabled) "En Escucha" else "En Pausa",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (settings.speakerEnabled) EmeraldGreen else TextMuted
                        )
                    )
                }
            }
        }

        // 2. Banner de Permisos de Notificaciones (si hace falta)
        if (!isNotificationPermissionGranted) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permiso de Notificaciones",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMain
                                )
                            )
                            Text(
                                text = "Necesario para detectar los pagos de Yape y bancos.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = TextMain),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Activar", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }

        // 3. Tarjeta Hero Obsidian (Inspirada en las imágenes de referencia)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = ObsidianDark),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL RECAUDADO HOY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9CA3AF),
                                letterSpacing = 1.2.sp
                            )
                        )

                        // Botón Ocultar/Mostrar saldo
                        IconButton(
                            onClick = { isAmountVisible = !isAmountVisible },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isAmountVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Alternar Visibilidad",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Monto Principal Estilo Fintech
                    Text(
                        text = if (isAmountVisible) String.format("S/ %.2f", totalToday) else "S/ ••••••",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-1.2).sp
                        )
                    )

                    // Sub-estadísticas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1F242C))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$countToday cobros registrados",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFD1D5DB),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        if (countToday > 0) {
                            val average = totalToday / countToday
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1F242C))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = String.format("Promedio S/ %.2f", average),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFD1D5DB),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    Divider(color = ObsidianBorder, thickness = 0.8.dp)

                    // Switch Principal de Altavoz
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Altavoz de Cobros",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Anunciar pagos entrantes por voz",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }

                        Switch(
                            checked = settings.speakerEnabled,
                            onCheckedChange = { checked ->
                                PaymentRepository.updateSettings { it.copy(speakerEnabled = checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreen,
                                uncheckedThumbColor = Color(0xFF9CA3AF),
                                uncheckedTrackColor = Color(0xFF262C36)
                            )
                        )
                    }
                }
            }
        }

        // 4. Módulo de Controles Rápidos (Bento Squircles - Estilo Image 1 y 2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Probar Altavoz
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTestVoice("¡Yape recibido! Pago verificado correctamente") }
                        .border(1.dp, LuxuryBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = LuxurySurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LuxuryBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VolumeUp,
                                contentDescription = null,
                                tint = TextMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Probar Voz",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                        )
                        Text(
                            text = "Escuchar audio",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Card 2: Mencionar Nombre
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            PaymentRepository.updateSettings { it.copy(mentionCustomerName = !it.mentionCustomerName) }
                        }
                        .border(1.dp, LuxuryBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = LuxurySurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LuxuryBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = if (settings.mentionCustomerName) EmeraldGreen else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Nombre",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                        )
                        Text(
                            text = if (settings.mentionCustomerName) "Activado" else "Solo monto",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (settings.mentionCustomerName) EmeraldGreen else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Card 3: Campanilla
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            PaymentRepository.updateSettings { it.copy(chimeBeforeSpeaking = !it.chimeBeforeSpeaking) }
                        }
                        .border(1.dp, LuxuryBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = LuxurySurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LuxuryBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                tint = if (settings.chimeBeforeSpeaking) EmeraldGreen else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Campana",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                        )
                        Text(
                            text = if (settings.chimeBeforeSpeaking) "Activada" else "Silenciada",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (settings.chimeBeforeSpeaking) EmeraldGreen else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // 5. Billeteras Vinculadas (Carrusel de Pills Moderno)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Billeteras Vinculadas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val apps = listOf(
                        Triple("Yape", YapeBrand, "Activo"),
                        Triple("Plin", PlinBrand, "Activo"),
                        Triple("BCP", BcpBrand, "Activo"),
                        Triple("Interbank", InterbankBrand, "Activo"),
                        Triple("BBVA", BbvaBrand, "Activo")
                    )

                    items(apps) { (name, color, status) ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(LuxurySurface)
                                .border(1.dp, LuxuryBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.take(1),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMain
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                            )
                        }
                    }
                }
            }
        }

        // 6. Historial de Cobros Recientes (Estilo Dribbble / Revolut)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cobros Recientes",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                )

                if (payments.isNotEmpty()) {
                    TextButton(onClick = { PaymentRepository.clearHistory() }) {
                        Text(
                            text = "Limpiar historial",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                        )
                    }
                }
            }
        }

        if (payments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LuxuryBorder, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = LuxurySurface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(LuxuryBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = "A la espera de tus cobros",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextMain
                            )
                        )
                        Text(
                            text = "Apenas un cliente te yapee o transfiera, el parlante anunciará el monto en voz alta y se registrará aquí.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            items(payments, key = { it.id }) { item ->
                LuxuryTransactionCard(payment = item)
            }
        }
    }
}

@Composable
fun LuxuryTransactionCard(payment: PaymentItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LuxuryBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = LuxurySurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Squircle Monograma
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(payment.appSource.badgeColorHex).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = payment.appSource.displayName.take(1),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(payment.appSource.badgeColorHex)
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = payment.senderName.ifEmpty { "Cliente" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                    )
                    Text(
                        text = "${payment.appSource.displayName} • ${payment.formattedTime}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Monto en verde esmeralda vibrante (+ S/ 25.00)
            Text(
                text = "+ ${payment.formattedAmount}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    letterSpacing = (-0.3).sp
                )
            )
        }
    }
}
