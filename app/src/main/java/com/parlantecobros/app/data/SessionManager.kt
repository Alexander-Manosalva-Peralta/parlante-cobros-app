package com.parlantecobros.app.data

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class UserSession(
    val isLoggedIn: Boolean = false,
    val userEmail: String = "",
    val merchantName: String = "",
    val planName: String = "Plan Pro 30 Días",
    val daysRemaining: Int = 30,
    val authToken: String = "",
    val deviceId: String = ""
)

object SessionManager {

    private const val PREFS_NAME = "punk_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_USER_EMAIL = "key_user_email"
    private const val KEY_MERCHANT_NAME = "key_merchant_name"
    private const val KEY_PLAN_NAME = "key_plan_name"
    private const val KEY_DAYS_REMAINING = "key_days_remaining"
    private const val KEY_AUTH_TOKEN = "key_auth_token"
    private const val KEY_DEVICE_ID = "key_device_id"

    private lateinit var prefs: SharedPreferences
    private val _sessionState = MutableStateFlow(UserSession())
    val sessionState: StateFlow<UserSession> = _sessionState.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            deviceId = if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                UUID.randomUUID().toString()
            }
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }

        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val merchant = prefs.getString(KEY_MERCHANT_NAME, "") ?: ""
        val plan = prefs.getString(KEY_PLAN_NAME, "Plan Pro 30 Días") ?: "Plan Pro 30 Días"
        val days = prefs.getInt(KEY_DAYS_REMAINING, 30)
        val token = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""

        _sessionState.value = UserSession(
            isLoggedIn = isLoggedIn,
            userEmail = email,
            merchantName = merchant,
            planName = plan,
            daysRemaining = days,
            authToken = token,
            deviceId = deviceId
        )
    }

    fun saveSession(
        email: String,
        merchantName: String,
        planName: String = "Plan Pro 30 Días",
        daysRemaining: Int = 30,
        token: String = UUID.randomUUID().toString()
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_MERCHANT_NAME, merchantName)
            putString(KEY_PLAN_NAME, planName)
            putInt(KEY_DAYS_REMAINING, daysRemaining)
            putString(KEY_AUTH_TOKEN, token)
            apply()
        }

        _sessionState.value = _sessionState.value.copy(
            isLoggedIn = true,
            userEmail = email,
            merchantName = merchantName,
            planName = planName,
            daysRemaining = daysRemaining,
            authToken = token
        )
    }

    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putString(KEY_AUTH_TOKEN, "")
            apply()
        }

        _sessionState.value = _sessionState.value.copy(
            isLoggedIn = false,
            authToken = ""
        )
    }

    fun getDeviceId(): String {
        return _sessionState.value.deviceId
    }
}
