package com.example.idlegame.network

import com.example.idlegame.data.GameState
import com.example.idlegame.network.dto.AuthResponse
import com.example.idlegame.network.dto.CloudSaveRequest
import com.example.idlegame.network.dto.GameSaveRequest
import com.example.idlegame.network.dto.GameStateResponse
import com.example.idlegame.network.dto.LoginRequest

class ApiRepository {
    private val api = RetrofitClient.apiService

    suspend fun login(username: String, password: String): ApiResult<AuthResponse> =
        SafeApiWrapper.call {
            val resp = api.login(LoginRequest(username, password))
            resp.data ?: error(resp.message)
        }

    suspend fun cloudSave(password: String): ApiResult<AuthResponse> =
        SafeApiWrapper.call {
            val resp = api.cloudSave(CloudSaveRequest(password))
            resp.data ?: error(resp.message)
        }

    suspend fun getServerTime(): ApiResult<Long> =
        SafeApiWrapper.call {
            val resp = api.serverTime()
            resp.data ?: error(resp.message)
        }

    suspend fun saveGameState(token: String, state: GameState): ApiResult<GameStateResponse> =
        SafeApiWrapper.call {
            val req = GameSaveRequest(
                stage                = state.stage,
                coins                = state.coins,
                gems                 = state.gems,
                totalAttack          = state.totalAttack(),
                weaponSlots          = state.weaponSlots,
                maxMilestoneReached  = state.maxMilestoneReached,
                totalEnemiesDefeated = state.totalEnemiesDefeated,
                totalCoinsEarned     = state.totalCoinsEarned
            )
            val resp = api.saveGameState("Bearer $token", req)
            resp.data ?: error(resp.message)
        }
}
