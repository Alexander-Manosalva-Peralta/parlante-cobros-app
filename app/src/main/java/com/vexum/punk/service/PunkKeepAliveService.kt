package com.vexum.punk.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vexum.punk.MainActivity
import com.vexum.punk.R
import kotlinx.coroutines.*

class PunkKeepAliveService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var watchdogScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        startForegroundNotification()
        startWatchdog()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        acquireWakeLock()
        startForegroundNotification()
        startWatchdog()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startWatchdog() {
        if (watchdogScope != null) return
        watchdogScope = CoroutineScope(Dispatchers.Default + Job())
        watchdogScope?.launch {
            while (isActive) {
                delay(10 * 60 * 1000L) // Chequeo periódico cada 10 minutos
                try {
                    if (!CobrosNotificationListener.isConnected) {
                        Log.w(TAG, "Watchdog detectó listener desvinculado, forzando reconexión...")
                        CobrosNotificationListener.forceRebind(applicationContext)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en ciclo watchdog", e)
                }
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "punk:KeepAliveServiceWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire()
                Log.d(TAG, "WakeLock adquirido con éxito para servicio 24/7.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adquiriendo WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock liberado.")
            }
        } catch (_: Exception) {}
    }

    private fun startForegroundNotification() {
        val channelId = "punk_keep_alive_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Punk Altavoz Permanente",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene el altavoz activo con la pantalla apagada"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Punk activo 24/7")
            .setContentText("Escuchando cobros de Yape, Plin y bancos en tiempo real")
            .setSmallIcon(R.drawable.logo_p_solo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        watchdogScope?.cancel()
        watchdogScope = null
        releaseWakeLock()

        // Solicitar reinicio automático si el sistema intenta destruir el servicio
        try {
            val restartIntent = Intent(applicationContext, PunkWatchdogReceiver::class.java).apply {
                action = "com.vexum.punk.RESTART_KEEPALIVE"
            }
            sendBroadcast(restartIntent)
        } catch (_: Exception) {}

        super.onDestroy()
    }

    companion object {
        private const val TAG = "PunkKeepAliveService"
        private const val NOTIFICATION_ID = 9001

        fun start(context: Context) {
            try {
                val intent = Intent(context, PunkKeepAliveService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando servicio persistente", e)
            }
        }
    }
}
