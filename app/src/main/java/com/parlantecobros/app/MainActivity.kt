package com.parlantecobros.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.NotificationManagerCompat
import com.parlantecobros.app.service.CobrosNotificationListener
import com.parlantecobros.app.service.SpeechManager
import com.parlantecobros.app.ui.screens.HomeScreen
import com.parlantecobros.app.ui.theme.ParlanteCobrosTheme

class MainActivity : ComponentActivity() {

    private lateinit var speechManager: SpeechManager
    private var isListenerEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechManager = SpeechManager(applicationContext)

        setContent {
            ParlanteCobrosTheme {
                HomeScreen(
                    isNotificationPermissionGranted = isListenerEnabled,
                    onRequestPermission = { openNotificationListenerSettings() },
                    onOpenAppSettings = { openAppSettings() },
                    onRebindService = { rebindNotificationListener() },
                    onTestVoice = { sampleText -> speechManager.testVoice(sampleText) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationListenerPermission()
        rebindNotificationListener()
    }

    override fun onDestroy() {
        speechManager.shutdown()
        super.onDestroy()
    }

    private fun checkNotificationListenerPermission() {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        isListenerEnabled = flat != null && flat.contains(packageName)
    }

    private fun rebindNotificationListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isListenerEnabled) {
            try {
                val component = ComponentName(this, CobrosNotificationListener::class.java)
                NotificationListenerService.requestRebind(component)
            } catch (_: Exception) {}
        }
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
