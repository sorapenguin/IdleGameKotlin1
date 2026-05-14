package com.example.idlegame.data

import com.example.idlegame.data.local.GameStateDao
import com.example.idlegame.data.local.GameStateEntity

class GameRepository(private val dao: GameStateDao) {

    data class OfflineResult(val minutes: Long, val coins: Long)

    suspend fun save(state: GameState) {
        dao.upsert(state.toEntity())
    }

    suspend fun load(): GameState {
        return dao.get()?.toDomain() ?: GameState()
    }

    fun calculateOfflineResult(state: GameState, serverEpochMs: Long): OfflineResult? {
        val elapsedMinutes = (serverEpochMs - state.lastSaveTime) / 60_000L
        if (elapsedMinutes < 30L) return null
        val maxMinutes = state.prestigeOfflineHours() * 60L
        val minutes = minOf(elapsedMinutes, maxMinutes)
        val atk = state.totalAttack()
        val enemyHp = state.enemyHp()
        if (atk <= enemyHp) return null
        val baseCoins = GameState.ENEMIES_PER_MINUTE * enemyHp * minutes
        val coins = (baseCoins * state.prestigeCoinMultiplier()).toLong()
        return OfflineResult(minutes, coins)
    }
}

// ---------- GameState ↔ GameStateEntity 変換 ----------

private fun serializeIntMap(map: Map<Int, Int>): String =
    map.entries.filter { it.value > 0 }.joinToString(",") { "${it.key}:${it.value}" }

private fun deserializeIntMap(str: String): Map<Int, Int> {
    if (str.isBlank()) return emptyMap()
    return str.split(",").associate { part ->
        val (key, value) = part.split(":")
        key.toInt() to value.toInt()
    }
}

private fun serializeStringIntMap(map: Map<String, Int>): String =
    map.entries.joinToString(",") { "${it.key}=${it.value}" }

private fun deserializeStringIntMap(str: String): Map<String, Int> {
    if (str.isBlank()) return emptyMap()
    return str.split(",").mapNotNull { part ->
        val idx = part.indexOf('=')
        if (idx < 0) null else part.substring(0, idx) to part.substring(idx + 1).toInt()
    }.toMap()
}

fun GameState.toEntity() = GameStateEntity(
    coins                 = coins,
    gems                  = gems,
    lastSaveTime          = System.currentTimeMillis(),
    weaponsJson           = serializeIntMap(weapons),
    weaponSlots           = weaponSlots,
    stage                 = stage,
    autoDeleteLevel       = autoDeleteLevel,
    starGenLevelsJson     = serializeIntMap(starGenLevels),
    coinAttackLevel       = coinAttackLevel,
    prestigeStones        = prestigeStones,
    prestigeUpgradesJson  = serializeIntMap(prestigeUpgrades),
    maxMilestoneReached   = maxMilestoneReached,
    achievementsClaimedJson = serializeStringIntMap(achievementsClaimed),
    totalEnemiesDefeated  = totalEnemiesDefeated,
    totalCoinsEarned      = totalCoinsEarned,
    gemAdWatchedToday     = gemAdWatchedToday,
    gemAdLastDate         = gemAdLastDate,
    lastGemAdTime         = lastGemAdTime,
    lastCoinAdTime        = lastCoinAdTime,
    attackBoostEndTime    = attackBoostEndTime,
    penaltyShieldActive   = penaltyShieldActive,
    lastAttackBoostAdTime = lastAttackBoostAdTime,
    lastShieldAdTime      = lastShieldAdTime
)

fun GameStateEntity.toDomain() = GameState(
    coins                = coins,
    gems                 = gems,
    lastSaveTime         = lastSaveTime,
    weapons              = deserializeIntMap(weaponsJson),
    weaponSlots          = weaponSlots,
    stage                = stage,
    autoDeleteLevel      = autoDeleteLevel,
    starGenLevels        = deserializeIntMap(starGenLevelsJson),
    coinAttackLevel      = coinAttackLevel,
    prestigeStones       = prestigeStones,
    prestigeUpgrades     = deserializeIntMap(prestigeUpgradesJson),
    maxMilestoneReached  = maxMilestoneReached,
    achievementsClaimed  = deserializeStringIntMap(achievementsClaimedJson),
    totalEnemiesDefeated = totalEnemiesDefeated,
    totalCoinsEarned     = totalCoinsEarned,
    gemAdWatchedToday    = gemAdWatchedToday,
    gemAdLastDate        = gemAdLastDate,
    lastGemAdTime        = lastGemAdTime,
    lastCoinAdTime       = lastCoinAdTime,
    attackBoostEndTime   = attackBoostEndTime,
    penaltyShieldActive  = penaltyShieldActive,
    lastAttackBoostAdTime = lastAttackBoostAdTime,
    lastShieldAdTime     = lastShieldAdTime
)
