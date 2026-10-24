package com.vexum.punk.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.vexum.punk.model.AppSource
import com.vexum.punk.model.AppSettings
import com.vexum.punk.model.PaymentItem
import com.vexum.punk.model.SpeechTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

data class WalletShare(
    val appSource: AppSource,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float
)

data class TimeSlotStat(
    val label: String,      // "Día", "Tarde", "Noche"
    val totalAmount: Double,
    val count: Int
)

enum class HistoryFilter(val label: String) {
    HOY("Hoy"),
    SEMANA("Esta Semana"),
    MES("Este Mes")
}

object PaymentRepository {

    private const val TAG = "PaymentRepository"
    private const val PREFS_NAME = "punk_settings_prefs"
    private const val HISTORY_FILENAME = "punk_payments_history.json"

    private const val KEY_SPEAKER_ENABLED = "key_speaker_enabled"
    private const val KEY_MENTION_NAME = "key_mention_name"
    private const val KEY_CHIME = "key_chime"
    private const val KEY_PITCH = "key_pitch"
    private const val KEY_RATE = "key_rate"
    private const val KEY_VOLUME = "key_volume"
    private const val KEY_TEMPLATE = "key_template"

    private var prefs: SharedPreferences? = null
    private var historyFile: File? = null

    private val _payments = MutableStateFlow<List<PaymentItem>>(emptyList())
    val payments: StateFlow<List<PaymentItem>> = _payments.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HistoryFilter.HOY)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _lastCapturedNotification = MutableStateFlow("A la espera de notificaciones...")
    val lastCapturedNotification: StateFlow<String> = _lastCapturedNotification.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        historyFile = File(context.filesDir, HISTORY_FILENAME)

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

        // Cargar historial persistente desde el almacenamiento local del teléfono
        val loaded = loadPaymentsFromDisk()
        _payments.value = loaded
        Log.d(TAG, "Historial persistente cargado con éxito: ${loaded.size} pagos guardados localmente.")
    }

    fun setFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
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
    private val recentDeduplicationCache = mutableMapOf<String, Long>()

    fun addPayment(payment: PaymentItem): Boolean {
        val deduplicationKey = if (payment.notificationKey.isNotEmpty() && payment.postTime > 0) {
            "${payment.notificationKey}_${payment.postTime}"
        } else {
            "${payment.appSource}_${payment.amount}_${payment.senderName}_${payment.rawText.hashCode()}"
        }

        val now = System.currentTimeMillis()
        val lastSeen = recentDeduplicationCache[deduplicationKey] ?: 0L

        // Ventana de 1500ms para actualizaciones idénticas del sistema
        if (lastSeen > 0 && (now - lastSeen < 1500)) {
            return false
        }

        recentDeduplicationCache[deduplicationKey] = now
        recentDeduplicationCache.entries.removeIf { now - it.value > 30_000 }

        // Agregar a la lista y limitar a 2000 pagos locales para optimización de memoria
        _payments.update { current ->
            val updated = (listOf(payment) + current).take(2000)
            persistPaymentsToDisk(updated)
            updated
        }
        return true
    }

    private fun persistPaymentsToDisk(list: List<PaymentItem>) {
        val file = historyFile ?: return
        try {
            val array = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("appSource", item.appSource.name)
                    put("senderName", item.senderName)
                    put("amount", item.amount)
                    put("currency", item.currency)
                    put("rawText", item.rawText)
                    put("timestamp", item.timestamp)
                    put("notificationKey", item.notificationKey)
                    put("postTime", item.postTime)
                }
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error persistiendo pagos en disco", e)
        }
    }

    private fun loadPaymentsFromDisk(): List<PaymentItem> {
        val file = historyFile ?: return emptyList()
        if (!file.exists()) return emptyList()

        return try {
            val content = file.readText()
            if (content.isBlank()) return emptyList()
            val array = JSONArray(content)
            // Límite de 30 días para no consumir memoria del teléfono innecesariamente
            val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            val list = mutableListOf<PaymentItem>()

            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                val ts = json.optLong("timestamp", System.currentTimeMillis())
                if (ts >= cutoff) {
                    val appSourceStr = json.optString("appSource", AppSource.GENERICO.name)
                    val appSource = try { AppSource.valueOf(appSourceStr) } catch (_: Exception) { AppSource.GENERICO }
                    list.add(
                        PaymentItem(
                            id = json.optString("id", ts.toString()),
                            appSource = appSource,
                            senderName = json.optString("senderName", "Cliente"),
                            amount = json.optDouble("amount", 0.0),
                            currency = json.optString("currency", "S/"),
                            rawText = json.optString("rawText", ""),
                            timestamp = ts,
                            notificationKey = json.optString("notificationKey", ""),
                            postTime = json.optLong("postTime", 0L)
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo historial persistente", e)
            emptyList()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.update { current ->
            val updated = transform(current)
            persistSettings(updated)
            updated
        }
    }

    // Filtrado de pagos según período seleccionado (Hoy, Semana, Mes)
    fun getPayments(filter: HistoryFilter): List<PaymentItem> {
        val list = _payments.value
        val now = Calendar.getInstance()
        val itemCal = Calendar.getInstance()

        return when (filter) {
            HistoryFilter.HOY -> {
                list.filter { item ->
                    itemCal.timeInMillis = item.timestamp
                    itemCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    itemCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                }
            }
            HistoryFilter.SEMANA -> {
                val startOfWeek = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                list.filter { it.timestamp >= startOfWeek }
            }
            HistoryFilter.MES -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                list.filter { it.timestamp >= startOfMonth }
            }
        }
    }

    fun getTotalAmount(filter: HistoryFilter): Double = getPayments(filter).sumOf { it.amount }
    fun getTotalCount(filter: HistoryFilter): Int = getPayments(filter).size
    fun getAverageTicket(filter: HistoryFilter): Double {
        val count = getTotalCount(filter)
        return if (count > 0) getTotalAmount(filter) / count else 0.0
    }
    fun getMaxPayment(filter: HistoryFilter): PaymentItem? = getPayments(filter).maxByOrNull { it.amount }

    val totalAmountToday: Double
        get() = getTotalAmount(HistoryFilter.HOY)

    val totalCountToday: Int
        get() = getTotalCount(HistoryFilter.HOY)

    val averageTicket: Double
        get() = getAverageTicket(HistoryFilter.HOY)

    val maxPayment: PaymentItem?
        get() = getMaxPayment(HistoryFilter.HOY)

    // BI: Distribución por Billetera según el filtro activo
    fun getWalletShares(filter: HistoryFilter): List<WalletShare> {
        val list = getPayments(filter)
        val total = getTotalAmount(filter)
        if (list.isEmpty() || total <= 0.0) return emptyList()

        return list.groupBy { it.appSource }
            .map { (source, items) ->
                val sum = items.sumOf { it.amount }
                val pct = (sum / total).toFloat() * 100f
                WalletShare(source, sum, items.size, pct)
            }
            .sortedByDescending { it.totalAmount }
    }

    val walletShares: List<WalletShare>
        get() = getWalletShares(_selectedFilter.value)

    // BI: 3 Franjas Horarias: Día, Tarde, Noche (Horas configuradas en código, sin mostrarlas en la UI)
    fun getTimeSlotStats(filter: HistoryFilter): List<TimeSlotStat> {
        val list = getPayments(filter)
        val cal = Calendar.getInstance()

        var diaAmount = 0.0
        var diaCount = 0
        var tardeAmount = 0.0
        var tardeCount = 0
        var nocheAmount = 0.0
        var nocheCount = 0

        for (item in list) {
            cal.timeInMillis = item.timestamp
            when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 6..13 -> { // Día: 06:00 a 13:59 (6 AM a 2 PM)
                    diaAmount += item.amount
                    diaCount++
                }
                in 14..18 -> { // Tarde: 14:00 a 18:59 (2 PM a 7 PM)
                    tardeAmount += item.amount
                    tardeCount++
                }
                else -> { // Noche: 19:00 a 05:59 (7 PM a 6 AM)
                    nocheAmount += item.amount
                    nocheCount++
                }
            }
        }

        return listOf(
            TimeSlotStat("Día", diaAmount, diaCount),
            TimeSlotStat("Tarde", tardeAmount, tardeCount),
            TimeSlotStat("Noche", nocheAmount, nocheCount)
        )
    }

    val timeSlotStats: List<TimeSlotStat>
        get() = getTimeSlotStats(_selectedFilter.value)

    val peakHourSlot: TimeSlotStat?
        get() = timeSlotStats.filter { it.totalAmount > 0 }.maxByOrNull { it.totalAmount }
}
