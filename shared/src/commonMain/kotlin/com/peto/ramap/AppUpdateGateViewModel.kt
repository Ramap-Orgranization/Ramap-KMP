package com.peto.ramap

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.update.AppUpdatePolicy
import com.peto.ramap.domain.repository.AppUpdateRepository
import com.peto.ramap.platform.AppVersionProvider
import com.peto.ramap.ui.base.BaseViewModel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

internal class AppUpdateGateViewModel(
    private val repository: AppUpdateRepository,
    private val versionProvider: AppVersionProvider,
) : BaseViewModel<AppUpdateUiState, Nothing, Nothing>(AppUpdateUiState()) {
    init {
        checkPolicy()
    }

    override suspend fun handleIntent(intent: Nothing) = Unit

    private fun checkPolicy() {
        launchTask(
            taskKey = POLICY_TASK_KEY,
            loadKey = AppUpdateLoadKey.Policy,
        ) {
            val loadedPolicy = fetchPolicy()
            reduce { copy(policy = loadedPolicy) }
        }
    }

    private suspend fun fetchPolicy(): AppUpdatePolicy? =
        withTimeoutOrNull(POLICY_TIMEOUT_MS.milliseconds) {
            when (val result = repository.fetchAppUpdatePolicy(versionProvider.platform)) {
                is RamapResult.Success -> result.data
                is RamapResult.Error -> null
            }
        }

    private companion object {
        const val POLICY_TASK_KEY = "app-update-policy"
        const val POLICY_TIMEOUT_MS = 5_000L
    }
}
