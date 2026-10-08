package com.parlantecobros.app.service

import android.app.Notification
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
        PaymentRepository.recordRawNotification("[$time] Conectado exitosamente al sistema de Android.")
        Log.d(TAG, "onListenerConnected: Android ha vinculado el servicio de notificaciones.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        PaymentRepository.recordRawNotification("[$time] Servicio desconectado temporalmente por Android.")
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
            val packageName = sbn.packageName ?: ""
            // Ignorar notificaciones de nuestra propia app
            if (packageName == applicationContext.packageName) return

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val ticker = notification.tickerText?.toString() ?: ""

            val combinedContent = if (bigText.isNotEmpty()) bigText else text

            val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            val rawPreview = "[$time] $packageName\n$title: $combinedContent"
            PaymentRepository.recordRawNotification(rawPreview)
            Log.d(TAG, "Notificación detectada: $rawPreview")

            // Intentar parsear el cobro (Yape, Plin, bancos)
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
                        PaymentRepository.recordRawNotification("[$time] ¡COBRO DETECTADO! ${payment.appSource.displayName} - S/ ${payment.amount}")
                        speechManager.speakPayment(payment, settings)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación", e)
        }
    }

    companion object {
        private const val TAG = "ParlanteCobrosListener"
    }
}
