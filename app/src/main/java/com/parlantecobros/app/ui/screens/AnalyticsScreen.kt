package com.parlantecobros.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parlantecobros.app.data.PaymentRepository
import com.parlantecobros.app.ui.theme.*

@Composable
fun AnalyticsScreen() {
    val payments by PaymentRepository.payments.collectAsState()

    val totalToday = PaymentRepository.totalAmountToday
    val countToday = PaymentRepository.totalCountToday
    val averageTicket = PaymentRepository.averageTicket
    val maxPayment = PaymentRepository.maxPayment
    val walletShares = PaymentRepository.walletShares
    val timeSlotStats = PaymentRepository.timeSlotStats
    val peakHour = PaymentRepository.peakHourSlot

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Título de la Sección BI
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Inteligencia de Negocio",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Análisis de Cobros",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        letterSpacing = (-0.5).sp
                    )
                )
            }
        }

        // 2. Tarjeta Hero BI (Ticket Promedio y Rendimiento)
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
                    Text(
                        text = "TICKET PROMEDIO POR CLIENTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF),
                            letterSpacing = 1.2.sp
                        )
                    )

                    Text(
                        text = String.format("S/ %.2f", averageTicket),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-1.2).sp
                        )
                    )

                    Divider(color = ObsidianBorder, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mayor Cobro Hoy",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF))
                            )
                            Text(
                                text = if (maxPayment != null) String.format("S/ %.2f", maxPayment.amount) else "S/ 0.00",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Ventas Totales",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF))
                            )
                            Text(
                                text = String.format("S/ %.2f (%d)", totalToday, countToday),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Insight de Horario Pico (Estilo Dribbble)
        if (peakHour != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldGreen.copy(alpha = 0.2f), RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = LuxurySurface),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EmeraldGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Horario con Mayor Movimiento",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "${peakHour.label} con S/ ${String.format("%.2f", peakHour.totalAmount)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Gráfico de Barras: Distribución de Ventas por Franja Horaria
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LuxuryBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = LuxurySurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rendimiento por Horario",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                        )
                        Text(
                            text = "Hoy",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }

                    // Gráfico de Barras Proporcional
                    val maxSlotAmount = timeSlotStats.maxOfOrNull { it.totalAmount }?.coerceAtLeast(1.0) ?: 1.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        timeSlotStats.forEach { slot ->
                            val ratio = (slot.totalAmount / maxSlotAmount).toFloat().coerceIn(0.05f, 1f)
                            val isPeak = peakHour != null && slot.label == peakHour.label

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (slot.totalAmount > 0) {
                                    Text(
                                        text = "S/${slot.totalAmount.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPeak) EmeraldGreen else TextMuted
                                        ),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .fillMaxHeight(ratio)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(if (isPeak) EmeraldGreen else Color(0xFFE5E7EB))
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = slot.label.takeWhile { it != '(' }.trim(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Distribución por Billetera Digital (Yape vs Plin vs BCP)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LuxuryBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = LuxurySurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Participación por Billetera",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                    )

                    if (walletShares.isEmpty()) {
                        Text(
                            text = "Aún no se han recibido pagos para calcular la distribución.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            walletShares.forEach { share ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(share.appSource.badgeColorHex))
                                            )
                                            Text(
                                                text = share.appSource.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextMain
                                                )
                                            )
                                        }

                                        Text(
                                            text = "S/ ${String.format("%.2f", share.totalAmount)} (${share.percentage.toInt()}%)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextMain
                                            )
                                        )
                                    }

                                    // Barra de Progreso Suave
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFF3F4F6))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(share.percentage / 100f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(share.appSource.badgeColorHex))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
