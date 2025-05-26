// File: data/repository/AuthRepository.kt (Simplified)
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
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = authApi.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                if (loginResponse.success && loginResponse.access_token != null) {
                    // Generate a user ID (in real app, this would come from backend)
                    val userId = generateUserId(username)
                    saveAuthData(loginResponse.access_token, username, userId)
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

    private suspend fun saveAuthData(token: String, username: String, userId: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USERNAME_KEY] = username
            preferences[USER_ID_KEY] = userId
        }
    }

    private fun generateUserId(username: String): String {
        // In a real app, this would come from the backend response
        return "user_${username}_${System.currentTimeMillis()}"
    }

    fun getAuthToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

    fun getUserId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }
    }

    fun getUsername(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USERNAME_KEY]
        }
    }
}