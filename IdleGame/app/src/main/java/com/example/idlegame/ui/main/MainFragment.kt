package com.example.idlegame.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.idlegame.IdleGameApp
import com.example.idlegame.R
import com.example.idlegame.data.GameRepository
import com.example.idlegame.data.GameState
import com.example.idlegame.databinding.FragmentMainBinding
import com.example.idlegame.util.formatNumber
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModel.Factory(requireActivity().application as IdleGameApp)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGoAchievement.setOnClickListener {
            findNavController().navigate(R.id.nav_achievement)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    binding.tvStage.text = if (s.isBossStage()) {
                        "★ BOSS Stage ${s.stage}  (HP×${s.bossMultiplier()})"
                    } else {
                        "Stage ${s.stage}"
                    }
                    binding.tvCoins.text = "コイン: ${formatNumber(s.coins)}"
                    binding.tvGems.text = "ジェム: ${s.gems}"

                    updateNextGoal(s)
                    updateAchievementBanner(s)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.battleLog.collect { log ->
                    binding.tvBattleLog.text = if (log.isEmpty()) {
                        "（冒険中... 1分ごとに記録が更新されます）"
                    } else {
                        log.joinToString("\n")
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingOffline.collect { result ->
                    if (result != null) showOfflineDialog(result)
                }
            }
        }

        // ブーストバナー（毎秒更新）
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    val s = viewModel.state.value
                    val remaining = s.boostRemainingMs()
                    if (remaining > 0) {
                        binding.tvBoostBanner.visibility = View.VISIBLE
                        val min = remaining / 60_000
                        val sec = (remaining % 60_000) / 1_000
                        binding.tvBoostBanner.text = "⚔ 攻撃力×2 発動中！ 残り ${min}分${"%02d".format(sec)}秒"
                    } else {
                        binding.tvBoostBanner.visibility = View.GONE
                    }
                    kotlinx.coroutines.delay(1_000L)
                }
            }
        }
    }

    private fun updateNextGoal(s: GameState) {
        val atk = s.totalAttack()
        val hp = s.enemyHp()
        val gap = hp - atk
        binding.tvNextGoal.text = if (gap > 0) {
            "次のステージに勝つためにあと攻撃力 ${formatNumber(gap)} 必要\n" +
            "（現在 ${formatNumber(atk)}  /  必要 ${formatNumber(hp)}）\n" +
            "→ 武器を増やすか、コイン強化・広告ブーストが有効です"
        } else {
            "✓ 現在のステージを突破中！（余裕: +${formatNumber(-gap)}）\n" +
            "Stage ${s.stage} → 次のボスまで頑張ろう"
        }
    }

    private fun updateAchievementBanner(s: GameState) {
        val claimable = GameState.ACHIEVEMENTS.sumOf { def -> s.achievementClaimable(def) }
        if (claimable > 0) {
            binding.bannerAchievement.visibility = View.VISIBLE
            binding.tvAchievementBadge.text = "実績 ${claimable}件 受け取れます！ (+ジェム)"
        } else {
            binding.bannerAchievement.visibility = View.GONE
        }
    }

    private fun showOfflineDialog(result: GameRepository.OfflineResult) {
        val hours = result.minutes / 60
        val mins  = result.minutes % 60
        val timeText = if (hours > 0) "${hours}時間${mins}分" else "${mins}分"
        val message = "冒険を続けていました！\n\n" +
                "オフライン時間: $timeText\n" +
                "獲得コイン: +${formatNumber(result.coins)}\n\n" +
                "広告を見るとコインが2倍になります！"

        AlertDialog.Builder(requireContext())
            .setTitle("おかえりなさい！")
            .setMessage(message)
            .setPositiveButton("広告を見て×2") { _, _ ->
                val app = requireActivity().application as IdleGameApp
                app.adManager.showRewarded(
                    requireActivity(),
                    onRewarded = { viewModel.collectOfflineEarnings(doubled = true) },
                    onFailed = {
                        Toast.makeText(requireContext(), "広告を読み込み中です。通常報酬を受け取ります", Toast.LENGTH_SHORT).show()
                        viewModel.collectOfflineEarnings(doubled = false)
                    }
                )
            }
            .setNegativeButton("このまま受け取る") { _, _ ->
                viewModel.collectOfflineEarnings(doubled = false)
            }
            .setCancelable(false)
            .show()
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveGame()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
