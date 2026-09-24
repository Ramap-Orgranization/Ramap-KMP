package com.peto.ramap.ui.main.my

import androidx.lifecycle.viewModelScope
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabLoadKey
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MyTabViewModel(
    private val repository: ProfileRepository,
    private val personalizationStore: ShopPersonalizationStore,
) : BaseViewModel<MyTabUiState, MyTabIntent, MyTabSideEffect>(MyTabUiState()) {
    private var requestGeneration = 0L

    init {
        viewModelScope.launch {
            repository.sessionUserIds.distinctUntilChanged().collect { userId ->
                requestGeneration++
                cancelTask(FETCH)
                reduce { MyTabUiState(userId = userId) }
                if (userId != null) refresh()
            }
        }
        viewModelScope.launch {
            personalizationStore.state.collect { state ->
                val personalization = (state as? PersonalizationBootstrapState.Success)?.value
                reduce {
                    copy(
                        bookmarkedCount = personalization?.bookmarkedShopIds?.size,
                        notificationCount = personalization?.notificationShopIds?.size,
                        hiddenCount = personalization?.hiddenShopIds?.size,
                    )
                }
            }
        }
    }

    override suspend fun handleIntent(intent: MyTabIntent) {
        when (intent) {
            MyTabIntent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        val userId = currentState.userId ?: return
        val generation = ++requestGeneration
        launchResultTask(
            taskKey = FETCH,
            loadKey = MyTabLoadKey.Fetch,
            onStart = { copy(failed = false) },
            request = repository::fetchMyProfile,
            onSuccess = { profile ->
                if (generation == requestGeneration && currentState.userId == userId) {
                    reduce { if (profile.userId == userId) copy(profile = profile) else copy(failed = true) }
                }
            },
            onError = {
                if (generation == requestGeneration && currentState.userId == userId) reduce { copy(failed = true) }
            },
        )
    }

    companion object {
        private const val FETCH = "my-tab-profile-fetch"
    }
}
