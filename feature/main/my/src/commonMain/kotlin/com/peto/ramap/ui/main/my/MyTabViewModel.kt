package com.peto.ramap.ui.main.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MyTabViewModel(
    private val repository: ProfileRepository,
    private val personalizationStore: ShopPersonalizationStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MyTabUiState())
    val uiState: StateFlow<MyTabUiState> = mutableUiState.asStateFlow()
    private var fetchJob: Job? = null
    private var requestGeneration = 0L

    init {
        viewModelScope.launch {
            repository.sessionUserIds.distinctUntilChanged().collect { userId ->
                requestGeneration++
                fetchJob?.cancel()
                mutableUiState.value = MyTabUiState(userId = userId, loading = userId != null)
                if (userId != null) refresh()
            }
        }
        viewModelScope.launch {
            personalizationStore.state.collect { state ->
                val personalization = (state as? PersonalizationBootstrapState.Success)?.value
                mutableUiState.value =
                    mutableUiState.value.copy(
                        bookmarkedCount = personalization?.bookmarkedShopIds?.size,
                        notificationCount = personalization?.notificationShopIds?.size,
                        hiddenCount = personalization?.hiddenShopIds?.size,
                    )
            }
        }
    }

    fun refresh() {
        val userId = mutableUiState.value.userId ?: return
        val generation = ++requestGeneration
        fetchJob?.cancel()
        fetchJob =
            viewModelScope.launch {
                mutableUiState.value = mutableUiState.value.copy(loading = true, failed = false)
                when (val result = repository.fetchMyProfile()) {
                    is RamapResult.Success -> {
                        if (generation == requestGeneration && mutableUiState.value.userId == userId) {
                            mutableUiState.value =
                                if (result.data.userId == userId) {
                                    mutableUiState.value.copy(profile = result.data, loading = false)
                                } else {
                                    mutableUiState.value.copy(loading = false, failed = true)
                                }
                        }
                    }
                    is RamapResult.Error -> {
                        if (generation == requestGeneration && mutableUiState.value.userId == userId) {
                            mutableUiState.value = mutableUiState.value.copy(loading = false, failed = true)
                        }
                    }
                }
            }
    }
}
