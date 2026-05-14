package com.example.idlegameapi.dto.response

import java.time.LocalDateTime

data class ScoreResponse(
    val userId: Long,
    val totalKills: Long,
    val maxStageReached: Int,
    val totalCoinsEarned: Long,
    val updatedAt: LocalDateTime
)
