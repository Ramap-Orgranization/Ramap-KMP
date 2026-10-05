package com.peto.ramap

import android.content.res.Configuration
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.update.AppUpdatePolicy
import com.peto.ramap.domain.repository.AppUpdateRepository
import com.peto.ramap.navigation.NavigationRouter
import com.peto.ramap.navigation.rememberNavigationState
import com.peto.ramap.platform.AppVersionProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(ExperimentalCoroutinesApi::class)
class AppConfigurationTest {
    @BeforeTest
    fun prepareDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `구성 변경 후 업데이트 정책을 재조회하지 않고 내비게이션 ViewModel을 유지한다`() = verifyPolicyRetention()

    @Test
    fun `정책 조회 중 구성 변경이 일어나도 진행 중인 요청을 유지한다`() =
        verifyPolicyRetention(
            policy = AppUpdatePolicy(minimumBuildNumber = 1, storeUrl = "https://example.com"),
            recreateWhileLoading = true,
        )

    @Test
    fun `구성 변경 후에도 필수 업데이트 정책이 앱 진입을 차단한다`() =
        verifyPolicyRetention(
            policy = AppUpdatePolicy(minimumBuildNumber = 10, storeUrl = "https://example.com"),
        )

    private fun verifyPolicyRetention(
        policy: AppUpdatePolicy? = null,
        recreateWhileLoading: Boolean = false,
    ) {
        var updateRequestCount = 0
        var navigationViewModelCreations = 0
        var navigationViewModel: ViewModel? = null
        var gateViewModel: AppUpdateGateViewModel? = null
        val response = CompletableDeferred<AppUpdatePolicy?>()
        val repository =
            object : AppUpdateRepository {
                override suspend fun fetchAppUpdatePolicy(platform: String): RamapResult<AppUpdatePolicy?> {
                    updateRequestCount++
                    return RamapResult.Success(response.await())
                }
            }
        val versionProvider =
            object : AppVersionProvider {
                override val versionName = "1.0.0"
                override val buildNumber = 1L
                override val platform = "android"
            }
        AppConfigurationTestActivity.content = content@{
            val retainedGate = viewModel { AppUpdateGateViewModel(repository, versionProvider) }
            gateViewModel = retainedGate
            // 무한 로딩 애니메이션 없이 진행 중인 요청의 구성 변경 수명을 검증한다.
            if (recreateWhileLoading) return@content
            AppUpdateGate(
                appUpdateRepository = repository,
                appVersionProvider = versionProvider,
                viewModel = retainedGate,
            ) {
                val navigationState = rememberNavigationState(onMapTabExited = {})
                NavigationRouter(
                    navigationState = navigationState,
                    mapScreen = {
                        navigationViewModel =
                            viewModel<ViewModel> {
                                navigationViewModelCreations++
                                object : ViewModel() {}
                            }
                    },
                    rankingScreen = {},
                    eventListScreen = {},
                    operatingNoticeScreen = {},
                    myScreen = {},
                    settingsScreen = {},
                    accountSettingsScreen = {},
                    profileEditScreen = {},
                    myReviewsScreen = {},
                    otherReviewsScreen = {},
                    reviewWriteScreen = {},
                    informationScreen = {},
                    placeReportScreen = {},
                    hiddenScreen = {},
                    notificationSettingsScreen = {},
                    subscribedShopsScreen = {},
                    bookmarkedShopsScreen = {},
                    importationScreen = {},
                    importationGuideScreen = {},
                    eventScreen = {},
                )
            }
        }
        if (!recreateWhileLoading) response.complete(policy)
        val controller = Robolectric.buildActivity(AppConfigurationTestActivity::class.java).setup()
        try {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertEquals(1, updateRequestCount)
            val originalGateViewModel = assertNotNull(gateViewModel)
            val originalNavigationViewModel = navigationViewModel
            if (recreateWhileLoading) {
                assertTrue(originalGateViewModel.uiState.value.loadState.isAnyLoading)
            } else {
                assertEquals(policy, originalGateViewModel.uiState.value.policy)
            }

            val configuration =
                Configuration(controller.get().resources.configuration).apply {
                    orientation = Configuration.ORIENTATION_LANDSCAPE
                    screenWidthDp = 800
                    screenHeightDp = 400
                }
            controller.configurationChange(configuration)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

            assertSame(originalGateViewModel, gateViewModel)
            assertEquals(1, updateRequestCount)
            assertSame(originalNavigationViewModel, navigationViewModel)
            if (recreateWhileLoading) {
                assertTrue(originalGateViewModel.uiState.value.loadState.isAnyLoading)
            }
            response.complete(policy)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertEquals(policy, originalGateViewModel.uiState.value.policy)
            assertFalse(originalGateViewModel.uiState.value.loadState.isAnyLoading)
            if (!recreateWhileLoading) {
                assertEquals(if (policy == null) 1 else 0, navigationViewModelCreations)
                if (policy == null) assertNotNull(navigationViewModel) else assertNull(navigationViewModel)
            }
        } finally {
            controller.pause().stop().destroy()
            AppConfigurationTestActivity.content = {}
        }
    }
}
