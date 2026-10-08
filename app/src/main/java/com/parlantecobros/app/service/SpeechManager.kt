package com.parlantecobros.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.parlantecobros.app.model.AppSettings
import com.parlantecobros.app.model.PaymentItem
import com.parlantecobros.app.model.SpeechTemplate
import java.util.Locale

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingMessages = mutableListOf<String>()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error instanciando TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "PE"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("es", "ES"))
            }
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            isInitialized = true

            synchronized(pendingMessages) {
                pendingMessages.forEach { speakRaw(it) }
                pendingMessages.clear()
            }
        } else {
            Log.e("SpeechManager", "Error inicializando TextToSpeech: $status")
        }
    }

    fun speakPayment(payment: PaymentItem, settings: AppSettings) {
        if (!settings.speakerEnabled) return

        ensureAudibleVolume()

        if (settings.chimeBeforeSpeaking) {
            playChime()
        }

        val message = buildSpeechText(payment, settings)
        acquireWakeLock()

        tts?.setPitch(settings.speechPitch)
        tts?.setSpeechRate(settings.speechRate)

        if (isInitialized) {
            speakRaw(message)
        } else {
            synchronized(pendingMessages) {
                pendingMessages.add(message)
            }
        }
    }

    private fun ensureAudibleVolume() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            // Si el volumen está silenciado o muy bajo, asegurar al menos 75%
            if (currentVol < (maxVol * 0.35)) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.75).toInt(), 0)
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error ajustando volumen", e)
        }
    }

    private fun buildSpeechText(payment: PaymentItem, settings: AppSettings): String {
        val appName = payment.appSource.displayName
        val amountSpoken = payment.spokenAmountDescription
        val client = payment.senderName

        return when (settings.speechTemplate) {
            SpeechTemplate.COMPLETO -> {
                if (settings.mentionCustomerName && client.isNotEmpty() && client != "Cliente") {
                    "¡$appName recibido! $amountSpoken de $client"
                } else {
                    "¡$appName recibido! $amountSpoken"
                }
            }
            SpeechTemplate.CORTO -> "¡$appName! $amountSpoken"
            SpeechTemplate.SOLO_MONTO -> "¡Pago recibido de $amountSpoken!"
        }
    }

    fun testVoice(sampleText: String = "¡Yape recibido! Quince soles con cincuenta de Juan Pérez") {
        ensureAudibleVolume()
        playChime()
        speakRaw(sampleText)
    }

    private fun speakRaw(text: String) {
        try {
            tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "PAYMENT_VOICE_${System.currentTimeMillis()}"
            )
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error en speakRaw", e)
        }
    }

    private fun playChime() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error reproduciendo tono", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ParlanteCobros:SpeechWakeLock"
            )
            wakeLock.acquire(4000)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error con WakeLock", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
