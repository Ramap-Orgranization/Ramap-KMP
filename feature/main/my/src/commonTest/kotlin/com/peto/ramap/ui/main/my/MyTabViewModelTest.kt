package com.peto.ramap.ui.main.my

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopReportRepository
import com.peto.ramap.network.ReverseGeocoder
import com.peto.ramap.ui.common.CurrentLocationStore
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnCurrentAddressRefresh
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnCurrentLocationReportSubmit
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnHiddenShopsClick
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect.NavigateToHiddenShops
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect.ShowMyToast
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.place_report_location_unavailable_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyTabViewModelTest {
    @Test
    fun `비로그인 사용자가 숨김 매장을 누르면 로그인 가이드를 보여준다`() =
        coroutinesTest {
            val viewModel = myTabViewModel()

            viewModel.dispatch(OnHiddenShopsClick)
            runCurrent()

            assertEquals(true, viewModel.uiState.value.showLoginGuideDialog)
        }

    @Test
    fun `로그인 사용자가 숨김 매장을 누르면 숨김 매장 화면으로 이동한다`() =
        coroutinesTest {
            val viewModel = myTabViewModel(loginRepository = loggedInRepository())
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnHiddenShopsClick)
                assertEquals(NavigateToHiddenShops, awaitItem())
            }
        }

    @Test
    fun `현재 위치가 없으면 현재 위치 제보 시 에러 토스트를 보여준다`() =
        coroutinesTest {
            val viewModel = myTabViewModel()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnCurrentLocationReportSubmit)
                assertEquals(showMyToastSideEffect(Res.string.place_report_location_unavailable_message), awaitItem())
            }
        }

    @Test
    fun `공유 위치 저장소가 갱신되면 내 화면 상태에 현재 위치를 반영한다`() =
        coroutinesTest {
            val currentLocationStore = CurrentLocationStore()
            val viewModel = myTabViewModel(currentLocationStore = currentLocationStore)
            val location = Location(lat = 37.551, lng = 126.921)

            currentLocationStore.update(location)
            runCurrent()

            assertEquals(location, viewModel.uiState.value.currentLocation)
        }

    @Test
    fun `현재 위치가 변경되면 새 위치의 주소를 자동 조회한다`() =
        coroutinesTest {
            val currentLocationStore = CurrentLocationStore()
            val requestedLocations = mutableListOf<Location>()
            val viewModel =
                myTabViewModel(
                    currentLocationStore = currentLocationStore,
                    reverseGeocoder = reverseGeocoder(requestedLocations),
                )
            val firstLocation = Location(lat = 37.551, lng = 126.921)
            val secondLocation = Location(lat = 37.552, lng = 126.922)

            currentLocationStore.update(firstLocation)
            runCurrent()
            currentLocationStore.update(secondLocation)
            runCurrent()

            assertEquals(listOf(firstLocation, secondLocation), requestedLocations)
            assertEquals(secondLocation, viewModel.uiState.value.currentLocation)
            assertEquals("테스트 주소", viewModel.uiState.value.currentAddress)
        }

    @Test
    fun `주소 새로고침은 최신 현재 위치로 다시 조회한다`() =
        coroutinesTest {
            val currentLocationStore = CurrentLocationStore()
            val requestedLocations = mutableListOf<Location>()
            val viewModel =
                myTabViewModel(
                    currentLocationStore = currentLocationStore,
                    reverseGeocoder = reverseGeocoder(requestedLocations),
                )
            val firstLocation = Location(lat = 37.551, lng = 126.921)
            val latestLocation = Location(lat = 37.552, lng = 126.922)
            currentLocationStore.update(firstLocation)
            runCurrent()
            currentLocationStore.update(latestLocation)
            runCurrent()

            viewModel.dispatch(OnCurrentAddressRefresh)
            runCurrent()

            assertEquals(listOf(firstLocation, latestLocation, latestLocation), requestedLocations)
            assertEquals("테스트 주소", viewModel.uiState.value.currentAddress)
        }

    @Test
    fun `주소 조회 중 새로고침을 다시 눌러도 요청을 중복 실행하지 않는다`() =
        coroutinesTest {
            val currentLocationStore = CurrentLocationStore()
            val response = CompletableDeferred<RamapResult<String?>>()
            var requestCount = 0
            val viewModel =
                myTabViewModel(
                    currentLocationStore = currentLocationStore,
                    reverseGeocoder =
                        ReverseGeocoder {
                            requestCount++
                            response.await()
                        },
                )
            currentLocationStore.update(Location(lat = 37.551, lng = 126.921))
            runCurrent()

            assertTrue(viewModel.uiState.value.isAddressRefreshing)
            viewModel.dispatch(OnCurrentAddressRefresh)
            viewModel.dispatch(OnCurrentAddressRefresh)
            runCurrent()
            assertEquals(1, requestCount)

            response.complete(RamapResult.Success("테스트 주소"))
            runCurrent()
            assertFalse(viewModel.uiState.value.isAddressRefreshing)
        }
}

private fun showMyToastSideEffect(message: StringResource): ShowMyToast =
    ShowMyToast(
        ToastData(
            message = message,
            type = ToastType.ERROR,
        ),
    )

private fun myTabViewModel(
    loginRepository: FakeLoginRepository = FakeLoginRepository(),
    ramenShopRepository: FakeRamenShopRepository = FakeRamenShopRepository(),
    shopReportRepository: FakeShopReportRepository = FakeShopReportRepository(),
    currentLocationStore: CurrentLocationStore = CurrentLocationStore(),
    reverseGeocoder: ReverseGeocoder? = null,
): MyTabViewModel =
    MyTabViewModel(
        loginRepository = loginRepository,
        ramenShopRepository = ramenShopRepository,
        reportRepository = shopReportRepository,
        currentLocationStore = currentLocationStore,
        reverseGeocoder = reverseGeocoder,
    )

private fun reverseGeocoder(requestedLocations: MutableList<Location>): ReverseGeocoder =
    ReverseGeocoder { location ->
        requestedLocations += location
        RamapResult.Success("테스트 주소")
    }

private fun loggedInRepository(): FakeLoginRepository =
    FakeLoginRepository(
        initialSessionState = LoginSessionState.AUTHENTICATED,
        userEmail = "test@ramap.com",
    )
