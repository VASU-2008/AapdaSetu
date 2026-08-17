package com.example.aapdasetu.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.aapdasetu.data.api.RetrofitClient
import com.example.aapdasetu.data.model.AuthResponse
import com.example.aapdasetu.data.model.ErrorResponse
import com.example.aapdasetu.data.model.LoginRequest
import com.example.aapdasetu.data.model.RegisterRequest
import com.example.aapdasetu.data.model.UserModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

class AuthRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aapdasetu_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "user_username"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SERVER_IP = "server_ip"
    }

    init {
        val savedToken = prefs.getString(KEY_TOKEN, null)
        RetrofitClient.setToken(savedToken)

        val savedIp = prefs.getString(KEY_SERVER_IP, null)
        if (!savedIp.isNullOrEmpty()) {
            RetrofitClient.baseUrl = savedIp
        }
    }

    fun setServerIp(ip: String) {
        val formatted = if (ip.endsWith("/")) ip else "$ip/"
        RetrofitClient.baseUrl = formatted
        prefs.edit().putString(KEY_SERVER_IP, formatted).apply()
    }

    fun getServerIp(): String {
        return RetrofitClient.baseUrl
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getSavedUser(): UserModel? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, "")
        val email = prefs.getString(KEY_EMAIL, "")
        return UserModel(id = id, username = username, email = email)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    suspend fun login(email: String, pass: String, rememberMe: Boolean = true): AuthResult<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService()
                val response = api.login(LoginRequest(email = email.trim(), password = pass))

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val token = body.token
                    val user = body.user

                    if (!token.isNullOrEmpty()) {
                        RetrofitClient.setToken(token)
                        if (rememberMe) {
                            prefs.edit()
                                .putString(KEY_TOKEN, token)
                                .putString(KEY_USER_ID, user?.id)
                                .putString(KEY_USERNAME, user?.username)
                                .putString(KEY_EMAIL, user?.email)
                                .putBoolean(KEY_REMEMBER_ME, true)
                                .apply()
                        }
                    }
                    AuthResult.Success(body)
                } else {
                    val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    AuthResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                AuthResult.Error(e.localizedMessage ?: "Network error connecting to backend")
            }
        }
    }

    suspend fun register(fullName: String, email: String, pass: String): AuthResult<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService()
                val response = api.register(
                    RegisterRequest(
                        fullName = fullName.trim(),
                        email = email.trim(),
                        password = pass
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val token = body.token
                    val user = body.user

                    if (!token.isNullOrEmpty()) {
                        RetrofitClient.setToken(token)
                        prefs.edit()
                            .putString(KEY_TOKEN, token)
                            .putString(KEY_USER_ID, user?.id)
                            .putString(KEY_USERNAME, user?.username)
                            .putString(KEY_EMAIL, user?.email)
                            .putBoolean(KEY_REMEMBER_ME, true)
                            .apply()
                    }
                    AuthResult.Success(body)
                } else {
                    val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    AuthResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                AuthResult.Error(e.localizedMessage ?: "Network error connecting to backend")
            }
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            try {
                RetrofitClient.getApiService().logout()
            } catch (_: Exception) { }
            RetrofitClient.setToken(null)
            prefs.edit().clear().apply()
        }
    }

    private fun parseErrorMessage(errorJson: String?): String {
        if (errorJson.isNullOrEmpty()) return "Unknown error occurred"
        return try {
            val errorObj = Gson().fromJson(errorJson, ErrorResponse::class.java)
            errorObj.message ?: "Authentication failed"
        } catch (_: Exception) {
            "Authentication failed. Please check your details."
        }
    }
}
