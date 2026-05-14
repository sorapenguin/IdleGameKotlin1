package com.example.idlegame.data

data class GameState(
    val coins: Long = 0L,
    val gems: Int = 0,
    val lastSaveTime: Long = System.currentTimeMillis(),
    val weapons: Map<Int, Int> = emptyMap(),
    val weaponSlots: Int = 5,
    val stage: Long = 1L,
    val autoDeleteLevel: Int = 0,
    val starGenLevels: Map<Int, Int> = emptyMap(),  // star → 0=ロック, 1-50=確率レベル
    val coinAttackLevel: Int = 0,                    // 購入回数（0=未購入）
    val prestigeStones: Int = 0,
    val prestigeUpgrades: Map<Int, Int> = emptyMap(), // upgradeId → level
    val maxMilestoneReached: Int = 0,                  // 敗北農場防止: 到達済み最大(stage/100)
    val achievementsClaimed: Map<String, Int> = emptyMap(),
    val totalEnemiesDefeated: Long = 0L,
    val totalCoinsEarned: Long = 0L,
    val gemAdWatchedToday: Int = 0,
    val gemAdLastDate: String = "",
    val lastGemAdTime: Long = 0L,
    val lastCoinAdTime: Long = 0L,
    val attackBoostEndTime: Long = 0L,        // 緊急強化広告の終了時刻
    val penaltyShieldActive: Boolean = false, // 落下防止シールド（次回敗北1回無効）
    val lastAttackBoostAdTime: Long = 0L,
    val lastShieldAdTime: Long = 0L
) {
    fun totalWeapons(): Int = weapons.values.sum()
    fun isAttackBoosted(): Boolean = System.currentTimeMillis() < attackBoostEndTime
    fun boostRemainingMs(): Long = maxOf(0L, attackBoostEndTime - System.currentTimeMillis())

    fun totalAttack(): Long {
        val base = weapons.entries.sumOf { (level, count) -> starAttack(level) * count } + coinAttackBonus()
        val boostMul = if (isAttackBoosted()) 2.0 else 1.0
        return (base * prestigeAttackMultiplier() * boostMul).toLong()
    }
    fun weaponSlotExpandCost(): Long = 100L shl (weaponSlots - 5)

    // coinAttackLevel 回購入済みのときの累計攻撃力ボーナス: 2^0+2^1+...+2^(n-1) = 2^n - 1
    fun coinAttackBonus(): Long = if (coinAttackLevel == 0) 0L else (1L shl coinAttackLevel) - 1
    // 次の購入コスト（Lv n+1 は 2^n コイン）
    fun coinAttackNextCost(): Long = 1L shl coinAttackLevel
    // 次の購入で得られる攻撃力増加量（コストと同値）
    fun coinAttackNextBonus(): Long = 1L shl coinAttackLevel
    fun bossMultiplier(): Int = when (stage) {
        100L   -> 2
        300L   -> 3
        500L   -> 5
        1000L  -> 7
        10000L -> 10
        else   -> 1
    }
    fun isBossStage(): Boolean = bossMultiplier() > 1
    fun enemyHp(): Long = stage * bossMultiplier()
    fun maxWeaponLevel(): Int = weapons.keys.maxOrNull() ?: 0
    fun coinAdReward(): Long = maxOf(1000L, maxMilestoneReached.toLong() * 100L * 10L)
    fun maxAutoDeleteLevel(): Int = maxOf(0, maxWeaponLevel() - 1)

    fun isStarUnlocked(star: Int): Boolean = (starGenLevels[star] ?: 0) > 0
    fun starGenLevel(star: Int): Int = starGenLevels[star] ?: 0
    fun canUnlockStar(star: Int): Boolean {
        if (star < 2 || isStarUnlocked(star)) return false
        return if (star == 2) true else starGenLevel(star - 1) >= 10
    }
    // currentLevel → nextLevel へのコスト: (currentLevel+1) × star × 2
    fun starUpgradeCost(star: Int, currentLevel: Int): Int = (currentLevel + 1) * star * 2
    fun starUnlockCost(star: Int): Int = star * 100

    // --- 恒久アップグレード ---
    fun prestigeUpgradeLevel(id: Int): Int = prestigeUpgrades[id] ?: 0
    fun prestigeUpgradeMax(id: Int): Int = when (id) {
        PRESTIGE_ATTACK    -> 20
        PRESTIGE_COIN      -> 10
        PRESTIGE_OFFLINE   -> 8
        PRESTIGE_GEM_DROP  -> 10
        else               -> 0
    }
    // 次レベル購入コスト（現在レベルから+1する費用）
    fun prestigeUpgradeCost(id: Int): Int {
        val lv = prestigeUpgradeLevel(id)
        return when (id) {
            PRESTIGE_OFFLINE -> (lv + 1) * 2   // 2, 4, 6...
            else             -> lv + 1          // 1, 2, 3...
        }
    }
    fun prestigeAttackMultiplier(): Double = 1.0 + 0.05 * prestigeUpgradeLevel(PRESTIGE_ATTACK)
    fun prestigeCoinMultiplier(): Double   = 1.0 + 0.10 * prestigeUpgradeLevel(PRESTIGE_COIN)
    fun prestigeOfflineHours(): Int        = 8 + prestigeUpgradeLevel(PRESTIGE_OFFLINE)
    fun prestigeGemDropRate(): Float       = GEM_DROP_CHANCE + 0.01f * prestigeUpgradeLevel(PRESTIGE_GEM_DROP)

    fun achievementTimesEarned(def: AchievementDef): Int {
        val times = when (def.statKey) {
            "enemies" -> (totalEnemiesDefeated / def.threshold).toInt()
            "stage"   -> (maxMilestoneReached.toLong() * 100L / def.threshold).toInt()
            "coins"   -> (totalCoinsEarned / def.threshold).toInt()
            else      -> 0
        }
        return if (def.oneTime) minOf(times, 1) else times
    }
    fun achievementClaimable(def: AchievementDef): Int =
        maxOf(0, achievementTimesEarned(def) - (achievementsClaimed[def.id] ?: 0))

    companion object {
        const val ENEMIES_PER_MINUTE = 60L
        const val STAGE_PENALTY = 200L
        const val GEM_DROP_CHANCE = 0.05f
        const val COIN_ATTACK_MAX_LEVEL = 62  // Long 溢れ防止上限

        val BOSS_STAGES = listOf(100L, 300L, 500L, 1000L, 10000L)

        data class AchievementDef(
            val id: String,
            val title: String,
            val description: String,
            val threshold: Long,
            val rewardGems: Int,
            val statKey: String,
            val oneTime: Boolean = false
        )

        val ACHIEVEMENTS: List<AchievementDef> = listOf(
            AchievementDef("kill_1k",  "1000体撃破",   "敵1000体撃破ごと",       1_000L, 5,  "enemies"),
            AchievementDef("stage_ms", "ステージ到達", "ステージ100の倍数ごと",    100L,  10, "stage"),
        )

        const val GEM_AD_DAILY_LIMIT  = 5
        const val GEM_AD_REWARD       = 10
        const val COIN_AD_COOLDOWN_MS = 10 * 60 * 1000L
        const val ATTACK_BOOST_DURATION_MS    = 10 * 60 * 1000L  // 10分
        const val ATTACK_BOOST_AD_COOLDOWN_MS = 15 * 60 * 1000L  // 15分クールダウン
        const val SHIELD_AD_COOLDOWN_MS       = 30 * 60 * 1000L  // 30分クールダウン

        const val PRESTIGE_ATTACK   = 1
        const val PRESTIGE_COIN     = 2
        const val PRESTIGE_OFFLINE  = 3
        const val PRESTIGE_GEM_DROP = 4

        fun starAttack(level: Int): Long {
            var atk = 10L
            repeat(level - 1) { atk = (atk * 2.2).toLong() }
            return atk
        }
    }
}
