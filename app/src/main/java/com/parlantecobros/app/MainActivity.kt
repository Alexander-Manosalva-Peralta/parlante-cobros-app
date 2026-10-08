package com.parlantecobros.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.NotificationManagerCompat
import com.parlantecobros.app.data.PaymentRepository
import com.parlantecobros.app.service.CobrosNotificationListener
import com.parlantecobros.app.service.PunkKeepAliveService
import com.parlantecobros.app.service.SpeechManager
import com.parlantecobros.app.ui.screens.HomeScreen
import com.parlantecobros.app.ui.theme.ParlanteCobrosTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var speechManager: SpeechManager
    private var isListenerEnabled by mutableStateOf(false)
    private var isBatteryOptIgnored by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechManager = SpeechManager(applicationContext)

        // Iniciar servicio en primer plano para mantener punk activo 24/7 con pantalla apagada
        PunkKeepAliveService.start(applicationContext)

        setContent {
            ParlanteCobrosTheme {
                HomeScreen(
                    isNotificationPermissionGranted = isListenerEnabled,
                    isBatteryOptimizationIgnored = isBatteryOptIgnored,
                    onRequestPermission = { openNotificationListenerSettings() },
                    onRequestIgnoreBatteryOptimization = { requestIgnoreBatteryOptimizations() },
                    onOpenAppSettings = { openAppSettings() },
                    onRebindService = { forceRebindAndTest() },
                    onTestVoice = { sampleText -> speechManager.testVoice(sampleText) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationListenerPermission()
        checkBatteryOptimization()
        PunkKeepAliveService.start(applicationContext)
        forceRebind()
    }

    override fun onDestroy() {
        speechManager.shutdown()
        super.onDestroy()
    }

    private fun checkNotificationListenerPermission() {
        try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
            isListenerEnabled = enabledPackages.contains(packageName)
        } catch (_: Exception) {
            val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            isListenerEnabled = flat != null && flat.contains(packageName)
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            isBatteryOptIgnored = pm.isIgnoringBatteryOptimizations(packageName)
        } else {
            isBatteryOptIgnored = true
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    private fun forceRebind() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isListenerEnabled) {
            try {
                val component = ComponentName(this, CobrosNotificationListener::class.java)
                NotificationListenerService.requestRebind(component)
            } catch (_: Exception) {}
        }
    }

    private fun forceRebindAndTest() {
        forceRebind()
        val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        PaymentRepository.recordRawNotification("[$time] Servicio reconectado manualmente por el usuario.")
        Toast.makeText(this, "Servicio reconectado. Probando altavoz...", Toast.LENGTH_SHORT).show()
        speechManager.testVoice("¡punk reconectado y funcionando correctamente!")
    }

    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }
}
