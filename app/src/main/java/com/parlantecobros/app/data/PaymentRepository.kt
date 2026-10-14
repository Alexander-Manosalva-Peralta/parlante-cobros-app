package com.parlantecobros.app.data

import android.content.Context
import android.content.SharedPreferences
import com.parlantecobros.app.model.AppSource
import com.parlantecobros.app.model.AppSettings
import com.parlantecobros.app.model.PaymentItem
import com.parlantecobros.app.model.SpeechTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

data class WalletShare(
    val appSource: AppSource,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float
)

data class TimeSlotStat(
    val label: String,
    val hourStart: Int,
    val hourEnd: Int,
    val totalAmount: Double,
    val count: Int
)

object PaymentRepository {

    private const val PREFS_NAME = "punk_settings_prefs"
    private const val KEY_SPEAKER_ENABLED = "key_speaker_enabled"
    private const val KEY_MENTION_NAME = "key_mention_name"
    private const val KEY_CHIME = "key_chime"
    private const val KEY_PITCH = "key_pitch"
    private const val KEY_RATE = "key_rate"
    private const val KEY_VOLUME = "key_volume"
    private const val KEY_TEMPLATE = "key_template"

    private var prefs: SharedPreferences? = null

    private val _payments = MutableStateFlow<List<PaymentItem>>(emptyList())
    val payments: StateFlow<List<PaymentItem>> = _payments.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _lastCapturedNotification = MutableStateFlow<String>("A la espera de notificaciones...")
    val lastCapturedNotification: StateFlow<String> = _lastCapturedNotification.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val speaker = sp.getBoolean(KEY_SPEAKER_ENABLED, true)
        val mentionName = sp.getBoolean(KEY_MENTION_NAME, true)
        val chime = sp.getBoolean(KEY_CHIME, true)
        val pitch = sp.getFloat(KEY_PITCH, 1.0f)
        val rate = sp.getFloat(KEY_RATE, 0.95f)
        val volume = sp.getFloat(KEY_VOLUME, 1.0f)
        val templateName = sp.getString(KEY_TEMPLATE, SpeechTemplate.COMPLETO.name) ?: SpeechTemplate.COMPLETO.name
        val template = try { SpeechTemplate.valueOf(templateName) } catch (_: Exception) { SpeechTemplate.COMPLETO }

        _settings.value = AppSettings(
            speakerEnabled = speaker,
            mentionCustomerName = mentionName,
            speechTemplate = template,
            chimeBeforeSpeaking = chime,
            speechPitch = pitch,
            speechRate = rate,
            speechVolume = volume
        )
    }

    private fun persistSettings(s: AppSettings) {
        prefs?.edit()?.apply {
            putBoolean(KEY_SPEAKER_ENABLED, s.speakerEnabled)
            putBoolean(KEY_MENTION_NAME, s.mentionCustomerName)
            putBoolean(KEY_CHIME, s.chimeBeforeSpeaking)
            putFloat(KEY_PITCH, s.speechPitch)
            putFloat(KEY_RATE, s.speechRate)
            putFloat(KEY_VOLUME, s.speechVolume)
            putString(KEY_TEMPLATE, s.speechTemplate.name)
            apply()
        }
    }

    fun recordRawNotification(info: String) {
        _lastCapturedNotification.value = info
    }

    // Algoritmo bancario de deduplicación de alta concurrencia
    // Permite pagos sucesivos rápidos (ej. 3 Yapes en segundos) y solo filtra retransmisiones del SO
    private val recentDeduplicationCache = mutableMapOf<String, Long>()

    fun addPayment(payment: PaymentItem): Boolean {
        val deduplicationKey = if (payment.notificationKey.isNotEmpty() && payment.postTime > 0) {
            "${payment.notificationKey}_${payment.postTime}"
        } else {
            "${payment.appSource}_${payment.amount}_${payment.senderName}_${payment.rawText.hashCode()}"
        }

        val now = System.currentTimeMillis()
        val lastSeen = recentDeduplicationCache[deduplicationKey] ?: 0L

        // Ventana estricta de solo 1500ms para actualizaciones idénticas del sistema
        if (lastSeen > 0 && (now - lastSeen < 1500)) {
            return false
        }

        recentDeduplicationCache[deduplicationKey] = now
        recentDeduplicationCache.entries.removeIf { now - it.value > 30_000 }

        _payments.update { current ->
            listOf(payment) + current.take(99)
        }
        return true
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.update { current ->
            val updated = transform(current)
            persistSettings(updated)
            updated
        }
    }

    fun clearHistory() {
        _payments.value = emptyList()
    }

    val totalAmountToday: Double
        get() = _payments.value.sumOf { it.amount }

    val totalCountToday: Int
        get() = _payments.value.size

    val averageTicket: Double
        get() = if (totalCountToday > 0) totalAmountToday / totalCountToday else 0.0

    val maxPayment: PaymentItem?
        get() = _payments.value.maxByOrNull { it.amount }

    // BI: Distribución por Billetera (Yape, Plin, BCP, etc.)
    val walletShares: List<WalletShare>
        get() {
            val list = _payments.value
            val total = totalAmountToday
            if (list.isEmpty() || total <= 0.0) return emptyList()

            return list.groupBy { it.appSource }
                .map { (source, items) ->
                    val sum = items.sumOf { it.amount }
                    val pct = (sum / total).toFloat() * 100f
                    WalletShare(source, sum, items.size, pct)
                }
                .sortedByDescending { it.totalAmount }
        }

    // BI: Ventas por franja horaria (Mañana, Almuerzo, Tarde, Noche)
    val timeSlotStats: List<TimeSlotStat>
        get() {
            val list = _payments.value
            val slots = listOf(
                TimeSlotStat("Mañana (6-12h)", 6, 12, 0.0, 0),
                TimeSlotStat("Almuerzo (12-15h)", 12, 15, 0.0, 0),
                TimeSlotStat("Tarde (15-19h)", 15, 19, 0.0, 0),
                TimeSlotStat("Noche (19-24h)", 19, 24, 0.0, 0)
            )

            val cal = Calendar.getInstance()
            return slots.map { slot ->
                val matching = list.filter { item ->
                    cal.timeInMillis = item.timestamp
                    val h = cal.get(Calendar.HOUR_OF_DAY)
                    h >= slot.hourStart && h < slot.hourEnd
                }
                slot.copy(
                    totalAmount = matching.sumOf { it.amount },
                    count = matching.size
                )
            }
        }

    // Franja horaria pico
    val peakHourSlot: TimeSlotStat?
        get() = timeSlotStats.filter { it.totalAmount > 0 }.maxByOrNull { it.totalAmount }
}
