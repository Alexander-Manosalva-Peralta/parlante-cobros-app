package com.parlantecobros.app.service

import com.parlantecobros.app.model.AppSource
import com.parlantecobros.app.model.PaymentItem
import java.util.regex.Pattern

object PaymentParser {

    // Regex para detectar montos: S/ 15.00, S/15, S/. 20.50, 15.50 soles
    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:S\/\.?|S\s*\/\.?|Soles?)\s*([\d]+(?:[\.,][\d]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([\d]+(?:[\.,][\d]{2}))\s*(?:soles|S\/\.?)""", Pattern.CASE_INSENSITIVE)
    )

    // Regex para extraer el nombre del remitente
    private val SENDER_PATTERNS = listOf(
        // "Te llegó un Yape de Juan Perez por S/..."
        Pattern.compile("""de\s+([A-Za-zÁÉÍÓÚáéíóúñÑ\s]{3,35}?)(?:\s+por|\s+a\s+tu|\s+con|\s+el|\.|$|S\/)""", Pattern.CASE_INSENSITIVE),
        // "Juan Perez te envió un Yape..." o "Carlos te transfirió..."
        Pattern.compile("""^([A-Za-zÁÉÍÓÚáéíóúñÑ\s]{3,35}?)\s+te\s+(?:envi[oó]|transfiri[oó]|yape[oó])""", Pattern.CASE_INSENSITIVE),
        // "¡Yape de Juan Perez!"
        Pattern.compile("""Yape\s+de\s+([A-Za-zÁÉÍÓÚáéíóúñÑ\s]{3,35}?)(?:!|\.|\s+por)""", Pattern.CASE_INSENSITIVE)
    )

    fun parse(packageName: String, title: String, text: String, subText: String = ""): PaymentItem? {
        val fullContent = "$title. $text. $subText".trim()

        // 1. Detectar si el texto o paquete pertenece a cobros/pagos
        val appSource = AppSource.fromPackageOrText(packageName, fullContent)

        // Verificamos si parece una notificación de ingreso/cobro (no un débito o cobro que el usuario hizo a otro)
        val isPaymentReceived = isIncomingPayment(fullContent, packageName)
        if (!isPaymentReceived) return null

        // 2. Extraer el monto
        val amount = extractAmount(fullContent) ?: return null
        if (amount <= 0.0) return null

        // 3. Extraer el nombre de quien envía
        val sender = extractSender(fullContent, title)

        return PaymentItem(
            appSource = appSource,
            senderName = sender,
            amount = amount,
            currency = "S/",
            rawText = fullContent
        )
    }

    private fun isIncomingPayment(content: String, pkg: String): Boolean {
        val lower = content.lowercase()

        // Filtro para ignorar notificaciones de pagos que uno mismo hizo (egreso) o promociones
        val isOutgoing = lower.contains("has pagado") ||
                lower.contains("pagaste") ||
                lower.contains("tu pago de") ||
                lower.contains("compra aprobada") ||
                lower.contains("promoción") ||
                lower.contains("descuento") ||
                lower.contains("código de aprobación")

        if (isOutgoing) return false

        // Palabras clave de cobro/ingreso
        val incomingKeywords = listOf(
            "te llegó", "te enviaron", "te envió", "te envio", "te transfirió",
            "te transfirio", "recibiste", "abono", "transferencia recibida",
            "yapeaste con éxito", "te yapeó", "te yapearon", "te plineó",
            "te plinearon", "recibiste un yape", "recibiste un plin"
        )

        val hasIncomingKeyword = incomingKeywords.any { lower.contains(it) }

        // Si es la app de Yape directamente, casi todas las notificaciones recibidas son yapeos
        val isYapeApp = pkg.contains("yape") || lower.contains("yape")

        return hasIncomingKeyword || isYapeApp
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val rawValue = matcher.group(1)?.replace(",", ".") ?: continue
                try {
                    return rawValue.toDouble()
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun extractSender(content: String, title: String): String {
        for (pattern in SENDER_PATTERNS) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val match = matcher.group(1)?.trim() ?: ""
                val cleaned = cleanSenderName(match)
                if (cleaned.isNotEmpty()) {
                    return cleaned
                }
            }
        }

        // Si el título parece un nombre de persona (ej. "Carlos Rodriguez")
        val cleanTitle = cleanSenderName(title)
        if (cleanTitle.length in 3..30 && !cleanTitle.contains("Yape", ignoreCase = true) && !cleanTitle.contains("Plin", ignoreCase = true)) {
            return cleanTitle
        }

        return "Cliente"
    }

    private fun cleanSenderName(name: String): String {
        return name
            .replace(Regex("""(?i)\b(un|el|la|por|de|en|tu|yape|plin|soles)\b"""), "")
            .replace(Regex("""[^A-Za-zÁÉÍÓÚáéíóúñÑ\s]"""), "")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
