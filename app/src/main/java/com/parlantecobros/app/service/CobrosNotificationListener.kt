package com.parlantecobros.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.parlantecobros.app.data.PaymentRepository

class CobrosNotificationListener : NotificationListenerService() {

    private lateinit var speechManager: SpeechManager

    override fun onCreate() {
        super.onCreate()
        speechManager = SpeechManager(applicationContext)
        startForegroundNotification()
        Log.d(TAG, "CobrosNotificationListener iniciado y listo.")
    }

    override fun onDestroy() {
        speechManager.shutdown()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        val extras = sbn.notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val combinedContent = if (bigText.isNotEmpty()) bigText else text

        // Intentar parsear el cobro
        val payment = PaymentParser.parse(
            packageName = packageName,
            title = title,
            text = combinedContent,
            subText = subText
        ) ?: return

        val settings = PaymentRepository.settings.value

        // Verificar si la app específica está activada en ajustes
        val isAppActive = settings.activeApps[payment.appSource] ?: true
        if (!isAppActive) {
            Log.d(TAG, "Ignorando pago de ${payment.appSource.displayName} por estar desactivada.")
            return
        }

        // Registrar pago y anunciar
        val isNew = PaymentRepository.addPayment(payment)
        if (isNew) {
            Log.d(TAG, "¡Cobro detectado con éxito! ${payment.appSource.displayName} - S/ ${payment.amount}")
            speechManager.speakPayment(payment, settings)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    private fun startForegroundNotification() {
        val channelId = "parlante_cobros_listener_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servicio de Parlante Cobros",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activo el escuchador de cobros en segundo plano"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Parlante Cobros Activo")
            .setContentText("Escuchando notificaciones de Yape, Plin y bancos...")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // ID persistente
        startForeground(1001, notification)
    }

    companion object {
        private const val TAG = "ParlanteCobrosListener"
    }
}
