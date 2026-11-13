package com.peto.ramap.ui.onboarding

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopSuggestionsRepository
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeOnboardingStorage
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopSuggestionsRepository
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.onboarding.di.onboardingModule
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingPermissionTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `위치와 알림을 차례로 거부해도 매장 선택 뒤 로그인 없이 앱을 시작할 수 있다`() =
        runComposeUiTest {
            val storage = FakeOnboardingStorage()
            val tracker = FakeAnalyticsTracker()
            val toastManager = ToastManager()
            val login = FakeLoginRepository()
            val personalization = FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("one", "two", "three")))
            var didComplete = false
            lateinit var onboardingViewModel: OnboardingViewModel
            val lifecycleOwner =
                object : LifecycleOwner {
                    override val lifecycle = LifecycleRegistry.createUnsafe(this)
                }
            val registry =
                object : ActivityResultRegistry() {
                    val requests = mutableListOf<List<String>>()
                    private var lastRequestCode = 0

                    override fun <I, O> onLaunch(
                        requestCode: Int,
                        contract: ActivityResultContract<I, O>,
                        input: I,
                        options: ActivityOptionsCompat?,
                    ) {
                        lastRequestCode = requestCode
                        requests +=
                            contract
                                .createIntent(RuntimeEnvironment.getApplication(), input)
                                .getStringArrayExtra(ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSIONS)
                                .orEmpty()
                                .toList()
                    }

                    fun denyLastRequest() {
                        val permissions = requests.last().toTypedArray()
                        val denial =
                            Intent()
                                .putExtra(ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSIONS, permissions)
                                .putExtra(
                                    ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS,
                                    IntArray(permissions.size) { PackageManager.PERMISSION_DENIED },
                                )
                        dispatchResult(lastRequestCode, Activity.RESULT_OK, denial)
                    }
                }
            val registryOwner =
                object : ActivityResultRegistryOwner {
                    override val activityResultRegistry = registry
                }
            runOnIdle {
                lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
                onboardingViewModel = OnboardingViewModel(storage, tracker, login, personalization)
            }
            val testModule =
                module {
                    includes(onboardingModule)
                    single<OnboardingStorage> { storage }
                    single<LoginRepository> { login }
                    single<PersonalizationStore> { personalization }
                    single<RamenShopRepository> { FakeRamenShopRepository() }
                    single<ShopSuggestionsRepository> { FakeShopSuggestionsRepository(RamenShops(emptyMap())) }
                    single { SearchRamenShopsUseCase(get()) }
                    single { SignInUseCase(get(), LoginAnalytics(tracker, FakeCrashReporter())) }
                }
            setContent {
                KoinApplication(configuration = koinConfiguration { modules(testModule) }) {
                    CompositionLocalProvider(
                        LocalLifecycleOwner provides lifecycleOwner,
                        LocalActivityResultRegistryOwner provides registryOwner,
                    ) {
                        RamapTheme {
                            OnboardingRoute(
                                onCompleted = { didComplete = true },
                                toastManager = toastManager,
                                viewModel = onboardingViewModel,
                            )
                        }
                    }
                }
            }

            val location = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            val notification = listOf(Manifest.permission.POST_NOTIFICATIONS)
            runOnIdle { assertTrue(registry.requests.isEmpty()) }
            onNodeWithText("다음").performClick()
            runOnIdle {
                assertEquals(listOf(location), registry.requests)
                lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
                registry.denyLastRequest()
            }
            runOnIdle {
                assertEquals(listOf(location), registry.requests)
                lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
            }
            runOnIdle {
                assertEquals(listOf(location, notification), registry.requests)
                assertFalse(storage.confirmed)
                registry.denyLastRequest()
            }

            onNodeWithText("앱 접근 권한이 필요해요.").assertDoesNotExist()
            onNodeWithText("매장 더보기에서 알림을 설정할 수 있어요.").assertIsDisplayed()
            runOnIdle {
                assertTrue(storage.confirmed)
                assertFalse(onboardingViewModel.uiState.value.loadState.isAnyLoading)
            }

            onNodeWithText("건너뛰기").performClick()
            onNodeWithText("다음").performClick()
            onNodeWithText("저장한 라멘집 소식을\n알림으로 받아보세요").assertIsDisplayed()
            onNodeWithText("로그인하면 이벤트나 임시휴무 알림을 설정하고\n다녀온 매장에 리뷰를 남길 수 있어요.").assertIsDisplayed()
            onNodeWithText("카카오로 시작하기").assertIsDisplayed()
            runOnIdle {
                assertFalse(didComplete)
                assertFalse(storage.completed)
            }
            onNodeWithText("로그인 없이 시작하기 ->").assertIsDisplayed().performClick()
            runOnIdle {
                assertTrue(didComplete)
                assertTrue(storage.completed)
                assertEquals(setOf("one", "two", "three"), personalization.bookmarkedShopIds.value)
                assertFalse(login.hasSession())
            }
        }
}
