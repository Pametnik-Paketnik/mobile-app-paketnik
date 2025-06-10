package com.jvn.myapplication.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.model.ErrorResponse
import com.jvn.myapplication.data.model.LoginRequest
import com.jvn.myapplication.data.model.LoginResponse
import com.jvn.myapplication.data.model.RegisterRequest
import com.jvn.myapplication.data.model.RegisterResponse
import com.jvn.myapplication.data.model.User
import com.jvn.myapplication.data.model.UserUpdateRequest
import com.jvn.myapplication.data.model.TotpLoginRequest
import com.jvn.myapplication.data.model.TwoFactorMethod
import com.jvn.myapplication.data.model.DeviceRegistrationRequest
import com.jvn.myapplication.data.model.DeviceRegistrationResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

import com.jvn.myapplication.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import android.util.Log

class AuthRepository(private val context: Context) {
    private val authApi = NetworkModule.authApi

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val NAME_KEY = stringPreferencesKey("name")
        private val SURNAME_KEY = stringPreferencesKey("surname")
        private val EMAIL_KEY = stringPreferencesKey("email")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_TYPE_KEY = stringPreferencesKey("user_type")
        private val FACE_2FA_ENABLED_KEY = booleanPreferencesKey("face_2fa_enabled")
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                if (loginResponse.success) {
                    // Check if 2FA is required
                    if (loginResponse.twoFactorRequired == true) {
                        // Return the response with 2FA info (tempToken and available methods)
                        println("🔍 DEBUG - AuthRepository: 2FA required, returning temp token")
                        Result.success(loginResponse)
                    } else if (loginResponse.access_token != null && loginResponse.user != null) {
                        // Standard login - save auth data
                        saveAuthData(
                            token = loginResponse.access_token,
                            name = loginResponse.user.name,
                            surname = loginResponse.user.surname,
                            email = loginResponse.user.email,
                            userId = loginResponse.user.id.toString(),
                            userType = loginResponse.user.userType
                        )

                        println("🔍 DEBUG - AuthRepository: Standard login successful")
                        Result.success(loginResponse)
                    } else {
                        Result.failure(Exception("Invalid login response"))
                    }
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

    suspend fun register(name: String, surname: String, email: String, password: String, userType: String): RegisterResponse {
        return try {
            val request = RegisterRequest(
                name = name,
                surname = surname,
                email = email,
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
                        name = registerResponse.user.name,
                        surname = registerResponse.user.surname,
                        email = registerResponse.user.email,
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
                    user = User(id = 0, name = "", surname = "", email = "", userType = "")
                )
            }
        } catch (e: Exception) {
            RegisterResponse(
                success = false,
                message = e.message ?: "Registration failed",
                access_token = "",
                user = User(id = 0, name = "", surname = "", email = "", userType = "")
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
        name: String,
        surname: String,
        email: String,
        userId: String,
        userType: String? = null
    ) {
        println("🔍 DEBUG - AuthRepository.saveAuthData(): Saving data...")
        println("🔍 DEBUG - Token: ${token.take(20)}...")
        println("🔍 DEBUG - Name: $name")
        println("🔍 DEBUG - Surname: $surname")
        println("🔍 DEBUG - Email: $email")
        println("🔍 DEBUG - UserId: $userId")
        println("🔍 DEBUG - UserType: $userType")
        
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[NAME_KEY] = name
            preferences[SURNAME_KEY] = surname
            preferences[EMAIL_KEY] = email
            preferences[USER_ID_KEY] = userId
            userType?.let { preferences[USER_TYPE_KEY] = it }
            
            println("🔍 DEBUG - AuthRepository.saveAuthData(): Saved ${preferences.asMap().size} entries to DataStore")
        }
        
        // Verify what was saved
        val savedUserId = getUserId().first()
        println("🔍 DEBUG - AuthRepository.saveAuthData(): Verification - retrieved user ID: '$savedUserId'")
        
        // Register device for 2FA notifications after successful authentication
        try {
            val fcmToken = getCurrentFcmToken()
            if (fcmToken != null) {
                Log.d("AuthRepository", "🔐 Registering device after successful authentication...")
                val result = registerDevice(fcmToken)
                result.onSuccess { deviceResponse ->
                    Log.d("AuthRepository", "✅ Post-login device registration successful")
                }.onFailure { exception ->
                    Log.e("AuthRepository", "❌ Post-login device registration failed: ${exception.message}")
                }
            } else {
                Log.w("AuthRepository", "⚠️ No FCM token available for device registration")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "💥 Error during post-login device registration: ${e.message}")
        }
    }
    
    private suspend fun getCurrentFcmToken(): String? {
        return try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get current FCM token: ${e.message}")
            null
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
            val userId = preferences[USER_ID_KEY]
            println("🔍 DEBUG - AuthRepository.getUserId(): Retrieved user ID: '$userId'")
            println("🔍 DEBUG - AuthRepository.getUserId(): DataStore contains ${preferences.asMap().size} entries")
            println("🔍 DEBUG - AuthRepository.getUserId(): All keys: ${preferences.asMap().keys}")
            userId
        }
    }

    fun getName(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[NAME_KEY]
        }
    }

    fun getSurname(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[SURNAME_KEY]
        }
    }

    fun getEmail(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[EMAIL_KEY]
        }
    }

    // Keep this for backward compatibility but it will return email now
    fun getUsername(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[EMAIL_KEY] // Return email as username for compatibility
        }
    }

    fun getUserType(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_TYPE_KEY]
        }
    }

    // Face 2FA state management
    suspend fun setFace2FAEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FACE_2FA_ENABLED_KEY] = enabled
        }
    }

    fun isFace2FAEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[FACE_2FA_ENABLED_KEY] ?: false
        }
    }

    // Push notification methods for 2FA
    suspend fun updateFcmToken(fcmToken: String) {
        val token = getAuthToken().first()
        if (!token.isNullOrEmpty()) {
            try {
                val response = authApi.updateFcmToken("Bearer $token", mapOf("fcm_token" to fcmToken))
                if (response.isSuccessful) {
                    println("🔍 DEBUG - AuthRepository: FCM token updated successfully")
                } else {
                    println("🔍 DEBUG - AuthRepository: FCM token update failed: ${response.code()}")
                }
            } catch (e: Exception) {
                println("🔍 DEBUG - AuthRepository: FCM token update exception: ${e.message}")
                throw e
            }
        }
    }

    suspend fun approvePendingAuth(pendingAuthId: String) {
        val token = getAuthToken().first()
        if (!token.isNullOrEmpty()) {
            try {
                val response = authApi.approvePendingAuth(
                    "Bearer $token",
                    mapOf("pendingAuthId" to pendingAuthId)
                )
                if (response.isSuccessful) {
                    println("🔍 DEBUG - AuthRepository: Login approved successfully")
                } else {
                    println("🔍 DEBUG - AuthRepository: Login approval failed: ${response.code()}")
                    throw Exception("Failed to approve login")
                }
            } catch (e: Exception) {
                println("🔍 DEBUG - AuthRepository: Login approval exception: ${e.message}")
                throw e
            }
        } else {
            throw Exception("Not authenticated")
        }
    }

    suspend fun denyPendingAuth(pendingAuthId: String) {
        val token = getAuthToken().first()
        if (!token.isNullOrEmpty()) {
            try {
                val response = authApi.denyPendingAuth(
                    "Bearer $token",
                    mapOf("pendingAuthId" to pendingAuthId)
                )
                if (response.isSuccessful) {
                    println("🔍 DEBUG - AuthRepository: Login denied successfully")
                } else {
                    println("🔍 DEBUG - AuthRepository: Login denial failed: ${response.code()}")
                    throw Exception("Failed to deny login")
                }
            } catch (e: Exception) {
                println("🔍 DEBUG - AuthRepository: Login denial exception: ${e.message}")
                throw e
            }
        } else {
            throw Exception("Not authenticated")
        }
    }

    // Method to verify password by attempting login
    suspend fun verifyPassword(email: String, password: String): Result<Boolean> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                Result.success(loginResponse.success)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Method to update user profile
    suspend fun updateUserProfile(
        userId: Int,
        name: String,
        surname: String,
        email: String,
        password: String,
        userType: String
    ): Result<User> {
        return try {
            val token = getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            val request = UserUpdateRequest(
                name = name,
                surname = surname,
                email = email,
                password = password,
                userType = userType
            )

            val response = authApi.updateUser(userId, "Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                val updatedUser = response.body()!!
                println("🔍 DEBUG - AuthRepository: Profile update successful, saving to DataStore")
                
                // Update local data store with new information
                saveAuthData(
                    token = token,
                    name = updatedUser.name,
                    surname = updatedUser.surname,
                    email = updatedUser.email,
                    userId = updatedUser.id.toString(),
                    userType = updatedUser.userType
                )
                
                Result.success(updatedUser)
            } else {
                println("🔍 DEBUG - AuthRepository: Update failed with code: ${response.code()}")
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gson = Gson()
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Update failed: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - AuthRepository: Update exception: ${e.message}")
            Result.failure(e)
        }
    }

    // 2FA Verification Methods
    suspend fun verifyTotpLogin(tempToken: String, code: String): Result<LoginResponse> {
        return try {
            val request = TotpLoginRequest(tempToken, code)
            val response = authApi.totpLogin(request)
            
            if (response.isSuccessful && response.body() != null) {
                val totpResponse = response.body()!!
                if (totpResponse.success) {
                    // Save auth data after successful TOTP verification
                    saveAuthData(
                        token = totpResponse.access_token,
                        name = totpResponse.user.name,
                        surname = totpResponse.user.surname,
                        email = totpResponse.user.email,
                        userId = totpResponse.user.id.toString(),
                        userType = totpResponse.user.userType
                    )
                    
                    println("🔍 DEBUG - AuthRepository: TOTP verification successful")
                    // Convert to LoginResponse for consistency
                    val loginResponse = LoginResponse(
                        success = totpResponse.success,
                        message = totpResponse.message,
                        access_token = totpResponse.access_token,
                        user = totpResponse.user,
                        twoFactorRequired = totpResponse.twoFactorRequired,
                        tempToken = totpResponse.tempToken,
                        available_2fa_methods = totpResponse.available_2fa_methods
                    )
                    Result.success(loginResponse)
                } else {
                    Result.failure(Exception(totpResponse.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gson = Gson()
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "TOTP verification failed: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - AuthRepository: TOTP verification exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun verifyFaceLogin(tempToken: String, faceImageFile: File): Result<LoginResponse> {
        return try {
            // Create multipart request
            val tempTokenBody = tempToken.toRequestBody("text/plain".toMediaTypeOrNull())
            val faceImageBody = faceImageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val faceImagePart = MultipartBody.Part.createFormData("face_image", faceImageFile.name, faceImageBody)
            
            val response = authApi.faceLogin(tempTokenBody, faceImagePart)
            
            if (response.isSuccessful && response.body() != null) {
                val faceResponse = response.body()!!
                if (faceResponse.success) {
                    // Save auth data after successful face verification
                    saveAuthData(
                        token = faceResponse.access_token,
                        name = faceResponse.user.name,
                        surname = faceResponse.user.surname,
                        email = faceResponse.user.email,
                        userId = faceResponse.user.id.toString(),
                        userType = faceResponse.user.userType
                    )
                    
                    println("🔍 DEBUG - AuthRepository: Face verification successful")
                    // Convert to LoginResponse for consistency
                    val loginResponse = LoginResponse(
                        success = faceResponse.success,
                        message = faceResponse.message,
                        access_token = faceResponse.access_token,
                        user = faceResponse.user,
                        twoFactorRequired = faceResponse.twoFactorRequired,
                        tempToken = faceResponse.tempToken,
                        available_2fa_methods = faceResponse.available_2fa_methods
                    )
                    Result.success(loginResponse)
                } else {
                    Result.failure(Exception(faceResponse.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gson = Gson()
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Face verification failed: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - AuthRepository: Face verification exception: ${e.message}")
            Result.failure(e)
        }
    }

    // Device registration for 2FA notifications
    suspend fun registerDevice(fcmToken: String): Result<DeviceRegistrationResponse> {
        return try {
            val token = getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            Log.d("AuthRepository", "🔐 Registering device for 2FA notifications...")
            
            // Get device information
            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver, 
                android.provider.Settings.Secure.ANDROID_ID
            )
            val appVersion = try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName
            } catch (e: Exception) {
                "1.0.0"
            }

            val request = DeviceRegistrationRequest(
                fcmToken = fcmToken,
                platform = "android",
                deviceName = deviceName,
                deviceId = deviceId,
                appVersion = appVersion
            )

            Log.d("AuthRepository", "📱 Device Info - Name: $deviceName, ID: ${deviceId?.take(8)}..., Version: $appVersion")
            
            val response = authApi.registerDevice("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                val deviceResponse = response.body()!!
                Log.d("AuthRepository", "✅ Device registration successful: ${deviceResponse.message}")
                Result.success(deviceResponse)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val gson = Gson()
                    val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Device registration failed: ${response.message()}"
                }
                Log.e("AuthRepository", "❌ Device registration failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "💥 Device registration exception: ${e.message}")
            Result.failure(e)
        }
    }
}