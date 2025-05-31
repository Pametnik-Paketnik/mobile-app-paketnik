package com.jvn.myapplication.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.model.ErrorResponse
import com.jvn.myapplication.data.model.LoginRequest
import com.jvn.myapplication.data.model.RegisterRequest
import com.jvn.myapplication.data.model.RegisterResponse
import com.jvn.myapplication.data.model.User
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
        private val USER_TYPE_KEY = stringPreferencesKey("user_type")
    }

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = authApi.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                if (loginResponse.success && loginResponse.access_token != null) {
                    // CRITICAL FIX: Save the REAL user ID from API response
                    saveAuthData(
                        token = loginResponse.access_token,
                        username = loginResponse.user.username,
                        userId = loginResponse.user.id.toString(), // Use real ID: "3" not "user_john_doe_123"
                        userType = loginResponse.user.userType
                    )

                    // Debug: Print what we're saving
                    println("🔍 DEBUG - AuthRepository: Saving real user ID: ${loginResponse.user.id}")
                    println("🔍 DEBUG - AuthRepository: Username: ${loginResponse.user.username}")
                    println("🔍 DEBUG - AuthRepository: UserType: ${loginResponse.user.userType}")

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

    suspend fun register(username: String, password: String, userType: String): RegisterResponse {
        return try {
            val request = RegisterRequest(
                username = username,
                password = password,
                userType = userType
            )
            val response = authApi.register(request)
            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!
                if (registerResponse.success) {
                    // Save authentication data
                    saveAuthData(
                        token = registerResponse.access_token,
                        username = registerResponse.user.username,
                        userId = registerResponse.user.id.toString(),
                        userType = registerResponse.user.userType
                    )
                }
                registerResponse
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gson = Gson()
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Registration failed: ${response.message()}"
                }
                RegisterResponse(
                    success = false,
                    message = errorMessage,
                    access_token = "",
                    user = User(id = 0, username = "", userType = "")
                )
            }
        } catch (e: Exception) {
            RegisterResponse(
                success = false,
                message = e.message ?: "Registration failed",
                access_token = "",
                user = User(id = 0, username = "", userType = "")
            )
        }
    }

    suspend fun logout() {
        println("🔍 DEBUG - AuthRepository: Starting logout process")

        val token = getAuthToken().first()
        if (!token.isNullOrEmpty()) {
            try {
                println("🔍 DEBUG - AuthRepository: Calling backend logout API")
                val response = authApi.logout("Bearer $token")
                if (response.isSuccessful) {
                    println("🔍 DEBUG - AuthRepository: Backend logout successful")
                } else {
                    println("🔍 DEBUG - AuthRepository: Backend logout failed: ${response.code()}")
                }
            } catch (e: Exception) {
                println("🔍 DEBUG - AuthRepository: Backend logout exception: ${e.message}")
            }
        }

        // CRITICAL: Always clear local storage regardless of backend response
        println("🔍 DEBUG - AuthRepository: Clearing local DataStore")
        try {
            context.dataStore.edit { preferences ->
                val sizeBefore = preferences.asMap().size
                println("🔍 DEBUG - AuthRepository: DataStore contained $sizeBefore entries before clear")

                preferences.clear()

                val sizeAfter = preferences.asMap().size
                println("🔍 DEBUG - AuthRepository: DataStore contains $sizeAfter entries after clear")
            }

            // Verify clearing worked
            val userIdAfterClear = getUserId().first()
            val usernameAfterClear = getUsername().first()
            val userTypeAfterClear = getUserType().first()
            val tokenAfterClear = getAuthToken().first()

            println("🔍 DEBUG - AuthRepository: After clear verification:")
            println("🔍 DEBUG - UserId: '$userIdAfterClear'")
            println("🔍 DEBUG - Username: '$usernameAfterClear'")
            println("🔍 DEBUG - UserType: '$userTypeAfterClear'")
            println("🔍 DEBUG - Token: '${tokenAfterClear?.take(20)}...'")

        } catch (e: Exception) {
            println("🔍 DEBUG - AuthRepository: DataStore clear failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun saveAuthData(
        token: String,
        username: String,
        userId: String,
        userType: String? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USERNAME_KEY] = username
            preferences[USER_ID_KEY] = userId
            userType?.let { preferences[USER_TYPE_KEY] = it }
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

    fun getUserType(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_TYPE_KEY]
        }
    }
}