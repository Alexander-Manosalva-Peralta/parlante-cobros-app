package com.parlantecobros.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.parlantecobros.app.data.PaymentRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CobrosNotificationListener : NotificationListenerService() {

    private lateinit var speechManager: SpeechManager

    // Lista blanca de paquetes financieros y billeteras autorizadas
    private val ALLOWED_PACKAGES = setOf(
        "com.bcp.innovacxion.yapeapp", // Yape
        "com.bcp.bank.bcp",            // BCP Banca Móvil
        "pe.com.interbank.mobilebanking", // Interbank (Plin)
        "com.bbva.bbvacontigo",        // BBVA (Plin)
        "pe.com.scotiabank.peru.pe",   // Scotiabank (Plin)
        "pe.com.scotiabank.peru",
        "pe.gob.bn.bancamovil",        // Banco de la Nación
        "com.tunki.wallet",            // Tunki
        "pe.com.agora",                // Agora
        "com.izipay.app",              // Izipay
        "pe.com.culqi"                 // Culqi
    )

    // Lista negra explícita de apps personales (WhatsApp, redes, mensajes, etc.)
    private val BLOCKED_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",            // WhatsApp Business
        "org.telegram.messenger",
        "com.facebook.orca",           // Messenger
        "com.facebook.katana",
        "com.instagram.android",
        "com.google.android.gm",       // Gmail
        "com.google.android.apps.messaging", // SMS
        "com.android.mms",
        "com.google.android.dialer",
        "com.android.server.telecom"
    )

    override fun onCreate() {
        super.onCreate()
        try {
            speechManager = SpeechManager(applicationContext)
            val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            PaymentRepository.recordRawNotification("[$time] Servicio iniciado y activo.")
            Log.d(TAG, "CobrosNotificationListener onCreate exitoso.")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate", e)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        PaymentRepository.recordRawNotification("[$time] Conectado exitosamente. Filtro financiero activo.")
        Log.d(TAG, "onListenerConnected: Android ha vinculado el servicio de notificaciones.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        PaymentRepository.recordRawNotification("[$time] Servicio desconectado temporalmente.")
        Log.w(TAG, "onListenerDisconnected: Android ha desvinculado el servicio.")
    }

    override fun onDestroy() {
        try {
            speechManager.shutdown()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        try {
            val packageName = sbn.packageName?.lowercase(Locale.ROOT) ?: ""
            
            // 1. FILTRO DE PRIVACIDAD ESTRICTO:
            // Si es nuestra propia app, WhatsApp, mensajes o redes sociales -> DESCARTAR INMEDIATAMENTE
            if (packageName == applicationContext.packageName.lowercase(Locale.ROOT) ||
                BLOCKED_PACKAGES.contains(packageName) ||
                packageName.contains("whatsapp") ||
                packageName.contains("telegram") ||
                packageName.contains("messenger") ||
                packageName.contains("instagram") ||
                packageName.contains("facebook") ||
                packageName.contains("messaging")
            ) {
                return
            }

            // 2. Solo permitir si pertenece a bancos o billeteras digitales
            val isFinancialApp = ALLOWED_PACKAGES.contains(packageName) ||
                    packageName.contains("yape") ||
                    packageName.contains("plin") ||
                    packageName.contains("bcp") ||
                    packageName.contains("interbank") ||
                    packageName.contains("bbva") ||
                    packageName.contains("scotiabank") ||
                    packageName.contains("bancamovil")

            if (!isFinancialApp) {
                // Descartar cualquier otra app que no sea de cobros
                return
            }

            // 3. Procesar ÚNICAMENTE las notificaciones financieras
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val ticker = notification.tickerText?.toString() ?: ""

            val combinedContent = if (bigText.isNotEmpty()) bigText else text

            val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            
            // Intentar parsear el cobro
            val payment = PaymentParser.parse(
                packageName = packageName,
                title = title,
                text = combinedContent,
                subText = subText,
                ticker = ticker
            )

            if (payment != null) {
                val settings = PaymentRepository.settings.value
                val isAppActive = settings.activeApps[payment.appSource] ?: true

                if (isAppActive) {
                    val isNew = PaymentRepository.addPayment(payment)
                    if (isNew) {
                        Log.d(TAG, "¡Cobro detectado! Anunciando: ${payment.appSource.displayName} S/ ${payment.amount}")
                        PaymentRepository.recordRawNotification("[$time] ¡COBRO ${payment.appSource.displayName}! S/ ${payment.amount} de ${payment.senderName}")
                        speechManager.speakPayment(payment, settings)
                    }
                }
            } else {
                // Registrar solo avisos del banco (ej. Yape, Plin) en el monitor
                PaymentRepository.recordRawNotification("[$time] [${packageName.takeLast(15)}] $title: $combinedContent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación", e)
        }
    }

    companion object {
        private const val TAG = "ParlanteCobrosListener"
    }
}
