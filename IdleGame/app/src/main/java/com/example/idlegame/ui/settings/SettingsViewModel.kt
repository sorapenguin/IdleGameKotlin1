package com.example.idlegame.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.idlegame.IdleGameApp
import com.example.idlegame.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as IdleGameApp).settingsRepository

    val soundEffects: StateFlow<Boolean> = repo.soundEffects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.DEFAULT_SOUND_EFFECTS)

    val vibration: StateFlow<Boolean> = repo.vibration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.DEFAULT_VIBRATION)

    fun setSoundEffects(enabled: Boolean) {
        viewModelScope.launch { repo.setSoundEffects(enabled) }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { repo.setVibration(enabled) }
    }
}
