package com.parlantecobros.app.data

import com.parlantecobros.app.model.AppSource
import com.parlantecobros.app.model.AppSettings
import com.parlantecobros.app.model.PaymentItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object PaymentRepository {

    private val _payments = MutableStateFlow<List<PaymentItem>>(emptyList())
    val payments: StateFlow<List<PaymentItem>> = _payments.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Cache para evitar notificaciones duplicadas (dentro de 10 segundos)
    private val recentDeduplicationCache = mutableMapOf<String, Long>()

    fun addPayment(payment: PaymentItem): Boolean {
        val deduplicationKey = "${payment.appSource}_${payment.amount}_${payment.senderName}"
        val now = System.currentTimeMillis()
        val lastSeen = recentDeduplicationCache[deduplicationKey] ?: 0L

        if (now - lastSeen < 10_000) {
            // Duplicado ignorado
            return false
        }

        recentDeduplicationCache[deduplicationKey] = now
        // Limpiar cache viejo
        recentDeduplicationCache.entries.removeIf { now - it.value > 60_000 }

        _payments.update { current ->
            listOf(payment) + current.take(99) // Guardar los últimos 100
        }
        return true
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.update(transform)
    }

    fun clearHistory() {
        _payments.value = emptyList()
    }

    val totalAmountToday: Double
        get() = _payments.value.sumOf { it.amount }

    val totalCountToday: Int
        get() = _payments.value.size
}
