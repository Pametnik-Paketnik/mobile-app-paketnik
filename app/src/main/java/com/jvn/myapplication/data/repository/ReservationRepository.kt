package com.jvn.myapplication.data.repository

import android.content.Context
import com.jvn.myapplication.data.api.NetworkModule
import com.jvn.myapplication.data.api.CreateReservationRequest
import com.jvn.myapplication.data.api.ReservationResponse
import com.jvn.myapplication.data.model.CheckInRequest
import com.jvn.myapplication.data.model.CheckInResponse
import com.jvn.myapplication.data.model.Reservation
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class ReservationRepository(private val context: Context) {
    private val reservationApi = NetworkModule.reservationApi
    private val authRepository = AuthRepository(context)

    suspend fun getReservationsByGuest(guestId: Int): Result<List<Reservation>> {
        return try {
            println("🔍 DEBUG - ReservationRepository.getReservationsByGuest(): Starting for guestId: $guestId")
            
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                println("🔍 DEBUG - ReservationRepository.getReservationsByGuest(): ERROR - No authentication token")
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - ReservationRepository.getReservationsByGuest(): Token found, length: ${token.length}")
            println("🔍 DEBUG - ReservationRepository.getReservationsByGuest(): Making API call to /api/reservations/guest/$guestId")
            
            val response = reservationApi.getReservationsByGuest(guestId.toString(), "Bearer $token")
            
            println("🔍 DEBUG - ReservationRepository.getReservationsByGuest(): API response received")
            println("🔍 DEBUG - Response code: ${response.code()}")
            println("🔍 DEBUG - Response successful: ${response.isSuccessful}")
            println("🔍 DEBUG - Response body null: ${response.body() == null}")
            
            if (response.isSuccessful && response.body() != null) {
                val reservations = response.body()!!
                println("🔍 DEBUG - ReservationRepository: Got ${reservations.size} reservations")
                reservations.forEachIndexed { index, reservation ->
                    println("🔍 DEBUG - Reservation $index: ID=${reservation.id}, Status=${reservation.status}, GuestId=${reservation.guest?.id}, BoxId=${reservation.box?.boxId}")
                }
                Result.success(reservations)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to fetch reservations: ${response.message()}"
                println("🔍 DEBUG - ReservationRepository: $errorMessage")
                println("🔍 DEBUG - Error body: $errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - ReservationRepository: Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun checkIn(reservationId: Int): Result<CheckInResponse> {
        return try {
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - ReservationRepository: Checking in reservation $reservationId")
            val request = CheckInRequest(reservationId)
            val response = reservationApi.checkIn("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                val checkInResponse = response.body()!!
                println("🔍 DEBUG - ReservationRepository: Check-in response: ${checkInResponse.message}")
                Result.success(checkInResponse)
            } else {
                val errorMessage = "Check-in failed: ${response.message()}"
                println("🔍 DEBUG - ReservationRepository: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - ReservationRepository: Check-in exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getReservationsByHost(hostId: Int): Result<List<Reservation>> {
        return try {
            println("🔍 DEBUG - ReservationRepository.getReservationsByHost(): Starting for hostId: $hostId")
            
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                println("🔍 DEBUG - ReservationRepository.getReservationsByHost(): ERROR - No authentication token")
                return Result.failure(Exception("No authentication token"))
            }

            println("🔍 DEBUG - ReservationRepository.getReservationsByHost(): Token found, making API call to /api/reservations/host/$hostId")
            
            val response = reservationApi.getReservationsByHost(hostId.toString(), "Bearer $token")
            
            println("🔍 DEBUG - ReservationRepository.getReservationsByHost(): API response received")
            println("🔍 DEBUG - Response code: ${response.code()}")
            println("🔍 DEBUG - Response successful: ${response.isSuccessful}")
            println("🔍 DEBUG - Response body null: ${response.body() == null}")
            
            if (response.isSuccessful && response.body() != null) {
                val reservations = response.body()!!
                println("🔍 DEBUG - ReservationRepository: Got ${reservations.size} reservations for host")
                reservations.forEachIndexed { index, reservation ->
                    println("🔍 DEBUG - Reservation $index: ID=${reservation.id}, Status=${reservation.status}, HostId=${reservation.host?.id}, BoxId=${reservation.box?.boxId}")
                }
                Result.success(reservations)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to fetch host reservations: ${response.message()}"
                println("🔍 DEBUG - ReservationRepository: $errorMessage")
                println("🔍 DEBUG - Error body: $errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - ReservationRepository: Exception in getReservationsByHost: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createReservation(
        guestId: Int,
        hostId: Int,
        boxId: String,
        checkinAt: String,
        checkoutAt: String
    ): Result<ReservationResponse> {
        return try {
            println("🔍 DEBUG - ReservationRepository.createReservation(): Starting reservation creation")
            println("🔍 DEBUG - guestId: $guestId, hostId: $hostId, boxId: $boxId")
            println("🔍 DEBUG - checkinAt: $checkinAt, checkoutAt: $checkoutAt")
            
            val token = authRepository.getAuthToken().first()
            if (token.isNullOrEmpty()) {
                println("🔍 DEBUG - ReservationRepository.createReservation(): ERROR - No authentication token")
                return Result.failure(Exception("No authentication token"))
            }

            val request = CreateReservationRequest(
                guestId = guestId,
                hostId = hostId,
                boxId = boxId,
                checkinAt = checkinAt,
                checkoutAt = checkoutAt
            )

            println("🔍 DEBUG - ReservationRepository.createReservation(): Making API call to /api/reservations")
            val response = reservationApi.createReservation(request, "Bearer $token")
            
            println("🔍 DEBUG - ReservationRepository.createReservation(): API response received")
            println("🔍 DEBUG - Response code: ${response.code()}")
            println("🔍 DEBUG - Response successful: ${response.isSuccessful}")
            
            if (response.isSuccessful && response.body() != null) {
                val reservationResponse = response.body()!!
                println("🔍 DEBUG - ReservationRepository: Reservation created successfully")
                println("🔍 DEBUG - Reservation ID: ${reservationResponse.id}")
                println("🔍 DEBUG - Status: ${reservationResponse.status}")
                Result.success(reservationResponse)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to create reservation: ${response.message()}"
                println("🔍 DEBUG - ReservationRepository: $errorMessage")
                println("🔍 DEBUG - Error body: $errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("🔍 DEBUG - ReservationRepository: Exception in createReservation: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            timestamp // Return original if parsing fails
        }
    }
} 