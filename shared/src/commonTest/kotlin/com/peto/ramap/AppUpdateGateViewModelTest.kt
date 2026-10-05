package com.peto.ramap

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.update.AppUpdatePolicy
import com.peto.ramap.domain.repository.AppUpdateRepository
import com.peto.ramap.platform.AppVersionProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateGateViewModelTest {
    @Test
    fun `정책 조회 실패 시 로딩을 끝내고 앱 진입을 허용한다`() =
        coroutinesTest {
            val viewModel =
                viewModel {
                    RamapResult.Error(RamapError.Unknown(IllegalStateException("failed")))
                }

            assertTrue(
                viewModel.uiState.value.loadState
                    .isLoading(AppUpdateLoadKey.Policy),
            )
            runCurrent()

            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
            assertNull(viewModel.uiState.value.policy)
        }

    @Test
    fun `정책 조회가 5초를 넘기면 로딩을 끝내고 앱 진입을 허용한다`() =
        coroutinesTest {
            val viewModel = viewModel { awaitCancellation() }
            runCurrent()
            advanceTimeBy(4_999)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.loadState
                    .isLoading(AppUpdateLoadKey.Policy),
            )

            advanceTimeBy(1)
            runCurrent()

            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
            assertNull(viewModel.uiState.value.policy)
        }

    private fun viewModel(
        request: suspend () -> RamapResult<AppUpdatePolicy?>,
    ): AppUpdateGateViewModel =
        AppUpdateGateViewModel(
            repository =
                object : AppUpdateRepository {
                    override suspend fun fetchAppUpdatePolicy(platform: String): RamapResult<AppUpdatePolicy?> = request()
                },
            versionProvider =
                object : AppVersionProvider {
                    override val versionName = "1.0.0"
                    override val buildNumber = 1L
                    override val platform = "android"
                },
        )
}
