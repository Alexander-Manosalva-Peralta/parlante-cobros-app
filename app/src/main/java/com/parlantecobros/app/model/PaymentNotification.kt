package com.parlantecobros.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppSource(
    val displayName: String,
    val packageNameKeywords: List<String>,
    val badgeColorHex: Long
) {
    YAPE("Yape", listOf("yape", "innovacxion"), 0xFF732282),
    PLIN("Plin", listOf("plin"), 0xFF00A3E0),
    BCP("BCP", listOf("bcp", "bancocredito"), 0xFF002A8F),
    INTERBANK("Interbank", listOf("interbank"), 0xFF009B3A),
    BBVA("BBVA", listOf("bbva"), 0xFF004481),
    SCOTIABANK("Scotiabank", listOf("scotiabank"), 0xFFED1111),
    BANCO_NACION("Banco de la Nación", listOf("bn.bancamovil", "bancodelanacion"), 0xFFC8102E),
    GENERICO("Cobro Bancario", emptyList(), 0xFF0071E3);

    companion object {
        fun fromPackageOrText(pkg: String, text: String): AppSource {
            val lowerPkg = pkg.lowercase(Locale.ROOT)
            val lowerText = text.lowercase(Locale.ROOT)

            return when {
                lowerPkg.contains("yape") || lowerText.contains("yape") -> YAPE
                lowerText.contains("plin") -> PLIN
                lowerPkg.contains("interbank") || lowerText.contains("interbank") -> INTERBANK
                lowerPkg.contains("bbva") || lowerText.contains("bbva") -> BBVA
                lowerPkg.contains("bcp") || lowerText.contains("bcp") -> BCP
                lowerPkg.contains("scotiabank") || lowerText.contains("scotiabank") -> SCOTIABANK
                lowerPkg.contains("bn.bancamovil") || lowerText.contains("banco de la naci") -> BANCO_NACION
                else -> GENERICO
            }
        }
    }
}

data class PaymentItem(
    val id: String = System.currentTimeMillis().toString(),
    val appSource: AppSource,
    val senderName: String,
    val amount: Double,
    val currency: String = "S/",
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedAmount: String
        get() = String.format(Locale.US, "%s %.2f", currency, amount)

    val spokenAmountDescription: String
        get() {
            val soles = amount.toInt()
            val centavos = Math.round((amount - soles) * 100).toInt()
            return when {
                soles == 0 && centavos > 0 -> "$centavos céntimos"
                centavos > 0 -> "$soles soles con $centavos céntimos"
                else -> "$soles soles"
            }
        }
}

enum class SpeechTemplate(val label: String) {
    COMPLETO("Completo: App, Monto y Cliente"),
    CORTO("Corto: App y Monto"),
    SOLO_MONTO("Solo Monto")
}

data class AppSettings(
    val speakerEnabled: Boolean = true,
    val mentionCustomerName: Boolean = true,
    val speechTemplate: SpeechTemplate = SpeechTemplate.COMPLETO,
    val chimeBeforeSpeaking: Boolean = true,
    val speechPitch: Float = 1.0f,
    val speechRate: Float = 0.95f, // Ligeramente pausado para claridad
    val activeApps: Map<AppSource, Boolean> = AppSource.values().associateWith { true }
)
