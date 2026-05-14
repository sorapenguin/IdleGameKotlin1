package com.example.idlegameapi.dto.response

import java.time.LocalDateTime

data class GameStateResponse(
    val id: Long,
    val userId: Long,
    val stage: Int,
    val coins: Long,
    val gems: Int,
    val totalAttack: Long,
    val weaponSlots: Int,
    val energy: Int,
    val lastSavedAt: LocalDateTime
)
