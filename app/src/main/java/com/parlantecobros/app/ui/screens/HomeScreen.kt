package com.parlantecobros.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parlantecobros.app.data.PaymentRepository
import com.parlantecobros.app.model.AppSource
import com.parlantecobros.app.model.PaymentItem
import com.parlantecobros.app.model.SpeechTemplate
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
    val lastCapturedNotification by PaymentRepository.lastCapturedNotification.collectAsState()

    val totalToday = payments.sumOf { it.amount }
    val countToday = payments.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppleBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // 1. Apple Header Minimalista
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Parlante Cobros",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppleTextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            text = "Avisos de voz para Yape, Plin y bancos",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppleTextSecondary
                            )
                        )
                    }

                    // Status Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (settings.speakerEnabled) AppleGreen.copy(alpha = 0.12f) else AppleTextSecondary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (settings.speakerEnabled) AppleGreen else AppleTextSecondary)
                        )
                        Text(
                            text = if (settings.speakerEnabled) "Activo" else "Pausado",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (settings.speakerEnabled) AppleGreen else AppleTextSecondary
                            )
                        )
                    }
                }
            }
        }

        // 2. Banner de Permisos (si no está activo)
        if (!isNotificationPermissionGranted) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AppleOrange.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = AppleCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = "Alerta",
                            tint = AppleOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permiso de notificaciones necesario",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleTextPrimary
                                )
                            )
                            Text(
                                text = "Permite que la app lea las notificaciones de Yape y bancos para hablar.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppleTextSecondary
                                )
                            )
                        }
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Activar",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }

        // 3. Tarjeta Hero de Cobros del Día (Estilo Apple KPI)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppleBorder, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCard),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL RECAUDADO HOY",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppleTextSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "$countToday cobros",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = AppleTextSecondary
                            )
                        )
                    }

                    Text(
                        text = String.format("S/ %.2f", totalToday),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppleTextPrimary,
                            letterSpacing = (-1).sp
                        )
                    )

                    Divider(color = AppleBorder, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voz por altavoz",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = AppleTextPrimary
                            )
                        )
                        Switch(
                            checked = settings.speakerEnabled,
                            onCheckedChange = { checked ->
                                PaymentRepository.updateSettings { it.copy(speakerEnabled = checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppleGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = AppleBorder
                            )
                        )
                    }
                }
            }
        }

        // 3.5. Diagnóstico de Notificaciones en Vivo y Xiaomi
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppleBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monitor de Notificaciones",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AppleTextPrimary
                            )
                        )
                        TextButton(onClick = onRebindService) {
                            Text("Reconectar y Probar", color = AppleBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleBackground)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = lastCapturedNotification,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppleTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenAppSettings,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ajustes de la App (Inicio Automático Xiaomi)", color = AppleTextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. Panel de Configuración Rápida
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppleBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Preferencias de Voz",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AppleTextPrimary
                        )
                    )

                    // Switch mención de nombre
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mencionar nombre del cliente",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = AppleTextPrimary
                                )
                            )
                            Text(
                                text = "Ej: '...de Carlos Pérez'",
                                style = MaterialTheme.typography.bodySmall.copy(color = AppleTextSecondary)
                            )
                        }
                        Switch(
                            checked = settings.mentionCustomerName,
                            onCheckedChange = { checked ->
                                PaymentRepository.updateSettings { it.copy(mentionCustomerName = checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppleBlue
                            )
                        )
                    }

                    Divider(color = AppleBorder, thickness = 0.8.dp)

                    // Campanilla previa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Campanilla previa (Ding)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = AppleTextPrimary
                                )
                            )
                            Text(
                                text = "Emite un tono antes de hablar",
                                style = MaterialTheme.typography.bodySmall.copy(color = AppleTextSecondary)
                            )
                        }
                        Switch(
                            checked = settings.chimeBeforeSpeaking,
                            onCheckedChange = { checked ->
                                PaymentRepository.updateSettings { it.copy(chimeBeforeSpeaking = checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppleBlue
                            )
                        )
                    }

                    // Botón probar voz
                    OutlinedButton(
                        onClick = {
                            onTestVoice("¡Yape recibido! Quince soles con cincuenta de Juan Pérez")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppleBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = "Probar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Probar Voz de Alerta",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // 5. Simulador Rápido de Pruebas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppleBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = AppleCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Simulador de Cobro Inmediato",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AppleTextPrimary
                        )
                    )
                    Text(
                        text = "Toca para simular que un cliente te paga y verificar el parlante:",
                        style = MaterialTheme.typography.bodySmall.copy(color = AppleTextSecondary)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val item = PaymentItem(
                                    appSource = AppSource.YAPE,
                                    senderName = "Carlos Mendoza",
                                    amount = 25.00,
                                    rawText = "Te llegó un Yape de Carlos Mendoza por S/ 25.00"
                                )
                                PaymentRepository.addPayment(item)
                                onTestVoice("¡Yape recibido! Veinticinco soles de Carlos Mendoza")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = YapeColor.copy(alpha = 0.12f),
                                contentColor = YapeColor
                            )
                        ) {
                            Text("Yape S/ 25", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                val item = PaymentItem(
                                    appSource = AppSource.PLIN,
                                    senderName = "María Rojas",
                                    amount = 18.50,
                                    rawText = "María Rojas te transfirió S/ 18.50 por Plin"
                                )
                                PaymentRepository.addPayment(item)
                                onTestVoice("¡Plin recibido! Dieciocho soles con cincuenta de María Rojas")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PlinColor.copy(alpha = 0.12f),
                                contentColor = PlinColor
                            )
                        ) {
                            Text("Plin S/ 18.50", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                val item = PaymentItem(
                                    appSource = AppSource.BCP,
                                    senderName = "Luis Gomez",
                                    amount = 120.00,
                                    rawText = "Abono BCP: S/ 120.00 de Luis Gomez"
                                )
                                PaymentRepository.addPayment(item)
                                onTestVoice("¡BCP recibido! Ciento veinte soles de Luis Gomez")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BcpColor.copy(alpha = 0.12f),
                                contentColor = BcpColor
                            )
                        ) {
                            Text("BCP S/ 120", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 6. Historial de Cobros Recientes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historial Reciente",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary
                    )
                )

                if (payments.isNotEmpty()) {
                    TextButton(onClick = { PaymentRepository.clearHistory() }) {
                        Text(
                            text = "Limpiar",
                            style = MaterialTheme.typography.labelMedium.copy(color = AppleTextSecondary)
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
                        .border(1.dp, AppleBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = AppleCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = AppleTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Aún no hay cobros registrados hoy",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = AppleTextSecondary
                            )
                        )
                        Text(
                            text = "Cuando recibas un Yape, Plin o abono, sonará aquí al instante.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppleTextTertiary
                            )
                        )
                    }
                }
            }
        } else {
            items(payments, key = { it.id }) { item ->
                PaymentCard(payment = item)
            }
        }
    }
}

@Composable
fun PaymentCard(payment: PaymentItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppleBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = AppleCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Badge con color de app
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
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

                Column {
                    Text(
                        text = payment.senderName.ifEmpty { "Cliente" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AppleTextPrimary
                        )
                    )
                    Text(
                        text = "${payment.appSource.displayName} • ${payment.formattedTime}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppleTextSecondary
                        )
                    )
                }
            }

            // Monto en grande y limpio
            Text(
                text = payment.formattedAmount,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppleTextPrimary
                )
            )
        }
    }
}
