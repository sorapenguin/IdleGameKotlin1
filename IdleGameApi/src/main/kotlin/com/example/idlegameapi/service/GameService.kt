package com.example.idlegameapi.service

import com.example.idlegameapi.dto.request.GameSaveRequest
import com.example.idlegameapi.dto.response.GameStateResponse
import com.example.idlegameapi.exception.ResourceNotFoundException
import com.example.idlegameapi.repository.GameStateRepository
import com.example.idlegameapi.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameService(
    private val gameStateRepository: GameStateRepository,
    private val userRepository: UserRepository
) {

    fun getGameState(username: String): GameStateResponse {
        val user = userRepository.findByUsername(username)
            ?: throw ResourceNotFoundException("User not found: $username")
        val state = gameStateRepository.findByUser(user)
            ?: throw ResourceNotFoundException("Game state not found for user: $username")
        return state.toResponse()
    }

    @Transactional
    fun saveGameState(username: String, request: GameSaveRequest): GameStateResponse {
        val user = userRepository.findByUsername(username)
            ?: throw ResourceNotFoundException("User not found: $username")
        val state = gameStateRepository.findByUser(user)
            ?: throw ResourceNotFoundException("Game state not found for user: $username")

        state.apply {
            stage = request.stage
            coins = request.coins
            gems = request.gems
            totalAttack = request.totalAttack
            weaponSlots = request.weaponSlots
            energy = request.energy
        }

        return gameStateRepository.save(state).toResponse()
    }

    private fun com.example.idlegameapi.entity.GameState.toResponse() = GameStateResponse(
        id = id,
        userId = user.id,
        stage = stage,
        coins = coins,
        gems = gems,
        totalAttack = totalAttack,
        weaponSlots = weaponSlots,
        energy = energy,
        lastSavedAt = lastSavedAt
    )
}
