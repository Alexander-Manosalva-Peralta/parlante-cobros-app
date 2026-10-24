package com.vexum.punk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PunkWatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("PunkWatchdogReceiver", "Señal del sistema recibida: $action")

        // 1. Iniciar/mantener activo el servicio de primer plano
        PunkKeepAliveService.start(context)

        // 2. Si el teléfono se reinició, se actualizó la app o se encendió la pantalla,
        // forzar reconexión del NotificationListenerService para evitar desconexiones de Android
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == Intent.ACTION_SCREEN_ON ||
            action == "com.vexum.punk.RESTART_KEEPALIVE"
        ) {
            CobrosNotificationListener.forceRebind(context)
        }
    }
}
