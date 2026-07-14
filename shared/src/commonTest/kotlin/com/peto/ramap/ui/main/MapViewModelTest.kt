package com.peto.ramap.ui.main

import app.cash.turbine.test
import com.peto.ramap.core.config.DefaultMapConfig
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.Personalization
import com.peto.ramap.domain.model.RamenShopFilter
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery
import com.peto.ramap.domain.model.ShopInformationField
import com.peto.ramap.domain.model.ShopInformationReport
import com.peto.ramap.domain.model.UnregisteredPlaceReport
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopReportRepository
import com.peto.ramap.fake.FakeShopWaitingSystemRepository
import com.peto.ramap.fixture.BOUNDS_FIXTURE
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.fixture.waitingSystemFixture
import com.peto.ramap.ui.common.CurrentLocationStore
import com.peto.ramap.ui.main.map.MapViewModel
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkedShopsToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBoundsChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCategoryFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCurrentLocationReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnFilterCleared
import com.peto.ramap.ui.main.map.contract.MapIntent.OnHiddenToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialLocationFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMyLocationChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnQueryChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSearchResultsDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopIdSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnUnregisteredPlaceReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowLoginGuide
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowToast
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.model.MapPersonalization
import com.peto.ramap.ui.main.map.model.SearchResultGuide
import com.peto.ramap.ui.main.map.model.SearchUiState
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.filter_empty_visible_result_message
import ramap.shared.generated.resources.hidden_shop_search_result_message
import ramap.shared.generated.resources.hide_shop_success_message
import ramap.shared.generated.resources.place_report_existing_shop_message
import ramap.shared.generated.resources.place_report_invalid_url_message
import ramap.shared.generated.resources.place_report_location_unavailable_message
import ramap.shared.generated.resources.place_report_success_message
import ramap.shared.generated.resources.search_result_empty_message
import ramap.shared.generated.resources.shop_information_report_failure_message
import ramap.shared.generated.resources.shop_information_report_success_message
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    @Test
    fun `연속 지도 영역 변경은 지연 후 마지막 영역만 조회한다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository()
            val viewModel = mapViewModel(ramenShopRepository)
            val lastBounds =
                BOUNDS_FIXTURE.copy(
                    minLat = BOUNDS_FIXTURE.minLat + 0.03,
                    maxLat = BOUNDS_FIXTURE.maxLat + 0.03,
                )

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(200)
            viewModel.dispatch(OnBoundsChanged(lastBounds))
            advanceTimeBy(349)
            runCurrent()

            assertEquals(emptyList(), ramenShopRepository.requestedBoundsHistory)

            advanceTimeBy(1)
            runCurrent()

            assertEquals(listOf(lastBounds), ramenShopRepository.requestedBoundsHistory)
        }

    @Test
    fun `마지막 성공 조회 영역과 의미 있게 다르지 않으면 조회하지 않는다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository()
            val viewModel = mapViewModel(ramenShopRepository)
            val nearbyBounds =
                BOUNDS_FIXTURE.copy(
                    minLat = BOUNDS_FIXTURE.minLat + 0.01,
                    maxLat = BOUNDS_FIXTURE.maxLat + 0.01,
                )

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(OnBoundsChanged(nearbyBounds))
            advanceTimeBy(350)
            runCurrent()

            assertEquals(listOf(BOUNDS_FIXTURE), ramenShopRepository.requestedBoundsHistory)
        }

    @Test
    fun `마지막 성공 조회 영역과 의미 있게 다르면 새로 조회한다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository()
            val viewModel = mapViewModel(ramenShopRepository)
            val changedBounds =
                BOUNDS_FIXTURE.copy(
                    minLat = BOUNDS_FIXTURE.minLat + 0.03,
                    maxLat = BOUNDS_FIXTURE.maxLat + 0.03,
                )

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(OnBoundsChanged(changedBounds))
            advanceTimeBy(350)
            runCurrent()

            assertEquals(
                listOf(BOUNDS_FIXTURE, changedBounds),
                ramenShopRepository.requestedBoundsHistory,
            )
        }

    @Test
    fun `조회 결과가 현재 가게 목록과 같으면 UI 상태를 다시 방출하지 않는다`() =
        coroutinesTest {
            val shops = RamenShops(listOf(ramenShopFixture()).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(result = shops)
            val viewModel = mapViewModel(ramenShopRepository)
            val changedBounds =
                BOUNDS_FIXTURE.copy(
                    minLat = BOUNDS_FIXTURE.minLat + 0.03,
                    maxLat = BOUNDS_FIXTURE.maxLat + 0.03,
                )

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertEquals(RamenShops(emptyMap()), initialState.shops)
                assertEquals(DefaultMapConfig.bounds, initialState.bounds)

                viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
                assertEquals(BOUNDS_FIXTURE, awaitItem().bounds)
                advanceTimeBy(350)
                runCurrent()
                assertEquals(shops, awaitItem().shops)

                viewModel.dispatch(OnBoundsChanged(changedBounds))
                assertEquals(changedBounds, awaitItem().bounds)
                advanceTimeBy(350)
                runCurrent()
                expectNoEvents()
            }
        }

    @Test
    fun `가게를 선택하면 상세를 즉시 열고 웨이팅 시스템을 조회한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val waitingSystem = waitingSystemFixture(shopId = shop.id)
            val waitingSystemRepository =
                FakeShopWaitingSystemRepository(result = waitingSystem)
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        ),
                    shopWaitingSystemRepository = waitingSystemRepository,
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(listOf(shop.id), waitingSystemRepository.requestedShopIds)
            assertEquals(waitingSystem, viewModel.uiState.value.shopWaiting[shop.id])
        }

    @Test
    fun `웨이팅 시스템 조회 결과가 없어도 선택한 가게 상세는 유지한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val waitingSystemRepository = FakeShopWaitingSystemRepository(result = null)
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        ),
                    shopWaitingSystemRepository = waitingSystemRepository,
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            val containsWaitingSystem =
                viewModel
                    .uiState
                    .value
                    .shopWaiting
                    .containsKey(shop.id)

            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(listOf(shop.id), waitingSystemRepository.requestedShopIds)
            assertEquals(true, containsWaitingSystem)
            assertEquals(null, viewModel.uiState.value.shopWaiting[shop.id])
        }

    @Test
    fun `매장 상세 조회에 실패하면 오류 토스트를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            error = RamapError.Unknown(IllegalStateException("failed")),
                        ),
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(OnShopSelected(shop))
                runCurrent()

                assertEquals(null, viewModel.uiState.value.shopDetail)
                assertEquals(false, viewModel.uiState.value.isShopDetailLoading)
                assertEquals(
                    ShowToast(
                        ToastData(
                            message = Res.string.data_load_failure_message,
                            type = ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `이미 조회한 가게를 다시 선택하면 상세를 중복 조회하지 않고 즉시 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val ramenShopRepository =
                FakeRamenShopRepository(fetchByIdsResult = RamenShops(mapOf(shop.id to shop)))
            val waitingSystemRepository =
                FakeShopWaitingSystemRepository(result = waitingSystemFixture(shop.id))
            val viewModel =
                mapViewModel(
                    ramenShopRepository = ramenShopRepository,
                    shopWaitingSystemRepository = waitingSystemRepository,
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            assertEquals(listOf(setOf(shop.id)), ramenShopRepository.requestedShopIdsHistory)
            assertEquals(listOf(shop.id), waitingSystemRepository.requestedShopIds)
            assertEquals(listOf(shop.id), ramenShopRepository.requestedActiveEventShopIds)
            assertEquals(
                shop,
                viewModel.uiState.value
                    .shopDetail
                    ?.shop,
            )
            assertEquals(false, viewModel.uiState.value.isShopDetailLoading)
        }

    @Test
    fun `서로 다른 가게의 상세는 각각 한 번씩 캐싱한다`() =
        coroutinesTest {
            val firstShop = ramenShopFixture(id = "shop-1")
            val secondShop = ramenShopFixture(id = "shop-2")
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(firstShop.id to firstShop, secondShop.id to secondShop)),
                )
            val waitingSystemRepository = FakeShopWaitingSystemRepository()
            val viewModel = mapViewModel(ramenShopRepository, waitingSystemRepository)

            viewModel.dispatch(OnShopSelected(firstShop))
            runCurrent()
            viewModel.dispatch(OnShopSelected(secondShop))
            runCurrent()
            viewModel.dispatch(OnShopSelected(firstShop))
            runCurrent()

            assertEquals(
                listOf(setOf(firstShop.id), setOf(secondShop.id)),
                ramenShopRepository.requestedShopIdsHistory,
            )
            assertEquals(listOf(firstShop.id, secondShop.id), waitingSystemRepository.requestedShopIds)
            assertEquals(
                firstShop,
                viewModel.uiState.value
                    .shopDetail
                    ?.shop,
            )
        }

    @Test
    fun `실패한 가게 상세는 캐싱하지 않아 다시 선택하면 재조회한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val ramenShopRepository =
                FakeRamenShopRepository(
                    error = RamapError.Unknown(IllegalStateException("failed")),
                )
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            assertEquals(4, ramenShopRepository.requestedShopIdsHistory.size)
            assertEquals(null, viewModel.uiState.value.shopDetail)
        }

    @Test
    fun `이벤트 조회 실패로 이벤트가 없는 상세도 캐싱한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                    activeEventError = RamapError.Unknown(IllegalStateException("failed")),
                )
            val waitingSystemRepository = FakeShopWaitingSystemRepository()
            val viewModel = mapViewModel(ramenShopRepository, waitingSystemRepository)

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            assertEquals(1, ramenShopRepository.requestedShopIdsHistory.size)
            assertEquals(1, ramenShopRepository.requestedActiveEventShopIds.size)
            assertEquals(
                null,
                viewModel.uiState.value
                    .shopDetail
                    ?.event,
            )
        }

    @Test
    fun `아이디로 이미 캐싱한 가게를 선택하면 추가 조회하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val ramenShopRepository =
                FakeRamenShopRepository(fetchByIdsResult = RamenShops(mapOf(shop.id to shop)))
            val waitingSystemRepository = FakeShopWaitingSystemRepository()
            val viewModel = mapViewModel(ramenShopRepository, waitingSystemRepository)

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()
            viewModel.dispatch(OnShopIdSelected(shop.id))
            runCurrent()

            assertEquals(listOf(setOf(shop.id)), ramenShopRepository.requestedShopIdsHistory)
            assertEquals(listOf(shop.id), waitingSystemRepository.requestedShopIds)
            assertEquals(
                shop,
                viewModel.uiState.value
                    .shopDetail
                    ?.shop,
            )
        }

    @Test
    fun `선택된 가게가 있어도 지도 영역 변경만으로는 웨이팅 시스템을 조회하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val ramenShopRepository = FakeRamenShopRepository()
            val waitingSystemRepository =
                FakeShopWaitingSystemRepository(result = waitingSystemFixture(shop.id))
            val viewModel =
                mapViewModel(
                    ramenShopRepository = ramenShopRepository,
                    shopWaitingSystemRepository = waitingSystemRepository,
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            waitingSystemRepository.requestedShopIds.clear()

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()

            assertEquals(listOf(BOUNDS_FIXTURE), ramenShopRepository.requestedBoundsHistory)
            assertEquals(emptyList(), waitingSystemRepository.requestedShopIds)
        }

    @Test
    fun `비로그인 상태에서도 매장 정보 제보를 제출한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "shop-1", name = "라멘집")
            val reportRepository = FakeShopReportRepository()
            val viewModel =
                mapViewModel(
                    ramenShopRepository = FakeRamenShopRepository(fetchByIdsResult = RamenShops(mapOf(shop.id to shop))),
                    shopReportRepository = reportRepository,
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    OnShopReportSubmitted(
                        wrongFields = setOf(ShopInformationField.ADDRESS, ShopInformationField.OTHER),
                        description = " 주소가 달라요 ",
                    ),
                )
                runCurrent()

                assertEquals(
                    listOf(
                        ShopInformationReport(
                            shopId = "shop-1",
                            shopName = "라멘집",
                            wrongFields = setOf(ShopInformationField.ADDRESS, ShopInformationField.OTHER),
                            description = "주소가 달라요",
                        ),
                    ),
                    reportRepository.reports,
                )
                assertEquals(showToastSideEffect(Res.string.shop_information_report_success_message), awaitItem())
            }
        }

    @Test
    fun `매장 정보 제보 실패시 실패 토스트를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val viewModel =
                mapViewModel(
                    ramenShopRepository = FakeRamenShopRepository(fetchByIdsResult = RamenShops(mapOf(shop.id to shop))),
                    shopReportRepository =
                        FakeShopReportRepository(
                            error = IllegalStateException("failed"),
                        ),
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    OnShopReportSubmitted(
                        wrongFields = setOf(ShopInformationField.PHONE),
                        description = "",
                    ),
                )
                runCurrent()

                assertEquals(
                    ShowToast(
                        ToastData(
                            message = Res.string.shop_information_report_failure_message,
                            type = ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `매장 정보 제보 중에는 중복 제출하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val reportRepository = FakeShopReportRepository(delayMillis = 1_000)
            val viewModel = mapViewModel(shopReportRepository = reportRepository)

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            val intent =
                OnShopReportSubmitted(
                    wrongFields = setOf(ShopInformationField.ADDRESS),
                    description = "주소가 달라요",
                )
            viewModel.dispatch(intent)
            viewModel.dispatch(intent)
            runCurrent()

            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(1, reportRepository.reports.size)
        }

    @Test
    fun `좋아요 토글 인텐트는 북마크 보기와 전체 보기를 전환한다`() =
        coroutinesTest {
            val viewModel = mapViewModel(loginRepository = loggedInRepository())
            runCurrent()

            viewModel.dispatch(OnBookmarkedShopsToggled)
            runCurrent()

            assertEquals(MapPersonalization.BOOKMARKED, viewModel.uiState.value.personalizationView)

            viewModel.dispatch(OnBookmarkedShopsToggled)
            runCurrent()

            assertEquals(MapPersonalization.ALL, viewModel.uiState.value.personalizationView)
        }

    @Test
    fun `비로그인 상태에서 좋아요 토글을 누르면 로그인 안내를 요청한다`() =
        coroutinesTest {
            val viewModel = mapViewModel()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnBookmarkedShopsToggled)
                runCurrent()

                assertEquals(ShowLoginGuide, awaitItem())
            }
        }

    @Test
    fun `카카오맵 또는 네이버 지도 주소로 미등록 장소를 제보한다`() =
        coroutinesTest {
            val reportRepository = FakeShopReportRepository()
            val viewModel = mapViewModel(shopReportRepository = reportRepository)

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    OnUnregisteredPlaceReportSubmitted(
                        placeUrl = " https://map.kakao.com/link/map/123 ",
                    ),
                )
                runCurrent()

                assertEquals(
                    listOf(UnregisteredPlaceReport(placeUrl = "https://map.kakao.com/link/map/123")),
                    reportRepository.placeReports,
                )
                assertEquals(showToastSideEffect(Res.string.place_report_success_message), awaitItem())
            }
        }

    @Test
    fun `지원하지 않는 지도 주소는 미등록 장소 제보를 제출하지 않는다`() =
        coroutinesTest {
            val reportRepository = FakeShopReportRepository()
            val viewModel = mapViewModel(shopReportRepository = reportRepository)

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    OnUnregisteredPlaceReportSubmitted(
                        placeUrl = "https://example.com/place/123",
                    ),
                )
                runCurrent()

                assertEquals(emptyList(), reportRepository.placeReports)
                assertEquals(
                    ShowToast(
                        ToastData(
                            message = Res.string.place_report_invalid_url_message,
                            type = ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `이미 등록된 매장 공유 내용이면 제보하지 않고 토스트만 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(name = "신멘", address = "경기 안양시 동안구 호성로 20")
            val reportRepository = FakeShopReportRepository()
            val ramenShopRepository =
                FakeRamenShopRepository(searchResult = RamenShops(mapOf(shop.id to shop)))
            val viewModel = mapViewModel(ramenShopRepository, shopReportRepository = reportRepository)
            val content =
                """[카카오맵] 신멘
                |경기 안양시 동안구 호성로 20
                |https://kko.to/example
                """.trimMargin()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnUnregisteredPlaceReportSubmitted(content))
                runCurrent()

                assertEquals(emptyList(), reportRepository.placeReports)
                assertEquals(null, viewModel.uiState.value.selectedShop)
                assertEquals(emptyList(), viewModel.uiState.value.focusShops)
                assertEquals(showToastSideEffect(Res.string.place_report_existing_shop_message), awaitItem())
            }
        }

    @Test
    fun `현재 위치로 미등록 장소를 제보한다`() =
        coroutinesTest {
            val reportRepository = FakeShopReportRepository()
            val viewModel = mapViewModel(shopReportRepository = reportRepository)
            val location = Location(lat = 37.275, lng = 127.009)

            viewModel.dispatch(OnMyLocationChanged(location))

            viewModel.sideEffect.test {
                viewModel.dispatch(OnCurrentLocationReportSubmitted)
                runCurrent()

                assertEquals(
                    listOf(UnregisteredPlaceReport(location = location)),
                    reportRepository.placeReports,
                )
                assertEquals(showToastSideEffect(Res.string.place_report_success_message), awaitItem())
            }
        }

    @Test
    fun `첫 현재 위치 수신시 초기 위치 포커스를 한 번 요청한다`() =
        coroutinesTest {
            val viewModel = mapViewModel()
            val location = Location(lat = 37.275, lng = 127.009)

            viewModel.dispatch(OnMyLocationChanged(location))
            runCurrent()

            assertEquals(location, viewModel.uiState.value.currentLocation)
            assertEquals(location, viewModel.uiState.value.initialFocusLocation)
            assertEquals(1L, viewModel.uiState.value.initialFocusRequestKey)
        }

    @Test
    fun `초기 위치 포커스를 소비하면 같은 위치 갱신으로 다시 요청하지 않는다`() =
        coroutinesTest {
            val viewModel = mapViewModel()
            val firstLocation = Location(lat = 37.275, lng = 127.009)
            val secondLocation = Location(lat = 37.276, lng = 127.01)

            viewModel.dispatch(OnMyLocationChanged(firstLocation))
            runCurrent()
            viewModel.dispatch(OnInitialLocationFocusConsumed)
            runCurrent()
            viewModel.dispatch(OnMyLocationChanged(secondLocation))
            runCurrent()

            assertEquals(secondLocation, viewModel.uiState.value.currentLocation)
            assertEquals(null, viewModel.uiState.value.initialFocusLocation)
            assertEquals(1L, viewModel.uiState.value.initialFocusRequestKey)
        }

    @Test
    fun `현재 위치가 없으면 위치 기반 미등록 장소 제보를 제출하지 않는다`() =
        coroutinesTest {
            val reportRepository = FakeShopReportRepository()
            val viewModel = mapViewModel(shopReportRepository = reportRepository)

            viewModel.sideEffect.test {
                viewModel.dispatch(OnCurrentLocationReportSubmitted)
                runCurrent()

                assertEquals(emptyList(), reportRepository.placeReports)
                assertEquals(
                    ShowToast(
                        ToastData(
                            message = Res.string.place_report_location_unavailable_message,
                            type = ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `검색어가 변경되면 지연 후 정규화한 검색어로 가게를 검색한다`() =
        coroutinesTest {
            val shops = RamenShops(listOf(ramenShopFixture()).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(searchResult = shops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnQueryChanged("  RAMEN   SHOP  "))
            advanceTimeBy(299)
            runCurrent()

            assertEquals(emptyList(), ramenShopRepository.requestedSearchQueries)

            advanceTimeBy(1)
            runCurrent()

            assertEquals(
                listOf(SearchQuery("ramen shop")),
                ramenShopRepository.requestedSearchQueries,
            )
            assertEquals(listOf(50), ramenShopRepository.requestedSearchLimits)
            assertEquals(shops, viewModel.uiState.value.search.results)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
            assertEquals(shops, viewModel.uiState.value.markerShops)
            assertEquals(shops.values.toList(), viewModel.uiState.value.focusShops)
        }

    @Test
    fun `검색 중에는 지도 영역 매장과 검색 결과를 함께 마커로 보여준다`() =
        coroutinesTest {
            val mapShops =
                RamenShops(
                    listOf(
                        ramenShopFixture().copy(
                            id = "map-shop",
                            name = "지도 매장",
                        ),
                    ).associateBy { it.id },
                )
            val searchShops =
                RamenShops(
                    listOf(
                        ramenShopFixture().copy(
                            id = "search-shop",
                            name = "검색 매장",
                        ),
                    ).associateBy { it.id },
                )
            val ramenShopRepository =
                FakeRamenShopRepository(
                    result = mapShops,
                    searchResult = searchShops,
                )
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(mapShops, viewModel.uiState.value.shops)
            assertEquals(searchShops, viewModel.uiState.value.search.results)
            assertEquals(RamenShops(mapShops + searchShops), viewModel.uiState.value.markerShops)
        }

    @Test
    fun `검색 결과가 하나이면 매장 상세 바텀시트를 바로 열고 웨이팅 시스템을 조회한다`() =
        coroutinesTest {
            val shop =
                ramenShopFixture().copy(
                    id = "search-shop",
                    name = "검색 매장",
                )
            val searchShops = RamenShops(listOf(shop).associateBy { it.id })
            val waitingSystem = waitingSystemFixture(shopId = shop.id)
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val waitingSystemRepository =
                FakeShopWaitingSystemRepository(result = waitingSystem)
            val viewModel =
                mapViewModel(
                    ramenShopRepository = ramenShopRepository,
                    shopWaitingSystemRepository = waitingSystemRepository,
                )

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(true, viewModel.uiState.value.showBottomSheet)
            assertEquals(false, viewModel.uiState.value.showSearchResults)
            assertEquals(searchShops, viewModel.uiState.value.search.results)
            assertEquals(searchShops, viewModel.uiState.value.markerShops)
            assertEquals(listOf(shop), viewModel.uiState.value.focusShops)
            assertEquals(listOf(shop.id), waitingSystemRepository.requestedShopIds)
            assertEquals(waitingSystem, viewModel.uiState.value.shopWaiting[shop.id])
        }

    @Test
    fun `숨김 검색 결과가 하나이면 상세를 열지 않고 숨김 안내를 보여준다`() =
        coroutinesTest {
            val hiddenShop =
                ramenShopFixture(
                    id = "hidden-shop",
                    name = "숨김 매장",
                    isVisible = false,
                )
            val searchShops = RamenShops(listOf(hiddenShop).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val waitingSystemRepository = FakeShopWaitingSystemRepository()
            val viewModel =
                mapViewModel(
                    ramenShopRepository = ramenShopRepository,
                    shopWaitingSystemRepository = waitingSystemRepository,
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(OnQueryChanged("숨김"))
                advanceTimeBy(300)
                runCurrent()

                assertEquals(null, viewModel.uiState.value.selectedShop)
                assertEquals(false, viewModel.uiState.value.showBottomSheet)
                assertEquals(false, viewModel.uiState.value.showSearchResults)
                assertEquals(SearchResultGuide.HIDDEN_ONLY, viewModel.uiState.value.searchResultGuide)
                assertEquals(searchShops, viewModel.uiState.value.search.results)
                assertEquals(searchShops, viewModel.uiState.value.markerShops)
                assertEquals(listOf(hiddenShop), viewModel.uiState.value.focusShops)
                assertEquals(emptyList(), waitingSystemRepository.requestedShopIds)
                assertEquals(showToastSideEffect(Res.string.hidden_shop_search_result_message), awaitItem())
            }
        }

    @Test
    fun `여러 검색 결과에 숨김 매장이 포함되어도 안내를 보여주지 않고 결과 목록을 보여준다`() =
        coroutinesTest {
            val visibleShop =
                ramenShopFixture(
                    id = "visible-shop",
                    name = "노출 매장",
                    isVisible = true,
                )
            val hiddenShop =
                ramenShopFixture(
                    id = "hidden-shop",
                    name = "숨김 매장",
                    isVisible = false,
                )
            val searchShops = RamenShops(listOf(visibleShop, hiddenShop).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.sideEffect.test {
                viewModel.dispatch(OnQueryChanged("라멘"))
                advanceTimeBy(300)
                runCurrent()

                assertEquals(null, viewModel.uiState.value.selectedShop)
                assertEquals(true, viewModel.uiState.value.showSearchResults)
                assertEquals(true, viewModel.uiState.value.showBottomSheet)
                assertEquals(listOf(visibleShop, hiddenShop), viewModel.uiState.value.searchResultShops)
                assertEquals(searchShops, viewModel.uiState.value.markerShops)
                expectNoEvents()
            }
        }

    @Test
    fun `검색 결과가 없으면 빈 결과 토스트를 보여준다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository(searchResult = RamenShops(emptyMap()))
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.sideEffect.test {
                viewModel.dispatch(OnQueryChanged("없는 라멘집"))
                advanceTimeBy(300)
                runCurrent()

                assertEquals(SearchResultGuide.SEARCH_EMPTY, viewModel.uiState.value.searchResultGuide)
                assertEquals(showToastSideEffect(Res.string.search_result_empty_message), awaitItem())
            }
        }

    @Test
    fun `좋아요 보기 상태에서도 선택한 숨김 매장은 마커로 유지한다`() {
        val hiddenShop =
            ramenShopFixture(
                id = "hidden-shop",
                name = "숨김 매장",
                isVisible = false,
            )
        val uiState =
            MapUiState(
                selectedShop = hiddenShop,
                hiddenShopIds = setOf(hiddenShop.id),
                personalizationView = MapPersonalization.BOOKMARKED,
            )

        assertEquals(
            RamenShops(mapOf(hiddenShop.id to hiddenShop)),
            uiState.markerShops,
        )
    }

    @Test
    fun `사용자가 숨김 처리한 매장도 검색 결과에서는 투명 표시 대상으로 유지한다`() {
        val hiddenShop =
            ramenShopFixture(
                id = "hidden-by-user-shop",
                name = "사용자 숨김 매장",
                isVisible = true,
            )
        val displayShop = hiddenShop.copy(isVisible = false)
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "사용자 숨김",
                        results = RamenShops(mapOf(hiddenShop.id to hiddenShop)),
                    ),
                hiddenShopIds = setOf(hiddenShop.id),
            )

        assertEquals(listOf(displayShop), uiState.searchResultShops)
        assertEquals(RamenShops(mapOf(displayShop.id to displayShop)), uiState.markerShops)
        assertEquals(SearchResultGuide.HIDDEN_ONLY, uiState.searchResultGuide)
        assertEquals(listOf(displayShop), uiState.focusShops)
    }

    @Test
    fun `사용자 숨김 매장을 해제하면 검색 결과 표시도 바로 노출 상태가 된다`() {
        val shop =
            ramenShopFixture(
                id = "unhidden-search-shop",
                name = "숨김 해제 매장",
                isVisible = true,
            )
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "숨김 해제",
                        results = RamenShops(mapOf(shop.id to shop)),
                    ),
                hiddenShopIds = emptySet(),
            )

        assertEquals(listOf(shop), uiState.searchResultShops)
        assertEquals(RamenShops(mapOf(shop.id to shop)), uiState.markerShops)
    }

    @Test
    fun `현재 검색 결과가 도착하기 전에는 기존 지도 영역 매장 마커를 유지한다`() =
        coroutinesTest {
            val mapShops =
                RamenShops(
                    listOf(
                        ramenShopFixture().copy(
                            id = "map-shop",
                            name = "지도 매장",
                        ),
                    ).associateBy { it.id },
                )
            val searchShops =
                RamenShops(
                    listOf(
                        ramenShopFixture().copy(
                            id = "search-shop",
                            name = "검색 매장",
                        ),
                    ).associateBy { it.id },
                )
            val ramenShopRepository =
                FakeRamenShopRepository(
                    result = mapShops,
                    searchResult = searchShops,
                )
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(299)
            runCurrent()

            assertEquals(mapShops, viewModel.uiState.value.markerShops)

            advanceTimeBy(1)
            runCurrent()

            assertEquals(RamenShops(mapShops + searchShops), viewModel.uiState.value.markerShops)
        }

    @Test
    fun `검색어가 연속으로 변경되면 마지막 검색어만 검색한다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository()
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(150)
            viewModel.dispatch(OnQueryChanged("라멘집"))
            advanceTimeBy(299)
            runCurrent()

            assertEquals(emptyList(), ramenShopRepository.requestedSearchQueries)

            advanceTimeBy(1)
            runCurrent()

            assertEquals(listOf(SearchQuery("라멘집")), ramenShopRepository.requestedSearchQueries)
        }

    @Test
    fun `검색 결과 바텀시트를 닫아도 검색어와 검색 결과는 유지한다`() =
        coroutinesTest {
            val searchShops =
                RamenShops(
                    listOf(
                        ramenShopFixture().copy(id = "search-shop-1"),
                        ramenShopFixture().copy(id = "search-shop-2"),
                    ).associateBy { it.id },
                )
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(true, viewModel.uiState.value.showSearchResults)
            assertEquals(true, viewModel.uiState.value.showBottomSheet)

            viewModel.dispatch(OnSearchResultsDismissed)
            runCurrent()

            assertEquals("라멘", viewModel.uiState.value.search.input)
            assertEquals(searchShops, viewModel.uiState.value.search.results)
            assertEquals(searchShops, viewModel.uiState.value.markerShops)
            assertEquals(false, viewModel.uiState.value.showSearchResults)
            assertEquals(false, viewModel.uiState.value.showBottomSheet)
        }

    @Test
    fun `동일 검색어로 다시 검색하면 재조회하지 않고 검색 결과 바텀시트를 다시 연다`() =
        coroutinesTest {
            val searchShops =
                RamenShops(
                    listOf(
                        ramenShopFixture().copy(id = "search-shop-1"),
                        ramenShopFixture().copy(id = "search-shop-2"),
                    ).associateBy { it.id },
                )
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()
            viewModel.dispatch(OnSearchResultsDismissed)
            runCurrent()
            ramenShopRepository.requestedSearchQueries.clear()
            val previousFocusRequestKey = viewModel.uiState.value.focusRequestKey

            viewModel.dispatch(OnQueryChanged("라멘"))
            runCurrent()

            assertEquals(emptyList(), ramenShopRepository.requestedSearchQueries)
            assertEquals(searchShops, viewModel.uiState.value.search.results)
            assertEquals(previousFocusRequestKey + 1, viewModel.uiState.value.focusRequestKey)
            assertEquals(searchShops.values.toList(), viewModel.uiState.value.focusShops)
            assertEquals(true, viewModel.uiState.value.showSearchResults)
            assertEquals(true, viewModel.uiState.value.showBottomSheet)
        }

    @Test
    fun `검색 결과 상세에서 매장을 숨기면 상세를 닫고 검색 결과에는 투명 표시로 유지한다`() =
        coroutinesTest {
            val shop =
                ramenShopFixture(
                    id = "search-hidden-by-user-shop",
                    name = "검색 후 숨김 매장",
                    isVisible = true,
                )
            val searchShops = RamenShops(mapOf(shop.id to shop))
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val viewModel =
                mapViewModel(
                    ramenShopRepository = ramenShopRepository,
                    loginRepository = loggedInRepository(),
                )
            runCurrent()

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(shop, viewModel.uiState.value.selectedShop)

            viewModel.dispatch(OnHiddenToggled(shop))
            runCurrent()

            val hiddenDisplayShop = shop.copy(isVisible = false)

            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(setOf(shop.id), viewModel.uiState.value.hiddenShopIds)
            assertEquals(listOf(hiddenDisplayShop), viewModel.uiState.value.searchResultShops)
            assertEquals(
                RamenShops(mapOf(shop.id to hiddenDisplayShop)),
                viewModel.uiState.value.markerShops,
            )
            assertEquals(emptyList(), viewModel.uiState.value.focusShops)
            assertEquals(false, viewModel.uiState.value.showBottomSheet)
            assertEquals(SearchResultGuide.HIDDEN_ONLY, viewModel.uiState.value.searchResultGuide)
        }

    @Test
    fun `매장을 숨김 처리하면 완료 토스트를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "hidden-toast-shop")
            val viewModel = mapViewModel(loginRepository = loggedInRepository())
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnHiddenToggled(shop))
                runCurrent()

                assertEquals(showToastSideEffect(Res.string.hide_shop_success_message), awaitItem())
            }
        }

    @Test
    fun `숨김 매장을 선택해 이동하면 전체 보기에서도 반투명 마커로 유지한다`() =
        coroutinesTest {
            val hiddenShop =
                ramenShopFixture(
                    id = "hidden-selected-shop",
                    name = "숨김 선택 매장",
                    isVisible = false,
                )
            val personalizationRepository =
                FakePersonalizationRepository(
                    Personalization(hiddenShopIds = setOf(hiddenShop.id)),
                )
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(hiddenShop.id to hiddenShop.copy(isVisible = true))),
                        ),
                    personalizationRepository = personalizationRepository,
                    loginRepository = loggedInRepository(),
                )
            runCurrent()

            viewModel.dispatch(OnShopSelected(hiddenShop))
            runCurrent()

            assertEquals(hiddenShop, viewModel.uiState.value.selectedShop)
            assertEquals(
                RamenShops(mapOf(hiddenShop.id to hiddenShop)),
                viewModel.uiState.value.markerShops,
            )
            assertEquals(listOf(hiddenShop), viewModel.uiState.value.focusShops)
        }

    @Test
    fun `검색 결과에서 숨긴 단일 매장을 같은 검색어로 다시 검색하면 재조회 없이 숨김 안내를 보여준다`() =
        coroutinesTest {
            val shop =
                ramenShopFixture(
                    id = "same-query-hidden-shop",
                    name = "같은 검색어 숨김 매장",
                    isVisible = true,
                )
            val searchShops = RamenShops(mapOf(shop.id to shop))
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val waitingSystemRepository = FakeShopWaitingSystemRepository()
            val viewModel =
                mapViewModel(
                    ramenShopRepository = ramenShopRepository,
                    shopWaitingSystemRepository = waitingSystemRepository,
                    loginRepository = loggedInRepository(),
                )
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnQueryChanged("라멘"))
                advanceTimeBy(300)
                runCurrent()

                viewModel.dispatch(OnHiddenToggled(shop))
                runCurrent()
                ramenShopRepository.requestedSearchQueries.clear()

                viewModel.dispatch(OnQueryChanged("  라멘  "))
                runCurrent()

                assertEquals(emptyList(), ramenShopRepository.requestedSearchQueries)
                assertEquals(null, viewModel.uiState.value.selectedShop)
                assertEquals(setOf(shop.id), viewModel.uiState.value.hiddenShopIds)
                assertEquals(listOf(shop.id), waitingSystemRepository.requestedShopIds)
                assertEquals(showToastSideEffect(Res.string.hide_shop_success_message), awaitItem())
                assertEquals(showToastSideEffect(Res.string.hidden_shop_search_result_message), awaitItem())
            }
        }

    @Test
    fun `검색 결과 마커를 선택하면 선택한 매장만 포커스하고 근거리 자동 이동을 끈다`() {
        val selectedShop = ramenShopFixture(id = "oreno-lotte-world-mall")
        val otherShop = ramenShopFixture(id = "oreno-gangnam")
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "오레노",
                        results = RamenShops(listOf(selectedShop, otherShop).associateBy { it.id }),
                    ),
                selectedShop = selectedShop,
            )

        assertEquals(listOf(selectedShop), uiState.focusShops)
        assertEquals(false, uiState.shouldFocusNearestSearchResult)
    }

    @Test
    fun `검색 결과 매장 상세를 닫아도 검색 결과 근거리 자동 이동을 다시 실행하지 않는다`() =
        coroutinesTest {
            val selectedShop = ramenShopFixture(id = "oreno-lotte-world-mall")
            val otherShop = ramenShopFixture(id = "oreno-gangnam")
            val searchShops =
                RamenShops(
                    listOf(selectedShop, otherShop).associateBy { it.id },
                )
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnQueryChanged("오레노"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(searchShops.values.toList(), viewModel.uiState.value.focusShops)
            assertEquals(true, viewModel.uiState.value.shouldFocusNearestSearchResult)

            viewModel.dispatch(OnShopSelected(selectedShop))
            runCurrent()

            assertEquals(true, viewModel.uiState.value.search.isResultFocusConsumed)
            assertEquals(listOf(selectedShop), viewModel.uiState.value.focusShops)
            assertEquals(false, viewModel.uiState.value.shouldFocusNearestSearchResult)

            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(true, viewModel.uiState.value.showSearchResults)
            assertEquals(emptyList(), viewModel.uiState.value.focusShops)
            assertEquals(false, viewModel.uiState.value.shouldFocusNearestSearchResult)
        }

    @Test
    fun `다중 검색 결과가 열려 있으면 현재 위치 기준 근거리 자동 이동 대상이다`() {
        val searchShops =
            RamenShops(
                listOf(
                    ramenShopFixture(id = "oreno-lotte-world-mall"),
                    ramenShopFixture(id = "oreno-gangnam"),
                ).associateBy { it.id },
            )
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "오레노",
                        results = searchShops,
                    ),
            )

        assertEquals(searchShops.values.toList(), uiState.focusShops)
        assertEquals(true, uiState.shouldFocusNearestSearchResult)
    }

    @Test
    fun `내 위치가 있으면 다중 검색 결과에서 가장 가까운 매장을 먼저 보여준다`() {
        val farShop =
            ramenShopFixture(
                id = "far-shop",
                location = Location(lat = 37.65, lng = 127.05),
            )
        val nearShop =
            ramenShopFixture(
                id = "near-shop",
                location = Location(lat = 37.551, lng = 126.921),
            )
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "오레노",
                        results = RamenShops(listOf(farShop, nearShop).associateBy { it.id }),
                    ),
                currentLocation = Location(lat = 37.55, lng = 126.92),
            )

        assertEquals(listOf(nearShop, farShop), uiState.searchResultShops)
        assertEquals(listOf(nearShop, farShop), uiState.focusShops)
    }

    @Test
    fun `검색어가 비어 있으면 검색 결과를 비우고 현재 지도 영역 매장을 보여준다`() =
        coroutinesTest {
            val mapShops =
                RamenShops(
                    listOf(
                        ramenShopFixture().copy(
                            id = "map-shop",
                            name = "지도 매장",
                        ),
                    ).associateBy { it.id },
                )
            val searchShops =
                RamenShops(
                    listOf(
                        ramenShopFixture().copy(
                            id = "search-shop",
                            name = "검색 매장",
                        ),
                    ).associateBy { it.id },
                )
            val ramenShopRepository =
                FakeRamenShopRepository(
                    result = mapShops,
                    searchResult = searchShops,
                )
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()
            viewModel.dispatch(OnQueryChanged("   "))
            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.search.results)
            assertEquals(mapShops, viewModel.uiState.value.shops)
            assertEquals(mapShops, viewModel.uiState.value.markerShops)
            assertEquals(listOf(SearchQuery("라멘")), ramenShopRepository.requestedSearchQueries)
        }

    @Test
    fun `카테고리 필터를 선택하면 필터 상태가 갱신되고 마커 목록에 적용된다`() =
        coroutinesTest {
            val mazesobaShop =
                ramenShopFixture(
                    id = "mazesoba-shop",
                    menuCategories = listOf(Category.MAZESOBA),
                )
            val jiroShop =
                ramenShopFixture(
                    id = "jiro-shop",
                    menuCategories = listOf(Category.JIRO),
                )
            val shops = RamenShops(listOf(mazesobaShop, jiroShop).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(result = shops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            runCurrent()

            assertEquals(
                setOf(Category.MAZESOBA),
                viewModel.uiState.value.filters
                    .toSet(),
            )
            assertEquals(
                RamenShops(mapOf(mazesobaShop.id to mazesobaShop)),
                viewModel.uiState.value.markerShops,
            )
        }

    @Test
    fun `선택된 카테고리를 다시 선택하면 필터에서 제거된다`() =
        coroutinesTest {
            val viewModel = mapViewModel()

            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            runCurrent()
            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            runCurrent()

            assertEquals(
                emptySet(),
                viewModel.uiState.value.filters
                    .toSet(),
            )
        }

    @Test
    fun `필터 초기화 인텐트는 모든 카테고리 필터를 제거한다`() =
        coroutinesTest {
            val viewModel = mapViewModel()

            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            viewModel.dispatch(OnCategoryFilterToggled(Category.JIRO))
            runCurrent()
            viewModel.dispatch(OnFilterCleared)
            runCurrent()

            assertEquals(
                emptySet(),
                viewModel.uiState.value.filters
                    .toSet(),
            )
        }

    @Test
    fun `필터와 맞지 않는 선택 매장은 닫는다`() =
        coroutinesTest {
            val shop =
                ramenShopFixture(
                    id = "shoyu-shop",
                    menuCategories = listOf(Category.SHOYU),
                )
            val viewModel = mapViewModel()

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            runCurrent()

            assertEquals(null, viewModel.uiState.value.selectedShop)
        }

    @Test
    fun `검색 결과도 카테고리 필터가 적용된 목록을 보여준다`() =
        coroutinesTest {
            val mazesobaShop =
                ramenShopFixture(
                    id = "mazesoba-shop",
                    menuCategories = listOf(Category.MAZESOBA),
                )
            val jiroShop =
                ramenShopFixture(
                    id = "jiro-shop",
                    menuCategories = listOf(Category.JIRO),
                )
            val searchShops = RamenShops(listOf(mazesobaShop, jiroShop).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(listOf(mazesobaShop), viewModel.uiState.value.searchResultShops)
            assertEquals(
                RamenShops(mapOf(mazesobaShop.id to mazesobaShop)),
                viewModel.uiState.value.markerShops,
            )
        }

    @Test
    fun `필터와 맞지 않는 단일 검색 결과는 자동 선택하지 않는다`() =
        coroutinesTest {
            val shop =
                ramenShopFixture(
                    id = "jiro-shop",
                    menuCategories = listOf(Category.JIRO),
                )
            val searchShops = RamenShops(listOf(shop).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(emptyList(), viewModel.uiState.value.searchResultShops)
        }

    @Test
    fun `검색 결과가 없으면 바텀시트를 보여주지 않는다`() {
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "없는매장",
                        results = RamenShops(emptyMap()),
                    ),
            )

        assertEquals(SearchResultGuide.SEARCH_EMPTY, uiState.searchResultGuide)
        assertEquals(false, uiState.showSearchResults)
        assertEquals(false, uiState.showBottomSheet)
    }

    @Test
    fun `검색어와 선택한 필터에 맞는 매장이 없으면 바텀시트를 보여주지 않는다`() {
        val shop =
            ramenShopFixture(
                id = "jiro-shop",
                menuCategories = listOf(Category.JIRO),
            )
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "라멘",
                        results = RamenShops(mapOf(shop.id to shop)),
                    ),
                filters = RamenShopFilter(setOf(Category.MAZESOBA)),
            )

        assertEquals(SearchResultGuide.QUERY_AND_FILTER_EMPTY, uiState.searchResultGuide)
        assertEquals(false, uiState.showSearchResults)
        assertEquals(false, uiState.showBottomSheet)
    }

    @Test
    fun `숨긴 매장 보기에서는 숨긴 매장만 마커로 보여준다`() {
        val visibleShop = ramenShopFixture(id = "visible-shop")
        val hiddenShop = ramenShopFixture(id = "hidden-shop")
        val displayHiddenShop = hiddenShop.copy(isVisible = false)
        val uiState =
            MapUiState(
                shops = RamenShops(listOf(visibleShop, hiddenShop).associateBy { it.id }),
                hiddenShopIds = setOf(hiddenShop.id),
                personalizationView = MapPersonalization.HIDDEN,
            )

        assertEquals(RamenShops(mapOf(displayHiddenShop.id to displayHiddenShop)), uiState.markerShops)
        assertEquals(false, uiState.showBottomSheet)
    }

    @Test
    fun `북마크 보기에서 검색하면 북마크한 검색 결과만 보여준다`() {
        val bookmarkedShop = ramenShopFixture(id = "bookmarked-search-shop")
        val unbookmarkedShop = ramenShopFixture(id = "unbookmarked-search-shop")
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "북마크",
                        results =
                            RamenShops(
                                listOf(bookmarkedShop, unbookmarkedShop).associateBy { it.id },
                            ),
                    ),
                bookmarkedShopIds = setOf(bookmarkedShop.id),
                personalizationView = MapPersonalization.BOOKMARKED,
            )

        assertEquals(listOf(bookmarkedShop), uiState.searchResultShops)
        assertEquals(RamenShops(mapOf(bookmarkedShop.id to bookmarkedShop)), uiState.markerShops)
        assertEquals(listOf(bookmarkedShop), uiState.focusShops)
        assertEquals(false, uiState.showSearchResults)
    }

    @Test
    fun `북마크한 매장이 없으면 검색 가이드 바텀시트를 보여주지 않는다`() {
        val shop = ramenShopFixture(id = "unbookmarked-search-shop")
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "라멘",
                        results = RamenShops(mapOf(shop.id to shop)),
                    ),
                bookmarkedShopIds = emptySet(),
                personalizationView = MapPersonalization.BOOKMARKED,
            )

        assertEquals(null, uiState.searchResultGuide)
        assertEquals(false, uiState.showSearchResults)
        assertEquals(false, uiState.showBottomSheet)
    }

    @Test
    fun `숨긴 매장 보기에서 검색하면 숨김 처리한 검색 결과만 투명 표시한다`() {
        val hiddenShop = ramenShopFixture(id = "hidden-search-shop")
        val visibleShop = ramenShopFixture(id = "visible-search-shop")
        val displayHiddenShop = hiddenShop.copy(isVisible = false)
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "숨김",
                        results =
                            RamenShops(
                                listOf(hiddenShop, visibleShop).associateBy { it.id },
                            ),
                    ),
                hiddenShopIds = setOf(hiddenShop.id),
                personalizationView = MapPersonalization.HIDDEN,
            )

        assertEquals(listOf(displayHiddenShop), uiState.searchResultShops)
        assertEquals(
            RamenShops(mapOf(displayHiddenShop.id to displayHiddenShop)),
            uiState.markerShops,
        )
        assertEquals(listOf(displayHiddenShop), uiState.focusShops)
        assertEquals(false, uiState.showSearchResults)
        assertEquals(false, uiState.showBottomSheet)
    }

    @Test
    fun `검색 결과 매장을 숨기면 전체 보기 검색 결과에서 투명 표시 대상으로 유지한다`() {
        val hiddenShop = ramenShopFixture(id = "hidden-after-search-shop")
        val visibleShop = ramenShopFixture(id = "visible-search-shop")
        val displayHiddenShop = hiddenShop.copy(isVisible = false)
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "라멘",
                        results =
                            RamenShops(
                                listOf(hiddenShop, visibleShop).associateBy { it.id },
                            ),
                    ),
                hiddenShopIds = setOf(hiddenShop.id),
            )

        assertEquals(listOf(displayHiddenShop, visibleShop), uiState.searchResultShops)
        assertEquals(
            RamenShops(
                mapOf(
                    displayHiddenShop.id to displayHiddenShop,
                    visibleShop.id to visibleShop,
                ),
            ),
            uiState.markerShops,
        )
        assertEquals(true, uiState.showSearchResults)
    }

    @Test
    fun `검색 결과 포커스를 소비한 뒤 필터가 바뀌어도 자동 포커스를 다시 실행하지 않는다`() {
        val selectedShop =
            ramenShopFixture(
                id = "selected-search-shop",
                menuCategories = listOf(Category.MAZESOBA),
            )
        val otherShop =
            ramenShopFixture(
                id = "other-search-shop",
                menuCategories = listOf(Category.MAZESOBA),
            )
        val uiState =
            MapUiState(
                search =
                    SearchUiState.loaded(
                        input = "오레노",
                        results =
                            RamenShops(
                                listOf(selectedShop, otherShop).associateBy { it.id },
                            ),
                        isResultFocusConsumed = true,
                    ),
                filters = RamenShopFilter(setOf(Category.MAZESOBA)),
            )

        assertEquals(listOf(selectedShop, otherShop), uiState.searchResultShops)
        assertEquals(emptyList(), uiState.focusShops)
        assertEquals(false, uiState.shouldFocusNearestSearchResult)
    }

    @Test
    fun `비로그인 상태에서 북마크를 누르면 로그인 안내를 요청한다`() =
        coroutinesTest {
            val viewModel = mapViewModel()
            val shop = ramenShopFixture()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnBookmarkToggled(shop))
                runCurrent()

                assertEquals(ShowLoginGuide, awaitItem())
            }
        }

    @Test
    fun `숨김 처리한 매장도 북마크할 수 있다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "hidden-bookmark-shop")
            val personalizationRepository =
                FakePersonalizationRepository(
                    Personalization(hiddenShopIds = setOf(shop.id)),
                )
            val viewModel =
                mapViewModel(
                    personalizationRepository = personalizationRepository,
                    loginRepository = loggedInRepository(),
                )
            runCurrent()

            viewModel.dispatch(OnBookmarkToggled(shop))
            runCurrent()

            assertEquals(setOf(shop.id), viewModel.uiState.value.bookmarkedShopIds)
        }

    @Test
    fun `비로그인 상태에서 숨기기를 누르면 로그인 안내를 요청한다`() =
        coroutinesTest {
            val viewModel = mapViewModel()
            val shop = ramenShopFixture()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnHiddenToggled(shop))
                runCurrent()

                assertEquals(ShowLoginGuide, awaitItem())
            }
        }

    @Test
    fun `필터 적용 후 숨김 검색 결과만 남으면 상세를 열지 않고 숨김 안내를 보여준다`() =
        coroutinesTest {
            val hiddenShop =
                ramenShopFixture(
                    id = "hidden-mazesoba-shop",
                    menuCategories = listOf(Category.MAZESOBA),
                    isVisible = false,
                )
            val visibleShop =
                ramenShopFixture(
                    id = "visible-jiro-shop",
                    menuCategories = listOf(Category.JIRO),
                    isVisible = true,
                )
            val searchShops = RamenShops(listOf(hiddenShop, visibleShop).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops)
            val waitingSystemRepository = FakeShopWaitingSystemRepository()
            val viewModel =
                mapViewModel(
                    ramenShopRepository = ramenShopRepository,
                    shopWaitingSystemRepository = waitingSystemRepository,
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
                viewModel.dispatch(OnQueryChanged("라멘"))
                advanceTimeBy(300)
                runCurrent()

                assertEquals(null, viewModel.uiState.value.selectedShop)
                assertEquals(false, viewModel.uiState.value.showBottomSheet)
                assertEquals(SearchResultGuide.HIDDEN_ONLY, viewModel.uiState.value.searchResultGuide)
                assertEquals(listOf(hiddenShop), viewModel.uiState.value.searchResultShops)
                assertEquals(listOf(hiddenShop), viewModel.uiState.value.focusShops)
                assertEquals(emptyList(), waitingSystemRepository.requestedShopIds)
                assertEquals(showToastSideEffect(Res.string.filter_empty_visible_result_message), awaitItem())
                assertEquals(showToastSideEffect(Res.string.hidden_shop_search_result_message), awaitItem())
            }
        }
}

private fun showToastSideEffect(message: StringResource): ShowToast =
    ShowToast(
        ToastData(
            message = message,
            type = ToastType.DEFAULT,
        ),
    )

private fun mapViewModel(
    ramenShopRepository: RamenShopRepository = FakeRamenShopRepository(),
    shopWaitingSystemRepository: ShopWaitingSystemRepository = FakeShopWaitingSystemRepository(),
    personalizationRepository: PersonalizationRepository = FakePersonalizationRepository(),
    loginRepository: FakeLoginRepository = FakeLoginRepository(),
    shopReportRepository: ShopReportRepository = FakeShopReportRepository(),
): MapViewModel =
    MapViewModel(
        ramenShopRepository,
        shopWaitingSystemRepository,
        personalizationRepository,
        shopReportRepository,
        loginRepository,
        CurrentLocationStore(),
    )

private fun loggedInRepository(): FakeLoginRepository =
    FakeLoginRepository(
        initialSessionStatus = authenticatedStatus(),
        userEmail = "test@ramap.com",
    )

private fun authenticatedStatus(): SessionStatus =
    SessionStatus.Authenticated(
        session =
            UserSession(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                expiresIn = 3600,
                tokenType = "bearer",
            ),
    )
