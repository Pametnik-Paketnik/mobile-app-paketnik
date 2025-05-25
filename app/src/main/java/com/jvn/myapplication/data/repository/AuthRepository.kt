// File: data/repository/AuthRepository.kt
package com.jvn.myapplication.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.model.ErrorResponse
import com.jvn.myapplication.data.model.LoginRequest
import com.jvn.myapplication.data.model.RegisterRequest
import com.jvn.myapplication.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AuthRepository(private val context: Context) {
    private val authApi = NetworkModule.authApi

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = authApi.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                if (loginResponse.success && loginResponse.access_token != null) {
                    saveAuthData(loginResponse.access_token, username)
                    Result.success("Login successful")
                } else {
                    Result.failure(Exception(loginResponse.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gson = Gson()
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Login failed: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, password: String, confirmPassword: String): Result<String> {
        return try {
            val response = authApi.register(RegisterRequest(username, password, confirmPassword))
            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!
                Result.success(registerResponse.message)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gson = Gson()
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Registration failed: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        val token = getAuthToken().first()
        if (!token.isNullOrEmpty()) {
            try {
                // Call backend logout API
                val response = authApi.logout("Bearer $token")
                if (response.isSuccessful) {
                    // Backend logout successful, clear local storage
                    context.dataStore.edit { preferences ->
                        preferences.clear()
                    }
                } else {
                    // Backend logout failed, but still clear local storage
                    context.dataStore.edit { preferences ->
                        preferences.clear()
                    }
                }
            } catch (e: Exception) {
                // Network error, but still clear local storage
                context.dataStore.edit { preferences ->
                    preferences.clear()
                }
            }
        } else {
            // No token to logout, just clear local storage
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    private suspend fun saveAuthData(token: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USERNAME_KEY] = username
        }
    }

    fun getAuthToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }
}