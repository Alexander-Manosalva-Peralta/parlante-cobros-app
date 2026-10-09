package com.parlantecobros.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

object AuthRepository {

    // 1. Endpoint principal en Vercel
    var backendApiUrl: String = "https://punk-admin-dashboard-qgmq.vercel.app/api/licenses/validate"

    // 2. Conexión directa de respaldo a Supabase
    private const val SUPABASE_URL = "https://uavilbyvctfydqvkfzjg.supabase.co/rest/v1/merchants"
    private const val SUPABASE_KEY = "sb_publishable_JH4aF-NZ23Do7n9EswWmDw_iIJNVbbp"

    suspend fun login(email: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()

        if (cleanEmail.isEmpty()) {
            return@withContext AuthResult.Error("Por favor ingresa tu correo electrónico.")
        }
        if (cleanPass.isEmpty()) {
            return@withContext AuthResult.Error("Por favor ingresa tu contraseña.")
        }

        val deviceId = SessionManager.getDeviceId()

        // 1. Validar contra el Backend en Vercel (que consulta Supabase)
        try {
            val url = URL(backendApiUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", cleanPass)
                put("deviceId", deviceId)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseStr = reader.readText()
                val json = JSONObject(responseStr)

                val merchantName = json.optString("merchantName", "Comercio Punk")
                val plan = json.optString("planName", "Pago Único Vitalicio (De por vida)")
                val days = json.optInt("daysRemaining", 9999)
                val token = json.optString("token", "PUNK_TOKEN_${System.currentTimeMillis()}")

                SessionManager.saveSession(
                    email = cleanEmail,
                    merchantName = merchantName,
                    planName = plan,
                    daysRemaining = days,
                    token = token
                )

                return@withContext AuthResult.Success(SessionManager.sessionState.value)
            } else if (responseCode == 400 || responseCode == 401 || responseCode == 403) {
                val errorStream = conn.errorStream
                val errorStr = if (errorStream != null) BufferedReader(InputStreamReader(errorStream)).readText() else ""
                val errJson = try { JSONObject(errorStr) } catch (_: Exception) { null }
                val errorMsg = errJson?.optString("message") ?: "Credenciales no autorizadas o cuenta inexistente en Punk."
                return@withContext AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Vercel no respondió, consultando directamente a Supabase: ${e.message}")
        }

        // 2. Consulta Directa a Supabase como respaldo (por si Vercel tiene demora de red)
        try {
            val encodedEmail = URLEncoder.encode(cleanEmail, "UTF-8")
            val directUrl = URL("$SUPABASE_URL?email=eq.$encodedEmail&select=*")
            val conn = (directUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 6000
                readTimeout = 6000
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val jsonArray = JSONArray(reader.readText())

                if (jsonArray.length() == 0) {
                    return@withContext AuthResult.Error("Comercio no encontrado en el sistema Punk. Verifica tu correo con el administrador.")
                }

                val merchant = jsonArray.getJSONObject(0)
                val expectedPass = merchant.optString("password_hash", "")
                val status = merchant.optString("status", "ACTIVE")
                val boundDevice = merchant.optString("bound_device_id", "")
                val businessName = merchant.optString("business_name", "Comercio Punk")
                val planName = merchant.optString("plan_name", "Pago Único Vitalicio (De por vida)")

                if (expectedPass != cleanPass) {
                    return@withContext AuthResult.Error("Contraseña incorrecta. Contacta a tu administrador Punk.")
                }

                if (status == "SUSPENDED") {
                    return@withContext AuthResult.Error("Tu cuenta ha sido suspendida por el administrador.")
                }

                // Verificación de Bloqueo Anti-Piratería por Dispositivo
                if (boundDevice.isNotEmpty() && boundDevice != "null" && boundDevice != deviceId) {
                    return@withContext AuthResult.Error("Esta cuenta ya está activa en otro teléfono celular. Para cambiar de equipo, solicita a tu administrador desvincularlo.")
                }

                SessionManager.saveSession(
                    email = cleanEmail,
                    merchantName = businessName,
                    planName = planName,
                    daysRemaining = 9999,
                    token = "PUNK_SUPABASE_${merchant.optString("id", "")}"
                )

                return@withContext AuthResult.Success(SessionManager.sessionState.value)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al consultar Supabase directamente: ${e.message}")
        }

        // Si fallaron tanto Vercel como Supabase o las credenciales no existen:
        return@withContext AuthResult.Error("No se pudo validar la licencia. Verifica que tu correo y contraseña estén registrados en el panel de Punk y que tengas conexión a internet.")
    }
}
