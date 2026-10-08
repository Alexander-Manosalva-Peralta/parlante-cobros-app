package com.parlantecobros.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "PE")) // Español de Perú
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback a español general
                tts?.setLanguage(Locale("es", "ES"))
            }
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM) // Se escucha fuerte como alarma/notificación comercial
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            isInitialized = true

            // Hablar mensajes pendientes si había alguno
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

        // 1. Play Chime / Campanilla de aviso
        if (settings.chimeBeforeSpeaking) {
            playChime()
        }

        // 2. Construir frase
        val message = buildSpeechText(payment, settings)

        // 3. Adquirir WakeLock temporal de 5 segundos para que la CPU no se duerma
        acquireWakeLock()

        // 4. Configurar pitch y velocidad
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

    fun testVoice(sampleText: String = "¡Yape recibido! Quince soles de Juan Pérez") {
        playChime()
        speakRaw(sampleText)
    }

    private fun speakRaw(text: String) {
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "PAYMENT_VOICE_${System.currentTimeMillis()}"
        )
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
            wakeLock.acquire(4000) // 4 segundos
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error con WakeLock", e)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
