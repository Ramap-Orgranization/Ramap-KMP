package com.peto.ramap.ui.report

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.repository.ReverseGeocoder
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopReportRepository
import com.peto.ramap.platform.location.CurrentLocationProvider
import com.peto.ramap.platform.location.PlatformLocation
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.report.contract.PlaceReportIntent
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect
import com.peto.ramap.ui.report.contract.PlaceReportUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.place_report_invalid_url_message
import ramap.shared.generated.resources.place_report_location_unavailable_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceReportViewModelTest {
    @Test
    fun `장소 URL 입력이 비어 있지 않으면 제출할 수 있다`() {
        assertFalse(PlaceReportUiState(placeUrl = "").canSubmitPlaceUrl)
        assertFalse(PlaceReportUiState(placeUrl = " \t\n").canSubmitPlaceUrl)
        assertTrue(PlaceReportUiState(placeUrl = "ㅗㅗ").canSubmitPlaceUrl)
    }

    @Test
    fun `지원하지 않는 장소 URL을 제출하면 에러 토스트를 보여준다`() =
        coroutinesTest {
            val viewModel = placeReportViewModel()

            viewModel.sideEffect.test {
                viewModel.dispatch(PlaceReportIntent.OnPlaceUrlChanged("ㅗㅗ"))
                viewModel.dispatch(PlaceReportIntent.OnPlaceReportSubmit)
                assertEquals(
                    PlaceReportSideEffect.ShowToast(
                        ToastData(
                            Res.string.place_report_invalid_url_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
                assertFalse(viewModel.uiState.value.isSubmitting)
            }
        }

    @Test
    fun `장소 URL 제보 저장 중에는 제출 로딩 상태이고 완료 후 해제된다`() =
        coroutinesTest {
            val reportRepository = FakeShopReportRepository(delayMillis = 1_000)
            val viewModel = placeReportViewModel(reportRepository = reportRepository)

            viewModel.dispatch(PlaceReportIntent.OnPlaceUrlChanged("https://kko.to/hgONCY9DKH"))
            viewModel.dispatch(PlaceReportIntent.OnPlaceReportSubmit)
            runCurrent()

            assertTrue(viewModel.uiState.value.isSubmitting)
            assertFalse(viewModel.uiState.value.canSubmitPlaceUrl)

            advanceTimeBy(1_000)
            runCurrent()

            assertFalse(viewModel.uiState.value.isSubmitting)
            assertEquals(1, reportRepository.placeReports.size)
        }

    @Test
    fun `현재 위치 제보 저장 중에는 두 제출 버튼을 비활성화한다`() =
        coroutinesTest {
            val currentLocationStore = CurrentLocationStore()
            val reportRepository = FakeShopReportRepository(delayMillis = 1_000)
            val viewModel =
                placeReportViewModel(
                    currentLocationStore = currentLocationStore,
                    reportRepository = reportRepository,
                )
            currentLocationStore.update(Location(lat = 37.551, lng = 126.921))
            runCurrent()

            viewModel.dispatch(PlaceReportIntent.OnPlaceUrlChanged("https://naver.me/GahpmIBD"))
            viewModel.dispatch(PlaceReportIntent.OnCurrentLocationReportSubmit)
            runCurrent()

            assertTrue(viewModel.uiState.value.isSubmitting)
            assertFalse(viewModel.uiState.value.canSubmitPlaceUrl)
            assertFalse(viewModel.uiState.value.canSubmitCurrentLocation)

            advanceTimeBy(1_000)
            runCurrent()

            assertFalse(viewModel.uiState.value.isSubmitting)
            assertEquals(1, reportRepository.placeReports.size)
        }

    @Test
    fun `제출 중 다시 제출해도 요청을 한 번만 보낸다`() =
        coroutinesTest {
            val reportRepository = FakeShopReportRepository(delayMillis = 1_000)
            val viewModel = placeReportViewModel(reportRepository = reportRepository)
            viewModel.dispatch(PlaceReportIntent.OnPlaceUrlChanged("https://kko.to/hgONCY9DKH"))

            viewModel.dispatch(PlaceReportIntent.OnPlaceReportSubmit)
            viewModel.dispatch(PlaceReportIntent.OnPlaceReportSubmit)
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(1, reportRepository.placeReports.size)
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun `현재 위치가 없으면 현재 위치 제보 시 에러 토스트를 보여준다`() =
        coroutinesTest {
            val viewModel = placeReportViewModel()

            viewModel.sideEffect.test {
                viewModel.dispatch(PlaceReportIntent.OnCurrentLocationReportSubmit)
                assertEquals(
                    PlaceReportSideEffect.ShowToast(
                        ToastData(
                            Res.string.place_report_location_unavailable_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `현재 위치가 변경되면 새 위치의 주소를 조회한다`() =
        coroutinesTest {
            val currentLocationStore = CurrentLocationStore()
            val requestedLocations = mutableListOf<Location>()
            val viewModel =
                placeReportViewModel(
                    currentLocationStore = currentLocationStore,
                    reverseGeocoder =
                        ReverseGeocoder { location ->
                            requestedLocations += location
                            RamapResult.Success("테스트 주소")
                        },
                )
            val location = Location(lat = 37.551, lng = 126.921)

            currentLocationStore.update(location)
            runCurrent()

            assertEquals(listOf(location), requestedLocations)
            assertEquals("테스트 주소", viewModel.uiState.value.currentAddress)
        }

    @Test
    fun `위치 권한이 허용되면 플랫폼 현재 위치를 캐시에 저장하고 주소를 조회한다`() =
        coroutinesTest {
            val currentLocationStore = CurrentLocationStore()
            val location = Location(lat = 37.551, lng = 126.921)
            val viewModel =
                placeReportViewModel(
                    currentLocationStore = currentLocationStore,
                    currentLocationProvider =
                        CurrentLocationProvider {
                            PlatformLocation(location.lat, location.lng)
                        },
                    reverseGeocoder = ReverseGeocoder { RamapResult.Success("테스트 주소") },
                )

            viewModel.dispatch(
                PlaceReportIntent.OnLocationPermissionResult(PermissionStatus.Granted),
            )
            runCurrent()

            assertEquals(location, currentLocationStore.location.value)
            assertEquals(location, viewModel.uiState.value.currentLocation)
            assertEquals("테스트 주소", viewModel.uiState.value.currentAddress)
            assertFalse(viewModel.uiState.value.isLocationLoading)
        }

    @Test
    fun `위치 권한이 거부되면 안내 토스트를 보여주고 위치를 조회하지 않는다`() =
        coroutinesTest {
            var requestCount = 0
            val viewModel =
                placeReportViewModel(
                    currentLocationProvider =
                        CurrentLocationProvider {
                            requestCount += 1
                            null
                        },
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    PlaceReportIntent.OnLocationPermissionResult(PermissionStatus.Blocked),
                )
                assertEquals(
                    PlaceReportSideEffect.ShowToast(
                        ToastData(
                            Res.string.location_permission_enable_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }
            assertEquals(0, requestCount)
        }

    @Test
    fun `플랫폼 위치 조회 실패 시 로딩을 해제하고 오류 토스트를 보여준다`() =
        coroutinesTest {
            val viewModel =
                placeReportViewModel(
                    currentLocationProvider = CurrentLocationProvider { null },
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    PlaceReportIntent.OnLocationPermissionResult(PermissionStatus.Granted),
                )
                assertEquals(
                    PlaceReportSideEffect.ShowToast(
                        ToastData(
                            Res.string.place_report_location_unavailable_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }
            assertFalse(viewModel.uiState.value.isLocationLoading)
        }

    @Test
    fun `주소 조회 중 위치가 변경되면 이전 조회를 취소하고 최신 위치의 주소를 사용한다`() =
        coroutinesTest {
            val currentLocationStore = CurrentLocationStore()
            val firstLocation = Location(lat = 37.551, lng = 126.921)
            val latestLocation = Location(lat = 37.552, lng = 126.922)
            var isFirstRequestCancelled = false
            val viewModel =
                placeReportViewModel(
                    currentLocationStore = currentLocationStore,
                    reverseGeocoder =
                        ReverseGeocoder { location ->
                            if (location == firstLocation) {
                                try {
                                    awaitCancellation()
                                } finally {
                                    isFirstRequestCancelled = true
                                }
                            }
                            RamapResult.Success("최신 주소")
                        },
                )

            currentLocationStore.update(firstLocation)
            runCurrent()
            currentLocationStore.update(latestLocation)
            runCurrent()

            assertEquals(true, isFirstRequestCancelled)
            assertEquals(latestLocation, viewModel.uiState.value.currentLocation)
            assertEquals("최신 주소", viewModel.uiState.value.currentAddress)
            assertEquals(false, viewModel.uiState.value.isAddressRefreshing)
        }
}

private fun placeReportViewModel(
    currentLocationStore: CurrentLocationStore = CurrentLocationStore(),
    reverseGeocoder: ReverseGeocoder = ReverseGeocoder { RamapResult.Success("") },
    reportRepository: FakeShopReportRepository = FakeShopReportRepository(),
    currentLocationProvider: CurrentLocationProvider = CurrentLocationProvider { null },
) = PlaceReportViewModel(
    ramenShopRepository = FakeRamenShopRepository(),
    reportRepository = reportRepository,
    currentLocationStore = currentLocationStore,
    reverseGeocoder = reverseGeocoder,
    currentLocationProvider = currentLocationProvider,
)
