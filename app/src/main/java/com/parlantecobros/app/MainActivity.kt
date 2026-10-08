package com.parlantecobros.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
                    onTestVoice = { sampleText -> speechManager.testVoice(sampleText) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationListenerPermission()
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

    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }
}
