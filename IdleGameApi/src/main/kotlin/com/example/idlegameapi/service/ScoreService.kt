package com.example.idlegameapi.service

import com.example.idlegameapi.dto.request.ScoreUpdateRequest
import com.example.idlegameapi.dto.response.ScoreResponse
import com.example.idlegameapi.exception.ResourceNotFoundException
import com.example.idlegameapi.repository.ScoreRepository
import com.example.idlegameapi.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScoreService(
    private val scoreRepository: ScoreRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun updateScore(username: String, request: ScoreUpdateRequest): ScoreResponse {
        val user = userRepository.findByUsername(username)
            ?: throw ResourceNotFoundException("User not found: $username")
        val score = scoreRepository.findByUser(user)
            ?: throw ResourceNotFoundException("Score not found for user: $username")

        // Only update if the new value is higher (prevent rollback cheating)
        score.apply {
            if (request.totalKills > totalKills) totalKills = request.totalKills
            if (request.maxStageReached > maxStageReached) maxStageReached = request.maxStageReached
            if (request.totalCoinsEarned > totalCoinsEarned) totalCoinsEarned = request.totalCoinsEarned
        }

        return scoreRepository.save(score).let { s ->
            ScoreResponse(
                userId = user.id,
                totalKills = s.totalKills,
                maxStageReached = s.maxStageReached,
                totalCoinsEarned = s.totalCoinsEarned,
                updatedAt = s.updatedAt
            )
        }
    }
}
