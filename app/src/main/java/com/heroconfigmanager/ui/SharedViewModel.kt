package com.heroconfigmanager.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heroconfigmanager.data.model.Hero
import com.heroconfigmanager.data.model.HeroConfig
import com.heroconfigmanager.data.repository.ConfigRepository
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String = "") : UiState()
    data class Error(val message: String) : UiState()
}

class SharedViewModel : ViewModel() {

    private val _config = MutableLiveData(HeroConfig())
    val config: LiveData<HeroConfig> get() = _config

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> get() = _uiState

    private val _lastSync = MutableLiveData("")
    val lastSync: LiveData<String> get() = _lastSync

    private val _isFetchingConfig = MutableLiveData(false)
    val isFetchingConfig: LiveData<Boolean> get() = _isFetchingConfig

    private val _hasFinishedInitialLoad = MutableLiveData(false)
    val hasFinishedInitialLoad: LiveData<Boolean> get() = _hasFinishedInitialLoad

    fun fetchConfig() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _isFetchingConfig.value = true
            when (val result = ConfigRepository.fetchConfig()) {
                is ConfigRepository.Result.Success -> {
                    _config.value = result.data
                    _lastSync.value = java.text.SimpleDateFormat(
                        "HH:mm:ss",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())
                    _uiState.value = UiState.Success("Config loaded from GitHub")
                }
                is ConfigRepository.Result.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
            _hasFinishedInitialLoad.value = true
            _isFetchingConfig.value = false
        }
    }

    fun pushConfig(updatedConfig: HeroConfig) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _config.value = updatedConfig
            when (val result = ConfigRepository.pushConfig(updatedConfig)) {
                is ConfigRepository.Result.Success -> {
                    _lastSync.value = java.text.SimpleDateFormat(
                        "HH:mm:ss",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())
                    _uiState.value = UiState.Success("Pushed to GitHub")
                }
                is ConfigRepository.Result.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun addOrUpdateHero(role: String, hero: Hero, existingHero: Hero? = null) {
        val current = _config.value ?: return
        val list = current.roles.heroesFor(role).toMutableList()

        if (existingHero != null) {
            val idx = list.indexOfFirst { it.hero == existingHero.hero }
            if (idx >= 0) list[idx] = hero else list.add(hero)
        } else {
            val idx = list.indexOfFirst { it.hero == hero.hero }
            if (idx >= 0) list[idx] = hero else list.add(hero)
        }
        _config.value = current.copy(roles = current.roles.withRole(role, list))
    }

    fun deleteHero(role: String, hero: Hero) {
        val current = _config.value ?: return
        val list = current.roles.heroesFor(role).toMutableList()
        list.remove(hero)
        _config.value = current.copy(roles = current.roles.withRole(role, list))
    }

    fun updateConfigMetadata(version: String, note: String) {
        val updated = (_config.value ?: HeroConfig()).copy(
            configVersion = version,
            configNote = note
        )
        pushConfig(updated)
    }

    fun clearState() {
        _uiState.value = UiState.Idle
    }
}
