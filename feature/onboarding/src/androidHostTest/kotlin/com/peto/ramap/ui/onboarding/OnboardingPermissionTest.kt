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
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeOnboardingStorage
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopSuggestionsRepository
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.onboarding.shopselection.ShopSelectionViewModel
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.viewModel
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
    fun `다음 버튼은 위치와 알림을 차례로 요청하고 둘 다 거부해도 사용법 안내를 표시한다`() =
        runComposeUiTest {
            val storage = FakeOnboardingStorage()
            val tracker = FakeAnalyticsTracker()
            val toastManager = ToastManager()
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
                onboardingViewModel = OnboardingViewModel(storage, tracker)
            }
            val testModule =
                module {
                    viewModel { parameters ->
                        val shops = FakeRamenShopRepository()
                        ShopSelectionViewModel(
                            searchShops = SearchRamenShopsUseCase(shops),
                            shopRepository = shops,
                            suggestionsRepository = FakeShopSuggestionsRepository(RamenShops(emptyMap())),
                            personalizationStore = FakePersonalizationRepository(),
                            loginRepository = FakeLoginRepository(),
                            storage = storage,
                            analytics = parameters.get(),
                        )
                    }
                }
            setContent {
                KoinApplication(configuration = koinConfiguration { modules(testModule) }) {
                    CompositionLocalProvider(
                        LocalLifecycleOwner provides lifecycleOwner,
                        LocalActivityResultRegistryOwner provides registryOwner,
                    ) {
                        RamapTheme {
                            OnboardingRoute(
                                onCompleted = {},
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
        }
}
