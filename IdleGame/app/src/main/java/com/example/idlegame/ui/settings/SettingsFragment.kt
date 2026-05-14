package com.example.idlegame.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.idlegame.IdleGameApp
import com.example.idlegame.R
import com.example.idlegame.databinding.FragmentSettingsBinding
import com.example.idlegame.network.TokenManager
import com.example.idlegame.ui.auth.LoginActivity
import com.example.idlegame.ui.main.MainViewModel
import com.example.idlegame.util.formatNumber
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: MainViewModel by activityViewModels {
        MainViewModel.Factory(requireActivity().application as IdleGameApp)
    }
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAchievements.setOnClickListener {
            findNavController().navigate(R.id.nav_achievement)
        }

        binding.btnStats.setOnClickListener {
            showStatsDialog()
        }

        binding.btnLoginLink.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                putExtra("from_settings", true)
            })
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("ログアウト")
                .setMessage("ログアウトしますか？\nゲームデータはこの端末に残ります。")
                .setPositiveButton("ログアウト") { _, _ ->
                    TokenManager.clearAuth(requireContext())
                    updateAccountUi()
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        observeSettings()
    }

    override fun onResume() {
        super.onResume()
        updateAccountUi()
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    settingsViewModel.soundEffects.collect { enabled ->
                        // リスナー一時解除してから値をセットして無限ループを防ぐ
                        binding.switchSoundEffects.setOnCheckedChangeListener(null)
                        binding.switchSoundEffects.isChecked = enabled
                        binding.switchSoundEffects.setOnCheckedChangeListener { _, checked ->
                            settingsViewModel.setSoundEffects(checked)
                        }
                    }
                }
                launch {
                    settingsViewModel.vibration.collect { enabled ->
                        binding.switchVibration.setOnCheckedChangeListener(null)
                        binding.switchVibration.isChecked = enabled
                        binding.switchVibration.setOnCheckedChangeListener { _, checked ->
                            settingsViewModel.setVibration(checked)
                        }
                    }
                }
            }
        }
    }

    private fun updateAccountUi() {
        if (TokenManager.isLoggedIn(requireContext())) {
            val username = TokenManager.getUsername(requireContext())
            binding.tvAccountStatus.text = "ログイン中: $username"
            binding.btnLoginLink.visibility = View.GONE
            binding.btnLogout.visibility = View.VISIBLE
        } else {
            binding.tvAccountStatus.text = "ゲストモード（データはこの端末のみ）"
            binding.btnLoginLink.visibility = View.VISIBLE
            binding.btnLogout.visibility = View.GONE
        }
    }

    private fun showStatsDialog() {
        val s = gameViewModel.state.value
        val message = """
            総撃破数:　　　　${formatNumber(s.totalEnemiesDefeated)} 体
            総獲得コイン:　　${formatNumber(s.totalCoinsEarned)}
            現在ステージ:　　${s.stage}
            最高到達ステージ: ${s.maxMilestoneReached * 100}+
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("統計")
            .setMessage(message)
            .setPositiveButton("閉じる", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
