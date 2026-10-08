package com.parlantecobros.app.service

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.parlantecobros.app.data.PaymentRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CobrosNotificationListener : NotificationListenerService() {

    private lateinit var speechManager: SpeechManager

    // Lista blanca de paquetes financieros y billeteras autorizadas en Perú
    private val ALLOWED_PACKAGES = setOf(
        "com.bcp.innovacxion.yapeapp", // Yape
        "com.bcp.innovacxion",
        "com.bcp.bank.bcp",            // BCP Banca Móvil
        "pe.com.interbank.mobilebanking", // Interbank (Plin)
        "com.bbva.bbvacontigo",        // BBVA (Plin)
        "pe.com.scotiabank.peru.pe",   // Scotiabank (Plin)
        "pe.com.scotiabank.peru",
        "pe.gob.bn.bancamovil",        // Banco de la Nación
        "com.tunki.wallet",            // Tunki
        "pe.com.agora",                // Agora
        "com.izipay.app",              // Izipay
        "pe.com.culqi",                // Culqi
        "pe.com.pagosdigitalesperuanos.bim", // Bim
        "pe.com.cajaarequipa.cajaarequipamovil", // Caja Arequipa
        "pe.com.cajahuancayo.appcajahuancayo",   // Caja Huancayo
        "pe.com.cajapiura.cajapiuramovil",       // Caja Piura
        "com.banbif.bancamovil",                 // BanBif
        "pe.com.pichincha.bancamovil"             // Banco Pichincha
    )

    // Lista negra estricta de apps personales y de mensajería (100% de privacidad garantizada)
    private val BLOCKED_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",            // WhatsApp Business
        "org.telegram.messenger",
        "com.facebook.orca",           // Messenger
        "com.facebook.katana",
        "com.instagram.android",
        "com.zhiliaoapp.musically",    // TikTok
        "com.twitter.android",
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
            PunkKeepAliveService.start(applicationContext)
            val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            PaymentRepository.recordRawNotification("[$time] Servicio iniciado y activo.")
            Log.d(TAG, "CobrosNotificationListener onCreate exitoso.")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PunkKeepAliveService.start(applicationContext)
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        PunkKeepAliveService.start(applicationContext)
        val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        PaymentRepository.recordRawNotification("[$time] Conectado exitosamente. Filtro financiero activo.")
        Log.d(TAG, "onListenerConnected: Android ha vinculado el servicio de notificaciones.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        PaymentRepository.recordRawNotification("[$time] Reconectando servicio...")
        Log.w(TAG, "onListenerDisconnected detectado, solicitando reconexión inmediata a Android...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, CobrosNotificationListener::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error en requestRebind", e)
            }
        }
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
            // WhatsApp, Telegram, redes sociales, llamadas y mensajes personales son descartados inmediatamente
            val isPersonalOrBlocked = packageName == applicationContext.packageName.lowercase(Locale.ROOT) ||
                    BLOCKED_PACKAGES.contains(packageName) ||
                    packageName.contains("whatsapp") ||
                    packageName.contains("telegram") ||
                    packageName.contains("messenger") ||
                    packageName.contains("instagram") ||
                    packageName.contains("facebook") ||
                    packageName.contains("tiktok") ||
                    packageName.contains("messaging") ||
                    packageName.contains("dialer") ||
                    packageName.contains("telecom")

            if (isPersonalOrBlocked) {
                return
            }

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val ticker = notification.tickerText?.toString() ?: ""

            val combinedContent = if (bigText.isNotEmpty()) bigText else text
            val lowerContent = combinedContent.lowercase(Locale.ROOT)

            // 2. Solo permitir si pertenece a bancos o contiene palabras financieras
            val isFinancialApp = ALLOWED_PACKAGES.contains(packageName) ||
                    packageName.contains("yape") ||
                    packageName.contains("plin") ||
                    packageName.contains("bcp") ||
                    packageName.contains("interbank") ||
                    packageName.contains("bbva") ||
                    packageName.contains("scotiabank") ||
                    packageName.contains("bancamovil") ||
                    packageName.contains("banco") ||
                    packageName.contains("caja") ||
                    lowerContent.contains("yape") ||
                    lowerContent.contains("plin")

            if (!isFinancialApp) {
                return
            }

            val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            val notificationKey = sbn.key ?: ""
            val postTime = sbn.postTime

            // 3. Procesar cobro con algoritmo bancario de concurrencia
            val payment = PaymentParser.parse(
                packageName = packageName,
                title = title,
                text = combinedContent,
                subText = subText,
                ticker = ticker,
                notificationKey = notificationKey,
                postTime = postTime
            )

            if (payment != null) {
                val settings = PaymentRepository.settings.value
                val isAppActive = settings.activeApps[payment.appSource] ?: true

                if (isAppActive) {
                    val isNew = PaymentRepository.addPayment(payment)
                    if (isNew) {
                        Log.d(TAG, "Cobro detectado y admitido: ${payment.appSource.displayName} S/ ${payment.amount} (key=$notificationKey, time=$postTime)")
                        PaymentRepository.recordRawNotification("[$time] COBRO ${payment.appSource.displayName}: S/ ${payment.amount} de ${payment.senderName}")
                        speechManager.speakPayment(payment, settings)
                    } else {
                        Log.d(TAG, "Notificacion duplicada del sistema descartada por idempotencia bancaria.")
                    }
                }
            } else {
                // Registrar solo avisos bancarios en el monitor
                PaymentRepository.recordRawNotification("[$time] [${packageName.takeLast(15)}] $title: $combinedContent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación", e)
        }
    }

    companion object {
        private const val TAG = "CobrosNotificationListener"
    }
}
