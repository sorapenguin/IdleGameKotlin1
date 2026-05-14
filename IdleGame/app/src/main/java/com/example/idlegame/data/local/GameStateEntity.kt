package com.example.idlegame.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Long,
    val gems: Int,
    val lastSaveTime: Long,
    val weaponsJson: String,
    val weaponSlots: Int,
    val stage: Long,
    val autoDeleteLevel: Int,
    val starGenLevelsJson: String,
    val coinAttackLevel: Int,
    val prestigeStones: Int,
    val prestigeUpgradesJson: String,
    val maxMilestoneReached: Int,
    val achievementsClaimedJson: String,
    val totalEnemiesDefeated: Long,
    val totalCoinsEarned: Long,
    val gemAdWatchedToday: Int,
    val gemAdLastDate: String,
    val lastGemAdTime: Long,
    val lastCoinAdTime: Long,
    val attackBoostEndTime: Long,
    val penaltyShieldActive: Boolean,
    val lastAttackBoostAdTime: Long,
    val lastShieldAdTime: Long
)
