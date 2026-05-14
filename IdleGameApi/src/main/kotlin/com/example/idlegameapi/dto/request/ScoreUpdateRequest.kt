package com.example.idlegameapi.dto.request

import jakarta.validation.constraints.Min

data class ScoreUpdateRequest(
    @field:Min(0, message = "Total kills cannot be negative")
    val totalKills: Long,

    @field:Min(1, message = "Max stage must be at least 1")
    val maxStageReached: Int,

    @field:Min(0, message = "Total coins earned cannot be negative")
    val totalCoinsEarned: Long
)
