package com.parlantecobros.app.service

import com.parlantecobros.app.model.AppSource
import com.parlantecobros.app.model.PaymentItem
import java.util.regex.Pattern

object PaymentParser {

    // Regex para detectar montos estándar: S/ 15.00, S/ 1,500.00, S/15, S/. 0.10, 0.10 soles, 10 céntimos
    private val AMOUNT_PATTERNS = listOf(
        // S/ 15.00 o S/ 1,500.00 o S/ 0.10 o S/10
        Pattern.compile("""(?:S\/\.?|S\s*\/\.?|Soles?)\s*([\d]{1,3}(?:[,\s]\d{3})*(?:\.\d{1,2})?|\d+(?:[\.,]\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        // 0.10 soles o 15.00 soles o 1,500 soles
        Pattern.compile("""([\d]{1,3}(?:[,\s]\d{3})*(?:\.\d{1,2})?|\d+(?:[\.,]\d{1,2})?)\s*(?:soles|sol|S\/\.?)""", Pattern.CASE_INSENSITIVE),
        // "10 céntimos" o "20 centimos" -> interpretado como céntimos
        Pattern.compile("""([\d]+)\s*c[eé]ntimos?""", Pattern.CASE_INSENSITIVE)
    )

    // Fallback para Yape: busca cualquier número decimal (ej. 0.10 o 5.00)
    private val YAPE_FALLBACK_AMOUNT = Pattern.compile("""(?:de|por|\$|S\/|\b)\s*([\d]+[\.,][\d]{2})\b""", Pattern.CASE_INSENSITIVE)

    // Regex para extraer el nombre del remitente
    private val SENDER_PATTERNS = listOf(
        // "¡Te yapearon! S/ 15.00 de Juan Pérez" o "Te llegó un Yape de Juan Perez por S/..."
        Pattern.compile("""(?:de)\s+([A-Za-zÁÉÍÓÚáéíóúñÑ\s]{3,35}?)(?:\s+por|\s+a\s+tu|\s+con|\s+el|\.|$|S\/)""", Pattern.CASE_INSENSITIVE),
        // "Juan Perez te envió un Yape..." o "Carlos te transfirió..." o "Juan Perez te yapeó..."
        Pattern.compile("""(?:^|[\.\!\?]\s*)([A-Za-zÁÉÍÓÚáéíóúñÑ\s]{3,35}?)\s+te\s+(?:envi[oó]|transfiri[oó]|yape[oó]|mand[oó]|pag[oó])""", Pattern.CASE_INSENSITIVE),
        // "¡Yape de Juan Perez!"
        Pattern.compile("""Yape\s+de\s+([A-Za-zÁÉÍÓÚáéíóúñÑ\s]{3,35}?)(?:!|\.|\s+por)""", Pattern.CASE_INSENSITIVE),
        // "¡Te yapearon! Juan te envió..."
        Pattern.compile("""(?:¡?Te yapearon!?)\s+([A-Za-zÁÉÍÓÚáéíóúñÑ\s]{3,35}?)\s+te\s+(?:envi[oó]|transfiri[oó]|yape[oó]|mand[oó]|pag[oó])""", Pattern.CASE_INSENSITIVE),
        // "Recibiste un Yape de Juan Perez"
        Pattern.compile("""recibiste\s+(?:un\s+yape\s+|un\s+plin\s+)?(?:de)\s+([A-Za-zÁÉÍÓÚáéíóúñÑ\s]{3,35}?)(?:\s+por|\.|$|S\/)""", Pattern.CASE_INSENSITIVE)
    )

    fun parse(
        packageName: String,
        title: String,
        text: String,
        subText: String = "",
        ticker: String = "",
        notificationKey: String = "",
        postTime: Long = 0L
    ): PaymentItem? {
        val fullContent = "$title. $text. $subText. $ticker".trim()

        // 1. Detectar si el texto o paquete pertenece a cobros/pagos
        val appSource = AppSource.fromPackageOrText(packageName, fullContent)

        // Verificamos si es una notificación legítima de cobro/ingreso
        val isPaymentReceived = isIncomingPayment(fullContent, packageName)
        if (!isPaymentReceived) return null

        // 2. Extraer el monto
        val amount = extractAmount(fullContent, packageName) ?: return null
        if (amount <= 0.0) return null

        // 3. Extraer el nombre de quien envía
        val sender = extractSender(fullContent, title)

        return PaymentItem(
            appSource = appSource,
            senderName = sender,
            amount = amount,
            currency = "S/",
            rawText = fullContent,
            notificationKey = notificationKey,
            postTime = postTime
        )
    }

    private fun isIncomingPayment(content: String, pkg: String): Boolean {
        val lower = content.lowercase(java.util.Locale.ROOT)
        val isYapeApp = pkg.contains("yape") || pkg.contains("innovacxion") || lower.contains("yape")

        // 1. Filtro estricto contra promociones, campañas, sorteos y marketing (ej: 'Gana 50 soles con Yape Hijos')
        // Usamos límites de palabras y frases específicas para no bloquear notas cotidianas de clientes (ej. 'compra', 'recarga')
        val promoPatterns = listOf(
            Regex("""\b(gana|ganar|ganaste)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(sorteo|sorteos|premio|premios)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(yape\s*hijos|yapehijos)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(préstamo|prestamo|crédito|credito)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(pide tu|solicita tu|te prestamos|abre tu cuenta|invita y gana)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(participa en|participa por|conoce los beneficios)\b""", RegexOption.IGNORE_CASE)
        )
        if (promoPatterns.any { it.containsMatchIn(lower) }) {
            return false
        }

        // 2. Filtro para ignorar notificaciones de egreso (pagos realizados por el propio usuario)
        val outgoingKeywords = listOf(
            "has pagado", "pagaste", "tu pago de", "compra aprobada",
            "código de aprobación", "codigo de aprobacion", "enviaste",
            "transferencia realizada", "transferencia enviada", "yapeaste", "plineaste"
        )
        if (outgoingKeywords.any { lower.contains(it) }) {
            return false
        }

        // 3. Palabras clave afirmativas de cobro/ingreso recibido
        val incomingKeywords = listOf(
            "te llegó", "te llego", "te enviaron", "te envió", "te envio",
            "te ha enviado", "te han enviado", "te transfirió", "te transfirio",
            "te transfirieron", "recibiste", "has recibido", "recibido",
            "abono", "depósito", "deposito", "transferencia recibida",
            "te yapeó", "te yapeo", "te yapearon", "te plineó", "te plineo",
            "te plinearon", "recibiste un yape", "recibiste un plin",
            "te pagó", "te pago", "te pagaron", "pago recibido", "cobro recibido",
            "te acaba de", "te mandó", "te mando", "nuevo pago", "nuevo yape",
            "confirmación de pago", "confirmacion de pago"
        )

        val hasIncomingKeyword = incomingKeywords.any { lower.contains(it) }

        // Si contiene palabras afirmativas de cobro, es un ingreso verificado
        if (hasIncomingKeyword) {
            return true
        }

        // Si proviene de la app oficial de Yape y no es egreso ni publicidad, es un cobro recibido
        if (isYapeApp) {
            return true
        }

        return false
    }

    private fun extractAmount(text: String, pkg: String): Double? {
        // Chequeo si dice específicamente "céntimos" (ej. "10 céntimos")
        val centMatcher = Pattern.compile("""([\d]+)\s*c[eé]ntimos?""", Pattern.CASE_INSENSITIVE).matcher(text)
        if (centMatcher.find()) {
            val cents = centMatcher.group(1)?.toDoubleOrNull()
            if (cents != null && cents > 0) {
                return cents / 100.0
            }
        }

        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val rawValue = matcher.group(1)?.replace(" ", "") ?: continue
                // Normalizar miles y decimales:
                val normalized = if (rawValue.contains(",") && rawValue.contains(".")) {
                    rawValue.replace(",", "")
                } else if (rawValue.contains(",")) {
                    val parts = rawValue.split(",")
                    if (parts.size == 2 && parts[1].length in 1..2) {
                        parts[0] + "." + parts[1]
                    } else {
                        rawValue.replace(",", "")
                    }
                } else {
                    rawValue
                }

                try {
                    val parsed = normalized.toDouble()
                    if (parsed > 0) return parsed
                } catch (_: Exception) {}
            }
        }

        // Si es de la app Yape directamente, intentamos fallback con cualquier número decimal
        if (pkg.contains("yape") || pkg.contains("innovacxion") || text.contains("yape", ignoreCase = true)) {
            val fallbackMatcher = YAPE_FALLBACK_AMOUNT.matcher(text)
            if (fallbackMatcher.find()) {
                val rawValue = fallbackMatcher.group(1)?.replace(",", ".") ?: ""
                val parsed = rawValue.toDoubleOrNull()
                if (parsed != null && parsed > 0) return parsed
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
