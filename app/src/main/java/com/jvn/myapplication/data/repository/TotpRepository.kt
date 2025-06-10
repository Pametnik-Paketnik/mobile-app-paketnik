package com.jvn.myapplication.data.repository

import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.api.TotpSetupResponse
import com.jvn.myapplication.data.api.TotpDisableResponse
import kotlinx.coroutines.flow.first

class TotpRepository(
    private val authRepository: AuthRepository
) {
    private val totpApi = NetworkModule.totpApi

    suspend fun setupTotp(): Result<TotpSetupResponse> {
        return try {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - TotpRepository: Setting up TOTP")
            
            val response = totpApi.setupTotp("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                val totpResponse = response.body()!!
                println("🔍 DEBUG - TotpRepository: TOTP setup successful")
                println("🔍 DEBUG - Secret: ${totpResponse.secret}")
                println("🔍 DEBUG - Manual Entry Key: ${totpResponse.manualEntryKey}")
                Result.success(totpResponse)
            } else {
                val errorMessage = "TOTP setup failed: ${response.message()}"
                println("🔍 DEBUG - TotpRepository: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - TotpRepository: Exception in setupTotp: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun disableTotp(): Result<TotpDisableResponse> {
        return try {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - TotpRepository: Disabling TOTP")
            
            val response = totpApi.disableTotp("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                val totpResponse = response.body()!!
                println("🔍 DEBUG - TotpRepository: TOTP disable successful: ${totpResponse.message}")
                Result.success(totpResponse)
            } else {
                val errorMessage = "TOTP disable failed: ${response.message()}"
                println("🔍 DEBUG - TotpRepository: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - TotpRepository: Exception in disableTotp: ${e.message}")
            Result.failure(e)
        }
    }
} 