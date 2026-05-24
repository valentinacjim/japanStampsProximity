package com.mapclover.stampquest.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapclover.stampquest.data.repository.KmlRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: KmlRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    fun loadStamps() {
        viewModelScope.launch {
            val stamps = repository.loadStamps()

            _uiState.update {
                it.copy(stamps = stamps)
            }
        }
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update {
            it.copy(selectedCategory = category)
        }
    }

    sealed class MapEvent {
        data object VibrateUnlock : MapEvent()
    }

    private val _events = MutableSharedFlow<MapEvent>()
    val events = _events.asSharedFlow()

    suspend fun onStampUnlocked() {
        _events.emit(MapEvent.VibrateUnlock)
    }
}