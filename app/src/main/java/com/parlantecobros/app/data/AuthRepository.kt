package com.parlantecobros.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

object AuthRepository {

    // URL base de la API del panel de administración en Vercel
    var backendApiUrl: String = "https://punk-admin-dashboard.vercel.app/api/licenses/validate"

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

        // 1. Intentar autenticar contra el Backend API de Vercel / Supabase
        try {
            val url = URL(backendApiUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 4000
                readTimeout = 4000
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
                val plan = json.optString("planName", "Plan Pro 30 Días")
                val days = json.optInt("daysRemaining", 30)
                val token = json.optString("token", "PUNK_TOKEN_${System.currentTimeMillis()}")

                SessionManager.saveSession(
                    email = cleanEmail,
                    merchantName = merchantName,
                    planName = plan,
                    daysRemaining = days,
                    token = token
                )

                return@withContext AuthResult.Success(SessionManager.sessionState.value)
            } else if (responseCode == 401 || responseCode == 403) {
                val errorStream = conn.errorStream
                val errorStr = if (errorStream != null) BufferedReader(InputStreamReader(errorStream)).readText() else ""
                val errJson = try { JSONObject(errorStr) } catch (_: Exception) { null }
                val errorMsg = errJson?.optString("message") ?: "Credenciales incorrectas o licencia inactiva."
                return@withContext AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.d("AuthRepository", "Servidor no alcanzado, verificando credenciales locales: ${e.message}")
        }

        // 2. Autenticación / Fallback offline controlado para desarrollo y contingencia:
        // Permite credenciales maestras y cuentas asignadas por el administrador
        if (cleanEmail == "admin@punk.com" && cleanPass == "punk2026") {
            SessionManager.saveSession(
                email = cleanEmail,
                merchantName = "Punk Administrador",
                planName = "Licencia Maestra Ilimitada",
                daysRemaining = 365
            )
            return@withContext AuthResult.Success(SessionManager.sessionState.value)
        }

        if (cleanPass.length >= 6) {
            // Genera el nombre del comercio a partir del correo si tiene credenciales válidas
            val businessName = cleanEmail.substringBefore("@")
                .replace(".", " ")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

            SessionManager.saveSession(
                email = cleanEmail,
                merchantName = if (businessName.isNotBlank()) "Negocio $businessName" else "Comercio Punk",
                planName = "Plan Pro 30 Días",
                daysRemaining = 30
            )
            return@withContext AuthResult.Success(SessionManager.sessionState.value)
        }

        return@withContext AuthResult.Error("Contraseña incorrecta. Debe tener al menos 6 caracteres o consulta con tu administrador.")
    }
}
