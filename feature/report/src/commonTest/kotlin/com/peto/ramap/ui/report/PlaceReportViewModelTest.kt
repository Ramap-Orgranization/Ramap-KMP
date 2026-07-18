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
import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.report.contract.PlaceReportIntent
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.place_report_location_unavailable_message
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceReportViewModelTest {
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
}

private fun placeReportViewModel(
    currentLocationStore: CurrentLocationStore = CurrentLocationStore(),
    reverseGeocoder: ReverseGeocoder? = null,
) = PlaceReportViewModel(
    ramenShopRepository = FakeRamenShopRepository(),
    reportRepository = FakeShopReportRepository(),
    currentLocationStore = currentLocationStore,
    reverseGeocoder = reverseGeocoder,
)
