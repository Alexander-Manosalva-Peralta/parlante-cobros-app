package com.vexum.punk.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexum.punk.R
import androidx.compose.foundation.BorderStroke
import com.vexum.punk.data.HistoryFilter
import com.vexum.punk.data.PaymentRepository
import com.vexum.punk.data.SessionManager
import com.vexum.punk.model.AppSource
import com.vexum.punk.model.PaymentItem
import com.vexum.punk.ui.theme.*
import java.util.Calendar

enum class BottomTab(val label: String) {
    INICIO("Caja"),
    ANALYTICS("BI Métricas"),
    AJUSTES("Ajustes")
}

@Composable
fun HomeScreen(
    isNotificationPermissionGranted: Boolean,
    isBatteryOptimizationIgnored: Boolean = true,
    onRequestPermission: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit = {},
    onOpenAppSettings: () -> Unit,
    onRebindService: () -> Unit,
    onTestVoice: (String) -> Unit,
    onLogout: () -> Unit = {}
) {
    val payments by PaymentRepository.payments.collectAsState()
    val settings by PaymentRepository.settings.collectAsState()

    var selectedTab by remember { mutableStateOf(BottomTab.INICIO) }

    // Saludo dinámico según la hora del día (sin emojis)
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBackground)
    ) {
        // Contenido según la pestaña seleccionada
        when (selectedTab) {
            BottomTab.INICIO -> {
                CashierContent(
                    greeting = greeting,
                    payments = payments,
                    settings = settings,
                    isNotificationPermissionGranted = isNotificationPermissionGranted,
                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                    onRequestPermission = onRequestPermission,
                    onRequestIgnoreBatteryOptimization = onRequestIgnoreBatteryOptimization,
                    onTestVoice = onTestVoice
                )
            }
            BottomTab.ANALYTICS -> {
                AnalyticsScreen()
            }
            BottomTab.AJUSTES -> {
                SettingsContent(
                    settings = settings,
                    onOpenAppSettings = onOpenAppSettings,
                    onRebindService = onRebindService,
                    onTestVoice = onTestVoice,
                    onLogout = onLogout
                )
            }
        }

        // Barra de Navegación Inferior Flotante (Floating Dock estilo Image 3)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(32.dp)),
                color = LuxurySurface,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockItem(
                        selected = selectedTab == BottomTab.INICIO,
                        icon = Icons.Outlined.Home,
                        label = "Caja",
                        onClick = { selectedTab = BottomTab.INICIO }
                    )

                    DockItem(
                        selected = selectedTab == BottomTab.ANALYTICS,
                        icon = Icons.Outlined.BarChart,
                        label = "BI & Métricas",
                        onClick = { selectedTab = BottomTab.ANALYTICS }
                    )

                    DockItem(
                        selected = selectedTab == BottomTab.AJUSTES,
                        icon = Icons.Outlined.Tune,
                        label = "Ajustes",
                        onClick = { selectedTab = BottomTab.AJUSTES }
                    )
                }
            }
        }
    }
}

@Composable
fun DockItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val bg = if (selected) ObsidianDark else Color.Transparent
    val contentColor = if (selected) Color.White else TextMuted

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        if (selected) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            )
        }
    }
}

@Composable
fun CashierContent(
    greeting: String,
    payments: List<PaymentItem>,
    settings: com.vexum.punk.model.AppSettings,
    isNotificationPermissionGranted: Boolean,
    isBatteryOptimizationIgnored: Boolean = true,
    onRequestPermission: () -> Unit,
    onRequestIgnoreBatteryOptimization: () -> Unit = {},
    onTestVoice: (String) -> Unit
) {
    val selectedFilter by PaymentRepository.selectedFilter.collectAsState()
    val filteredPayments = PaymentRepository.getPayments(selectedFilter)
    val totalAmount = PaymentRepository.getTotalAmount(selectedFilter)
    val count = PaymentRepository.getTotalCount(selectedFilter)
    var isAmountVisible by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Barra Superior con Logo punk y Saludo Dinámico
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Logo P de Punk oficial
                    Image(
                        painter = painterResource(id = R.drawable.logo_p_solo),
                        contentDescription = "Punk Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                    )

                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Punk Cobros",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMain,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                }

                // Indicador de Estado Discreto y Ejecutivo (Estilo Fintech)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LuxurySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (settings.speakerEnabled) Color(0xFF10B981) else Color(0xFF9CA3AF))
                        )
                        Text(
                            text = if (settings.speakerEnabled) "Audio POS Activo" else "Audio Pausado",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextMain,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // 2. Banner de Permiso
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
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permiso de Notificaciones",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMain
                                )
                            )
                            Text(
                                text = "Actívalo para que punk escuche tus cobros.",
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

        // 2.1. Banner de Optimización de Batería (Para funcionamiento con pantalla apagada)
        if (!isBatteryOptimizationIgnored) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pantalla Apagada",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMain
                                )
                            )
                            Text(
                                text = "Permite funcionamiento sin restricciones para escuchar cobros con el celular bloqueado.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                        Button(
                            onClick = onRequestIgnoreBatteryOptimization,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Permitir", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }

        // 3. Tarjeta Hero Obsidian
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
                            text = "TOTAL COBRADO (${selectedFilter.label.uppercase()})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9CA3AF),
                                letterSpacing = 1.2.sp
                            )
                        )

                        IconButton(
                            onClick = { isAmountVisible = !isAmountVisible },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isAmountVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isAmountVisible) String.format("S/ %.2f", totalAmount) else "S/ ••••••",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-1.2).sp
                        )
                    )

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
                                text = "$count cobros",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFD1D5DB),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        if (count > 0) {
                            val avg = totalAmount / count
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1F242C))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = String.format("Prom. S/ %.2f", avg),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    Divider(color = ObsidianBorder, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Altavoz Inteligente",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Anunciar cobros en voz alta",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF))
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

        // 4. Billeteras Conectadas
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Billeteras Conectadas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val apps = listOf(
                        Triple("Yape", YapeBrand, "Activo"),
                        Triple("Plin", PlinBrand, "Activo"),
                        Triple("BCP", BcpBrand, "Activo"),
                        Triple("Interbank", InterbankBrand, "Activo"),
                        Triple("BBVA", BbvaBrand, "Activo")
                    )

                    items(apps) { (name, color, _) ->
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

        // 5. Historial de Transacciones con Filtro por Fechas
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial de Cobros",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                    )
                }

                // Selector de Período Estilo Apple (Hoy | Esta Semana | Este Mes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LuxurySurface)
                        .border(1.dp, LuxuryBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HistoryFilter.values().forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ObsidianDark else Color.Transparent)
                                .clickable { PaymentRepository.setFilter(filter) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }

        if (filteredPayments.isEmpty()) {
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
                            text = "Sin cobros en este período",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextMain
                            )
                        )
                        Text(
                            text = "Cuando te yapeen o transfieran, Punk anunciará el monto por altavoz y se guardará automáticamente.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            // Agrupación de cobros por fecha (Hoy, Ayer o Fecha)
            val groupedPayments = filteredPayments.groupBy { item ->
                formatDateGroup(item.timestamp)
            }

            groupedPayments.forEach { (dateHeader, itemsInDate) ->
                item {
                    Text(
                        text = dateHeader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                items(itemsInDate, key = { it.id }) { item ->
                    LuxuryTransactionCard(payment = item)
                }
            }
        }
    }
}

@Composable
fun SettingsContent(
    settings: com.vexum.punk.model.AppSettings,
    onOpenAppSettings: () -> Unit,
    onRebindService: () -> Unit,
    onTestVoice: (String) -> Unit,
    onLogout: () -> Unit = {}
) {
    val session by SessionManager.sessionState.collectAsState()
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "Control de voz, sistema y comercio",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }

        // 1. CUENTA Y COMERCIO (Estilo Inset Grouped de Apple)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CUENTA Y COMERCIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LuxurySurface,
                    border = BorderStroke(1.dp, LuxuryBorder)
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
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = session.merchantName.ifEmpty { "Comercio Autorizado" },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextMain
                                    )
                                )
                                Text(
                                    text = session.userEmail.ifEmpty { "comercio@punk.com" },
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                        }

                        TextButton(
                            onClick = onLogout,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Salir",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEF4444)
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. PREFERENCIAS DE AUDIO (Agrupadas sin múltiples tarjetas)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PREFERENCIAS DE AUDIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LuxurySurface,
                    border = BorderStroke(1.dp, LuxuryBorder)
                ) {
                    Column {
                        // Fila: Mencionar nombre
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mencionar nombre de quien paga",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMain
                                    )
                                )
                                Text(
                                    text = "Ej: '...de Juan Pérez'",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                            Switch(
                                checked = settings.mentionCustomerName,
                                onCheckedChange = { checked ->
                                    PaymentRepository.updateSettings { it.copy(mentionCustomerName = checked) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldGreen
                                )
                            )
                        }

                        Divider(color = LuxuryBorder, thickness = 0.6.dp)

                        // Fila: Campanilla
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Campanilla previa (Chime)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMain
                                    )
                                )
                                Text(
                                    text = "Tono suave antes del anuncio",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                            Switch(
                                checked = settings.chimeBeforeSpeaking,
                                onCheckedChange = { checked ->
                                    PaymentRepository.updateSettings { it.copy(chimeBeforeSpeaking = checked) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldGreen
                                )
                            )
                        }

                        Divider(color = LuxuryBorder, thickness = 0.6.dp)

                        // Fila: Slider de Volumen
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Volumen de Voz",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMain
                                    )
                                )
                                Text(
                                    text = "${(settings.speechVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                )
                            }
                            Slider(
                                value = settings.speechVolume,
                                onValueChange = { vol ->
                                    PaymentRepository.updateSettings { it.copy(speechVolume = vol) }
                                },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = EmeraldGreen,
                                    activeTrackColor = EmeraldGreen,
                                    inactiveTrackColor = LuxuryBorder
                                )
                            )
                        }

                        Divider(color = LuxuryBorder, thickness = 0.6.dp)

                        // Fila de Acción: Probar Altavoz
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTestVoice("") }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VolumeUp,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Probar Altavoz de Cobro",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMain
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. SISTEMA Y SEGUNDO PLANO 24/7
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SISTEMA Y SEGUNDO PLANO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LuxurySurface,
                    border = BorderStroke(1.dp, LuxuryBorder)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRebindService() }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    tint = TextMain,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Reconectar Servicio del Sistema",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextMain
                                        )
                                    )
                                    Text(
                                        text = "Mantiene la vinculación activa 24/7",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Divider(color = LuxuryBorder, thickness = 0.6.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAppSettings() }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = TextMain,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Ajustes de Batería y Arranque",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextMain
                                        )
                                    )
                                    Text(
                                        text = "Quitar restricciones de segundo plano",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. LEGAL Y PRIVACIDAD
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LEGAL Y PRIVACIDAD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LuxurySurface,
                    border = BorderStroke(1.dp, LuxuryBorder)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    uriHandler.openUri("https://punk-admin-dashboard-qgmq.vercel.app/privacidad")
                                }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Policy,
                                    contentDescription = null,
                                    tint = TextMain,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Políticas de Privacidad",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMain
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Divider(color = LuxuryBorder, thickness = 0.6.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Lectura 100% local en tu teléfono. Sin acceso a chats personales ni claves bancarias.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }
                }
            }
        }

        // 5. AUTORÍA Y MARCA VEXUM
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Punk Cobros • Versión 1.2.3",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Desarrollado por Vexum",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
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

fun formatDateGroup(timestamp: Long): String {
    val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        itemCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        itemCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> "Hoy"

        itemCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
        itemCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Ayer"

        else -> java.text.SimpleDateFormat("d 'de' MMMM", java.util.Locale("es", "PE")).format(java.util.Date(timestamp))
    }
}


