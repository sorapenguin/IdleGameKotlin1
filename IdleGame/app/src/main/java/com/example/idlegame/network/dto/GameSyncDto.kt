package com.example.idlegame.network.dto

data class GameSaveRequest(
    val stage: Long,
    val coins: Long,
    val gems: Int,
    val totalAttack: Long,
    val weaponSlots: Int,
    val maxMilestoneReached: Int,
    val totalEnemiesDefeated: Long,
    val totalCoinsEarned: Long
)

data class GameStateResponse(
    val stage: Long,
    val coins: Long,
    val gems: Int,
    val totalAttack: Long,
    val weaponSlots: Int,
    val lastSavedAt: String
)
