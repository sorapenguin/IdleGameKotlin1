package com.example.idlegame.network

import com.example.idlegame.network.dto.ApiResponse
import com.example.idlegame.network.dto.AuthResponse
import com.example.idlegame.network.dto.CloudSaveRequest
import com.example.idlegame.network.dto.GameSaveRequest
import com.example.idlegame.network.dto.GameStateResponse
import com.example.idlegame.network.dto.LoginRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("api/auth/cloud-save")
    suspend fun cloudSave(@Body request: CloudSaveRequest): ApiResponse<AuthResponse>

    @GET("api/time")
    suspend fun serverTime(): ApiResponse<Long>

    @POST("api/game/save")
    suspend fun saveGameState(
        @Header("Authorization") bearerToken: String,
        @Body request: GameSaveRequest
    ): ApiResponse<GameStateResponse>
}
