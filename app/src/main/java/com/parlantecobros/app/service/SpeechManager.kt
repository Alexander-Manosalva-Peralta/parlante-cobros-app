package com.parlantecobros.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.parlantecobros.app.model.AppSettings
import com.parlantecobros.app.model.PaymentItem
import com.parlantecobros.app.model.SpeechTemplate
import java.util.Locale

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingMessages = mutableListOf<String>()
    private var wakeLock: PowerManager.WakeLock? = null
    private var pendingUtteranceCount = 0

    init {
        try {
            // Priorizar motor oficial de Google Speech Services si está instalado para síntesis neuronal humana
            tts = TextToSpeech(context.applicationContext, this, "com.google.android.tts")
        } catch (e: Exception) {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (ex: Exception) {
                Log.e("SpeechManager", "Error instanciando TextToSpeech", ex)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setupTtsEngine()
        } else {
            Log.e("SpeechManager", "Error inicializando TextToSpeech con motor Google, reintentando con motor por defecto: $status")
            try {
                tts = TextToSpeech(context.applicationContext) { fallbackStatus ->
                    if (fallbackStatus == TextToSpeech.SUCCESS) {
                        setupTtsEngine()
                    }
                }
            } catch (e: Exception) {
                Log.e("SpeechManager", "Error en fallback de TextToSpeech", e)
            }
        }
    }

    private fun setupTtsEngine() {
        configureBestNaturalVoice()

        // USAGE_ALARM garantiza que la voz se escuche AUNQUE la pantalla esté apagada,
        // el teléfono esté bloqueado o el dispositivo esté en modo silencioso/vibración.
        tts?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                acquireWakeLock()
            }

            override fun onDone(utteranceId: String?) {
                synchronized(this@SpeechManager) {
                    pendingUtteranceCount = (pendingUtteranceCount - 1).coerceAtLeast(0)
                    if (pendingUtteranceCount == 0) {
                        releaseWakeLock()
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                synchronized(this@SpeechManager) {
                    pendingUtteranceCount = (pendingUtteranceCount - 1).coerceAtLeast(0)
                    if (pendingUtteranceCount == 0) {
                        releaseWakeLock()
                    }
                }
            }
        })

        isInitialized = true

        synchronized(pendingMessages) {
            pendingMessages.forEach { speakRaw(it) }
            pendingMessages.clear()
        }
    }

    private fun configureBestNaturalVoice() {
        try {
            val peLocale = Locale("es", "PE")
            val langResult = tts?.setLanguage(peLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("es", "ES"))
            }

            val availableVoices = tts?.voices?.filter { voice ->
                voice.locale.language.equals("es", ignoreCase = true)
            } ?: emptyList()

            // Algoritmo de selección de voz más humana y natural:
            // Prioriza redes neuronales (WaveNet / Neural / Google Network), calidad alta y acento fluido
            val bestVoice = availableVoices.maxByOrNull { voice ->
                var score = 0
                val name = voice.name.lowercase(Locale.ROOT)

                // Calidad de síntesis
                if (voice.quality >= Voice.QUALITY_VERY_HIGH) score += 60
                if (voice.quality >= Voice.QUALITY_HIGH) score += 40

                // Modelos de red neuronal de Google (suenan idénticos a una persona real)
                if (name.contains("neural") || name.contains("wavenet") || name.contains("natural")) score += 100
                if (name.contains("network")) score += 75

                // Preferencia por español de Perú o Latinoamérica
                if (voice.locale.country.equals("PE", ignoreCase = true)) score += 45
                if (voice.locale.country.equals("419", ignoreCase = true) ||
                    voice.locale.country.equals("US", ignoreCase = true) ||
                    voice.locale.country.equals("MX", ignoreCase = true)
                ) score += 30

                // Voces de tesitura femenina / cálida (ana, eed, sfb, female)
                if (name.contains("female") || name.contains("eed") || name.contains("sfb") || name.contains("ana")) score += 20

                score
            }

            if (bestVoice != null) {
                tts?.voice = bestVoice
                Log.d("SpeechManager", "Voz natural seleccionada: ${bestVoice.name} (calidad: ${bestVoice.quality})")
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error configurando voz natural", e)
        }
    }

    fun speakPayment(payment: PaymentItem, settings: AppSettings) {
        if (!settings.speakerEnabled) return

        // 1. Activar WakeLock inmediatamente para que la CPU no se duerma con la pantalla apagada
        acquireWakeLock()

        // 2. Garantizar volumen audible en altavoz
        ensureAudibleVolume()

        // 3. Tono previo
        if (settings.chimeBeforeSpeaking) {
            playChime()
            try { Thread.sleep(220) } catch (_: Exception) {}
        }

        val message = buildSpeechText(payment, settings)

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
            // Asegurar volumen de Alarma y Música para que nunca esté silenciado
            val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val currentAlarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (currentAlarm < (maxAlarm * 0.4)) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (maxAlarm * 0.85).toInt(), 0)
            }

            val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentMusic < (maxMusic * 0.4)) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxMusic * 0.85).toInt(), 0)
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error ajustando volumen", e)
        }
    }

    private fun buildSpeechText(payment: PaymentItem, settings: AppSettings): String {
        val appName = payment.appSource.displayName
        val amountSpoken = payment.spokenAmountDescription
        val client = payment.senderName.trim()

        return when (settings.speechTemplate) {
            SpeechTemplate.COMPLETO -> {
                if (settings.mentionCustomerName && client.isNotEmpty() && !client.equals("Cliente", ignoreCase = true)) {
                    "¡$appName recibido! $amountSpoken, de $client."
                } else {
                    "¡$appName recibido! $amountSpoken."
                }
            }
            SpeechTemplate.CORTO -> "¡$appName! $amountSpoken."
            SpeechTemplate.SOLO_MONTO -> "¡Pago recibido de $amountSpoken!"
        }
    }

    fun testVoice(sampleText: String = "¡Yape recibido! Quince soles con cincuenta, de Juan Pérez.") {
        acquireWakeLock()
        ensureAudibleVolume()
        playChime()
        try { Thread.sleep(220) } catch (_: Exception) {}
        speakRaw(sampleText)
    }

    private fun speakRaw(text: String) {
        try {
            synchronized(this) {
                pendingUtteranceCount++
            }
            // Enrutamiento forzado por STREAM_ALARM para máxima potencia y pantalla apagada
            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            // QUEUE_ADD: encola pagos sucesivos (como depósitos ráfaga de bancos) sin truncar el anterior
            tts?.speak(
                text,
                TextToSpeech.QUEUE_ADD,
                params,
                "PUNK_VOICE_${System.currentTimeMillis()}_${(100..999).random()}"
            )
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error en speakRaw", e)
        }
    }

    private fun playChime() {
        try {
            // STREAM_ALARM se reproduce incluso en modo silencioso y pantalla bloqueada
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error reproduciendo tono", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "punk:SpeechWakeLock"
                )
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(30_000) // 30 segundos de vigencia máxima
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error adquiriendo WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            releaseWakeLock()
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
