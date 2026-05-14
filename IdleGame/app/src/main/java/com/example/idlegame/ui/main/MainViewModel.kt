package com.example.idlegame.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.idlegame.BuildConfig
import com.example.idlegame.data.GameRepository
import com.example.idlegame.data.GameState
import com.example.idlegame.network.ApiRepository
import com.example.idlegame.network.dataOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainViewModel(
    private val repository: GameRepository,
    private val apiRepository: ApiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    private val _battleLog = MutableStateFlow<List<String>>(emptyList())
    val battleLog: StateFlow<List<String>> = _battleLog

    private val _isIdleSleeping = MutableStateFlow(false)
    val isIdleSleeping: StateFlow<Boolean> = _isIdleSleeping

    // 未受け取りのオフライン報酬。nullなら表示しない。
    private val _pendingOffline = MutableStateFlow<GameRepository.OfflineResult?>(null)
    val pendingOffline: StateFlow<GameRepository.OfflineResult?> = _pendingOffline

    private var secondCount = 0
    private var minuteCount = 0
    private var tickerJob: Job? = null
    private var lastInteractionTime = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            _state.value = repository.load()
            checkOfflineReward()
            startTicking()
        }
    }

    // サーバー時刻を取得できた場合のみ報酬を計算する。オフライン・エラー時はスキップ。
    private suspend fun checkOfflineReward() {
        val serverEpochMs = apiRepository.getServerTime().dataOrNull() ?: return
        val offline = repository.calculateOfflineResult(_state.value, serverEpochMs)
        if (offline != null) _pendingOffline.value = offline
    }

    private fun startTicking() {
        tickerJob?.cancel()
        lastInteractionTime = System.currentTimeMillis()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                // DEBUGビルドのみ: 5分無操作でアイドルスリープ
                if (BuildConfig.DEBUG &&
                    System.currentTimeMillis() - lastInteractionTime > IDLE_TIMEOUT_MS) {
                    _isIdleSleeping.value = true
                    saveGame()
                    break
                }
                secondCount++
                processSecond()
                if (secondCount % 60 == 0) {
                    minuteCount++
                    processBattle()
                }
            }
        }
    }

    fun pauseTicking() {
        tickerJob?.cancel()
        tickerJob = null
        saveGame()
    }

    fun resumeTicking() {
        if (tickerJob?.isActive == true) return
        _isIdleSleeping.value = false
        viewModelScope.launch {
            checkOfflineReward()
            startTicking()
        }
    }

    fun recordInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        if (_isIdleSleeping.value) {
            resumeTicking()
        }
    }

    // オフライン報酬を受け取る。doubled=trueなら広告視聴で2倍。
    fun collectOfflineEarnings(doubled: Boolean) {
        val result = _pendingOffline.value ?: return
        _pendingOffline.value = null
        val coins = if (doubled) result.coins * 2L else result.coins
        val s = _state.value
        _state.value = s.copy(
            coins = s.coins + coins,
            totalCoinsEarned = s.totalCoinsEarned + coins
        )
        saveGame()
    }

    // 緊急強化広告: 10分間攻撃力×2
    fun watchAttackBoostAd() {
        val s = _state.value
        val now = System.currentTimeMillis()
        if (now - s.lastAttackBoostAdTime < GameState.ATTACK_BOOST_AD_COOLDOWN_MS) return
        _state.value = s.copy(
            attackBoostEndTime    = now + GameState.ATTACK_BOOST_DURATION_MS,
            lastAttackBoostAdTime = now
        )
    }

    // 落下防止シールド広告: 次回の敗北ペナルティを1回無効
    fun watchShieldAd() {
        val s = _state.value
        val now = System.currentTimeMillis()
        if (now - s.lastShieldAdTime < GameState.SHIELD_AD_COOLDOWN_MS) return
        _state.value = s.copy(
            penaltyShieldActive = true,
            lastShieldAdTime    = now
        )
    }

    private fun processSecond() {
        val original = _state.value
        var s = original

        if (s.totalWeapons() < s.weaponSlots) {
            val star = generateWeaponStar(s)
            val w = s.weapons.toMutableMap()
            w[star] = (w[star] ?: 0) + 1
            s = s.copy(weapons = w)
        }

        if (s.autoDeleteLevel > 0) {
            val filtered = s.weapons.filterKeys { it > s.autoDeleteLevel }
            if (filtered.size != s.weapons.size) s = s.copy(weapons = filtered)
        }

        if (s !== original) _state.value = s
    }

    private fun generateWeaponStar(s: GameState): Int {
        val unlocked = s.starGenLevels.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.key }
        for ((star, level) in unlocked) {
            if (Random.nextFloat() < level / 100f) return star
        }
        return 1
    }

    fun setAutoDeleteLevel(level: Int) {
        val s = _state.value
        val clamped = level.coerceIn(0, s.maxAutoDeleteLevel())
        if (s.autoDeleteLevel != clamped) _state.value = s.copy(autoDeleteLevel = clamped)
    }

    fun unlockStar(star: Int) {
        val s = _state.value
        if (!s.canUnlockStar(star)) return
        val cost = s.starUnlockCost(star)
        if (s.gems < cost) return
        val newLevels = s.starGenLevels.toMutableMap()
        newLevels[star] = 1
        _state.value = s.copy(gems = s.gems - cost, starGenLevels = newLevels)
    }

    fun upgradeStarGen(star: Int) {
        val s = _state.value
        if (!s.isStarUnlocked(star)) return
        val currentLevel = s.starGenLevel(star)
        if (currentLevel >= 50) return
        val cost = s.starUpgradeCost(star, currentLevel)
        if (s.gems < cost) return
        val newLevels = s.starGenLevels.toMutableMap()
        newLevels[star] = currentLevel + 1
        _state.value = s.copy(gems = s.gems - cost, starGenLevels = newLevels)
    }

    fun addCoins(amount: Long) {
        _state.value = _state.value.copy(coins = _state.value.coins + amount)
    }

    private fun processBattle() {
        val s = _state.value
        val atk = s.totalAttack()
        val enemyHp = s.enemyHp()
        val stageBefore = s.stage
        val isBoss = s.isBossStage()
        val bossPrefix = if (isBoss) "★BOSS★ " else ""

        if (atk > enemyHp) {
            val tentative = stageBefore + GameState.ENEMIES_PER_MINUTE
            val nextBoss = if (isBoss) null
                else GameState.BOSS_STAGES.firstOrNull { it > stageBefore && it <= tentative }
            val stageAfter = nextBoss ?: tentative

            val baseCoins = GameState.ENEMIES_PER_MINUTE * enemyHp
            val battleCoins = (baseCoins * s.prestigeCoinMultiplier()).toLong()

            var gemsEarned = 0
            val gemRate = s.prestigeGemDropRate()
            repeat(GameState.ENEMIES_PER_MINUTE.toInt()) {
                if (Random.nextFloat() < gemRate) gemsEarned++
            }

            val newMilestone = (stageAfter / 100).toInt()
            val stonesEarned = maxOf(0, newMilestone - s.maxMilestoneReached)
            val newMaxMilestone = maxOf(s.maxMilestoneReached, newMilestone)

            val gemText   = if (gemsEarned  > 0) "  ジェム+$gemsEarned" else ""
            val stoneText = if (stonesEarned > 0) "  輝石+$stonesEarned" else ""
            val bossCleared = if (isBoss) " 【ボス撃破！】" else ""
            val boostTag = if (s.isAttackBoosted()) " [強化中]" else ""
            val logEntry = "${minuteCount}分 ── $bossPrefix$stageBefore → $stageAfter  敵${GameState.ENEMIES_PER_MINUTE}体撃破！コイン+$battleCoins$gemText$stoneText$bossCleared$boostTag"

            _state.value = s.copy(
                stage                = stageAfter,
                coins                = s.coins + battleCoins,
                gems                 = s.gems + gemsEarned,
                prestigeStones       = s.prestigeStones + stonesEarned,
                maxMilestoneReached  = newMaxMilestone,
                totalEnemiesDefeated = s.totalEnemiesDefeated + GameState.ENEMIES_PER_MINUTE,
                totalCoinsEarned     = s.totalCoinsEarned + battleCoins
            )
            _battleLog.value = listOf(logEntry) + _battleLog.value.take(19)
        } else {
            val shieldActive = s.penaltyShieldActive
            val (stageAfter, failText) = if (shieldActive) {
                val label = if (isBoss) "ボスに敗北 【シールド発動！落下防止】" else "攻撃力不足 【シールド発動！落下防止】"
                stageBefore to label
            } else {
                // 序盤保護: ペナルティは現在ステージの50%か200の小さい方
                val penalty = minOf(GameState.STAGE_PENALTY, stageBefore / 2)
                val after = maxOf(1L, stageBefore - penalty)
                val label = if (isBoss) "ボスに敗北 (−${stageBefore - after}ステージ)"
                            else "攻撃力不足(−${stageBefore - after}ステージ)"
                after to label
            }
            val logEntry = "${minuteCount}分 ── $bossPrefix$stageBefore → $stageAfter  $failText"
            _state.value = s.copy(
                stage               = stageAfter,
                penaltyShieldActive = false  // シールドは使用済み or 元々なし
            )
            _battleLog.value = listOf(logEntry) + _battleLog.value.take(19)
        }

        saveGame()
    }

    fun upgradeCoinAttack() {
        val s = _state.value
        if (s.coinAttackLevel >= GameState.COIN_ATTACK_MAX_LEVEL) return
        val cost = s.coinAttackNextCost()
        if (s.coins < cost) return
        _state.value = s.copy(coins = s.coins - cost, coinAttackLevel = s.coinAttackLevel + 1)
    }

    fun buyPrestigeUpgrade(id: Int) {
        val s = _state.value
        val level = s.prestigeUpgradeLevel(id)
        if (level >= s.prestigeUpgradeMax(id)) return
        val cost = s.prestigeUpgradeCost(id)
        if (s.prestigeStones < cost) return
        val newUpgrades = s.prestigeUpgrades.toMutableMap()
        newUpgrades[id] = level + 1
        _state.value = s.copy(
            prestigeStones   = s.prestigeStones - cost,
            prestigeUpgrades = newUpgrades
        )
    }

    fun addGems(amount: Int) {
        _state.value = _state.value.copy(gems = _state.value.gems + amount)
    }

    fun watchGemAd() {
        val s = _state.value
        val now = System.currentTimeMillis()
        if (now - s.lastGemAdTime < GameState.COIN_AD_COOLDOWN_MS) return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val watchedToday = if (s.gemAdLastDate == today) s.gemAdWatchedToday else 0
        if (watchedToday >= GameState.GEM_AD_DAILY_LIMIT) return
        _state.value = s.copy(
            gems              = s.gems + GameState.GEM_AD_REWARD,
            gemAdWatchedToday = watchedToday + 1,
            gemAdLastDate     = today,
            lastGemAdTime     = now
        )
    }

    fun watchCoinAd() {
        val s = _state.value
        val now = System.currentTimeMillis()
        if (now - s.lastCoinAdTime < GameState.COIN_AD_COOLDOWN_MS) return
        val reward = s.coinAdReward()
        _state.value = s.copy(
            coins            = s.coins + reward,
            lastCoinAdTime   = now,
            totalCoinsEarned = s.totalCoinsEarned + reward
        )
    }

    private val _pendingMinutes = MutableStateFlow(0)
    val pendingMinutes: StateFlow<Int> = _pendingMinutes

    private var synthJob: Job? = null

    fun addSynthesisMinute() {
        val s = _state.value
        if (s.gems < 10) return
        _state.value = s.copy(gems = s.gems - 10)
        _pendingMinutes.value++
        synthJob?.cancel()
        synthJob = viewModelScope.launch {
            delay(1_000L)
            applyGemSynthesis()
        }
    }

    private fun applyGemSynthesis() {
        val minutes = _pendingMinutes.value
        if (minutes == 0) return
        _pendingMinutes.value = 0
        val s = _state.value
        val weapons = s.weapons.toMutableMap()

        repeat(minutes * 60) {
            val star = generateWeaponStar(s)
            weapons[star] = (weapons[star] ?: 0) + 1
        }

        val overflow = weapons.values.sum() - s.weaponSlots
        if (overflow > 0) {
            var toRemove = overflow
            for (star in weapons.keys.sorted()) {
                if (toRemove <= 0) break
                val count = weapons[star] ?: 0
                val remove = minOf(count, toRemove)
                if (count - remove == 0) weapons.remove(star) else weapons[star] = count - remove
                toRemove -= remove
            }
        }

        _state.value = s.copy(weapons = weapons)
    }

    private fun mergeWeaponsMap(weapons: MutableMap<Int, Int>) {
        var changed = true
        while (changed) {
            changed = false
            for (level in weapons.keys.sorted()) {
                val count = weapons[level] ?: 0
                if (count >= 2) {
                    weapons[level] = count - 2
                    if (weapons[level] == 0) weapons.remove(level)
                    weapons[level + 1] = (weapons[level + 1] ?: 0) + 1
                    changed = true
                    break
                }
            }
        }
    }

    fun mergeWeapons() {
        val weapons = _state.value.weapons.toMutableMap()
        mergeWeaponsMap(weapons)
        _state.value = _state.value.copy(weapons = weapons)
    }

    fun expandWeaponSlots(): Boolean {
        val s = _state.value
        if (s.weaponSlots >= 50) return false
        val cost = s.weaponSlotExpandCost()
        if (s.coins < cost) return false
        _state.value = s.copy(coins = s.coins - cost, weaponSlots = s.weaponSlots + 1)
        return true
    }

    fun claimAchievement(id: String) {
        val s = _state.value
        val def = GameState.ACHIEVEMENTS.find { it.id == id } ?: return
        val claimable = s.achievementClaimable(def)
        if (claimable <= 0) return
        val newClaimed = s.achievementsClaimed.toMutableMap()
        newClaimed[id] = (newClaimed[id] ?: 0) + claimable
        _state.value = s.copy(gems = s.gems + def.rewardGems * claimable, achievementsClaimed = newClaimed)
    }

    fun resetGame() {
        _state.value = GameState()
        _battleLog.value = emptyList()
        saveGame()
    }

    fun saveGame() {
        viewModelScope.launch { repository.save(_state.value) }
    }

    override fun onCleared() {
        viewModelScope.launch(NonCancellable) { repository.save(_state.value) }
        super.onCleared()
    }

    companion object {
        private const val IDLE_TIMEOUT_MS = 5 * 60 * 1000L
    }

    class Factory(private val app: com.example.idlegame.IdleGameApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(app.repository, app.apiRepository) as T
    }
}
