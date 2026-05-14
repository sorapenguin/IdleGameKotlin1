package com.example.idlegame.ui.weapon

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.idlegame.IdleGameApp
import com.example.idlegame.databinding.FragmentWeaponBinding
import com.example.idlegame.ui.main.MainViewModel
import com.example.idlegame.util.formatNumber
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WeaponFragment : Fragment() {

    private var _binding: FragmentWeaponBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModel.Factory(requireActivity().application as IdleGameApp)
    }

    private val adapter = WeaponAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeaponBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvWeapons.layoutManager = GridLayoutManager(requireContext(), 5)
        binding.rvWeapons.adapter = adapter

        // 初回表示
        adapter.update(viewModel.state.value)

        binding.btnRefreshWeapons.setOnClickListener {
            adapter.update(viewModel.state.value)
        }

        binding.btnMerge.setOnClickListener {
            viewModel.mergeWeapons()
        }

        binding.btnExpandSlots.setOnClickListener {
            if (!viewModel.expandWeaponSlots()) {
                val s = viewModel.state.value
                val msg = if (s.weaponSlots >= 50) "スロットが最大です (50)" else "コインが足りません"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAutoDeleteMinus.setOnClickListener {
            viewModel.setAutoDeleteLevel(viewModel.state.value.autoDeleteLevel - 1)
        }
        binding.btnAutoDeletePlus.setOnClickListener {
            viewModel.setAutoDeleteLevel(viewModel.state.value.autoDeleteLevel + 1)
        }

        // ジェム合成ボタン：押すたびに+1分、長押しで連続消費
        var holdJob: Job? = null
        binding.btnGemSynth.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    viewModel.addSynthesisMinute()
                    holdJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(400L)
                        while (isActive) {
                            viewModel.addSynthesisMinute()
                            delay(150L)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holdJob?.cancel()
                    holdJob = null
                    true
                }
                else -> false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var prevWeapons: Map<Int, Int> = emptyMap()
                viewModel.state.collect { s ->
                    if (s.weapons != prevWeapons) {
                        adapter.update(s)
                        prevWeapons = s.weapons
                    }
                    binding.tvTotalAttack.text = "総攻撃力: ${formatNumber(s.totalAttack())}"
                    binding.tvSlotInfo.text = "スロット: ${s.totalWeapons()} / ${s.weaponSlots}"
                    binding.btnExpandSlots.text = if (s.weaponSlots >= 50) {
                        "スロット最大 (50/50)"
                    } else {
                        "スロット拡張 (+1)\n費用: ${formatNumber(s.weaponSlotExpandCost())} コイン"
                    }
                    val maxDel = s.maxAutoDeleteLevel()
                    binding.tvAutoDeleteLevel.text = if (s.autoDeleteLevel == 0) "無効" else "★${s.autoDeleteLevel}以下を削除"
                    binding.btnAutoDeleteMinus.isEnabled = s.autoDeleteLevel > 0
                    binding.btnAutoDeletePlus.isEnabled = s.autoDeleteLevel < maxDel
                    binding.tvGemsWeapon.text = "ジェム: ${s.gems}  (10個 = 1分)"
                    binding.btnGemSynth.isEnabled = s.gems >= 10
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingMinutes.collect { minutes ->
                    binding.tvPendingMinutes.text = if (minutes == 0) "" else "×${minutes}分 (${minutes * 10}ジェム消費済)"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
