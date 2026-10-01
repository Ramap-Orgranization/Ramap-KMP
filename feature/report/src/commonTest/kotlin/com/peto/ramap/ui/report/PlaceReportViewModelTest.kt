package com.peto.ramap.ui.report

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.report.PlaceLinkProvider
import com.peto.ramap.domain.model.report.ResolvedPlaceLink
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.PlaceLinkResolver
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopReportRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.report.contract.PlaceReportIntent
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect
import com.peto.ramap.ui.report.contract.PlaceReportUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.place_report_failure_message
import ramap.shared.generated.resources.place_report_invalid_url_message
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
    fun `URL만 입력해도 해석된 카카오 장소 ID가 기존 매장과 같으면 제보하지 않는다`() =
        coroutinesTest {
            val reportRepository = FakeShopReportRepository()
            val shop =
                ramenShopFixture(kakaoPlaceUrl = "https://place.map.kakao.com/1521564391", name = "멘코지")
            val viewModel =
                placeReportViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            searchResult =
                                RamenShops(mapOf(shop.id to shop)),
                        ),
                    reportRepository = reportRepository,
                    placeLinkResolver =
                        PlaceLinkResolver {
                            ResolvedPlaceLink(PlaceLinkProvider.KAKAO, placeId = "1521564391")
                        },
                )

            viewModel.dispatch(PlaceReportIntent.OnPlaceUrlChanged("https://kko.to/2lSFVyW1b2"))
            viewModel.dispatch(PlaceReportIntent.OnPlaceReportSubmit)
            runCurrent()

            assertEquals(0, reportRepository.placeReports.size)
        }

    @Test
    fun `URL만 입력해도 해석된 네이버 장소명이 기존 매장과 같으면 제보하지 않는다`() =
        coroutinesTest {
            val reportRepository = FakeShopReportRepository()
            val shop =
                ramenShopFixture(name = "라멘야 시마")
            val viewModel =
                placeReportViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            searchResult =
                                RamenShops(mapOf(shop.id to shop)),
                        ),
                    reportRepository = reportRepository,
                    placeLinkResolver =
                        PlaceLinkResolver {
                            ResolvedPlaceLink(PlaceLinkProvider.NAVER, name = "라멘야 시마")
                        },
                )

            viewModel.dispatch(PlaceReportIntent.OnPlaceUrlChanged("https://naver.me/5MvGvjXa"))
            viewModel.dispatch(PlaceReportIntent.OnPlaceReportSubmit)
            runCurrent()

            assertEquals(0, reportRepository.placeReports.size)
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
    fun `기존 장소 검색에 실패하면 오류 토스트를 보여주고 제보하지 않는다`() =
        coroutinesTest {
            val reportRepository = FakeShopReportRepository()
            val viewModel =
                placeReportViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            error = RamapError.Network(IllegalStateException("offline")),
                        ),
                    reportRepository = reportRepository,
                )
            val sharedPlaceText =
                """[카카오맵] 신멘
                |경기 안양시 동안구 호성로 20
                |https://kko.to/hgONCY9DKH
                """.trimMargin()

            viewModel.sideEffect.test {
                viewModel.dispatch(PlaceReportIntent.OnPlaceUrlChanged(sharedPlaceText))
                viewModel.dispatch(PlaceReportIntent.OnPlaceReportSubmit)

                assertEquals(
                    PlaceReportSideEffect.ShowToast(
                        ToastData(
                            Res.string.place_report_failure_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }

            assertEquals(0, reportRepository.placeReports.size)
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
}

private fun placeReportViewModel(
    ramenShopRepository: FakeRamenShopRepository = FakeRamenShopRepository(),
    placeLinkResolver: PlaceLinkResolver = PlaceLinkResolver { null },
    reportRepository: FakeShopReportRepository = FakeShopReportRepository(),
) = PlaceReportViewModel(
    ramenShopRepository = ramenShopRepository,
    placeLinkResolver = placeLinkResolver,
    reportRepository = reportRepository,
)
