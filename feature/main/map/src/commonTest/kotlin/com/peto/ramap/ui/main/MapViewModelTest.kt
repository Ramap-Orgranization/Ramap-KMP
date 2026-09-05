package com.peto.ramap.ui.main

import app.cash.turbine.test
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.shop.model.ShopDetailSheetUiState
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.report.ShopInformationReport
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShopFilter
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeNotificationSettingsRepository
import com.peto.ramap.fake.FakeOperatingNoticeRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopReportRepository
import com.peto.ramap.fake.FakeShopWaitingSystemRepository
import com.peto.ramap.fixture.BOUNDS_FIXTURE
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.fixture.waitingSystemFixture
import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.main.map.MapViewModel
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkedShopsToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBoundsChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCameraPositionChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCategoryFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnHiddenToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialLocationFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLoginTypeSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMapTabExited
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMyLocationChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnOpenFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnQueryChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRequestedShopDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSelectedShopFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailRetry
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopIdSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopNotificationToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopShareClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnViewportLoadRetry
import com.peto.ramap.ui.main.map.contract.MapLoadKey
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShareShop
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowLoginGuide
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowToast
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.log.MapAnalytics
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.location.LocationFocusStatus
import com.peto.ramap.ui.main.map.model.search.SearchResultGuide
import com.peto.ramap.ui.main.map.model.search.SearchUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.hidden_shop_notification_unavailable_message
import ramap.shared.generated.resources.hidden_shop_search_result_message
import ramap.shared.generated.resources.hide_shop_success_message
import ramap.shared.generated.resources.search_result_empty_message
import ramap.shared.generated.resources.search_result_hidden_only_message
import ramap.shared.generated.resources.shop_information_report_failure_message
import ramap.shared.generated.resources.shop_information_report_success_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    @Test
    fun `랭킹 매장 상세를 연 뒤 지도 탭을 떠나면 모든 바텀시트를 닫는다`() =
        coroutinesTest {
            val selectedShop = ramenShopFixture(id = "selected-shop")
            val searchShops =
                RamenShops(
                    listOf(
                        selectedShop,
                        ramenShopFixture(id = "other-shop"),
                    ).associateBy { it.id },
                )
            val viewModel =
                mapViewModel(
                    ramenShopRepository = FakeRamenShopRepository(searchResult = searchShops),
                )

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()
            viewModel.dispatch(OnShopIdSelected(selectedShop.id))
            runCurrent()

            assertEquals(true, viewModel.uiState.value.showBottomSheet)

            viewModel.dispatch(OnMapTabExited)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals("", viewModel.uiState.value.search.input)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.search.results)
            assertEquals(false, viewModel.uiState.value.showSearchResults)
            assertEquals(false, viewModel.uiState.value.showBottomSheet)
        }

    @Test
    fun `지도 탭을 떠나면 진행 중인 검색과 검색 상태를 초기화한다`() =
        coroutinesTest {
            val viewModel =
                mapViewModel(
                    ramenShopRepository = FakeRamenShopRepository(searchDelayMillis = 1_000),
                )

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()
            assertEquals(true, viewModel.uiState.value.isSearchLoading)

            viewModel.dispatch(OnMapTabExited)
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(SearchUiModel(), viewModel.uiState.value.search)
            assertEquals(false, viewModel.uiState.value.isSearchLoading)
        }

    @Test
    fun `매장 공유를 누르면 공유 정보 side effect를 보낸다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val viewModel = mapViewModel()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnShopShareClicked(shop))

                assertEquals(
                    ShareShop(
                        shopId = shop.id,
                        shopName = shop.name,
                    ),
                    awaitItem(),
                )
            }
        }

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

            assertEquals(
                listOf(lastBounds.expandBy(0.5)),
                ramenShopRepository.requestedBoundsHistory,
            )
        }

    @Test
    fun `마지막 성공 선조회 영역 안에서 이동하면 조회하지 않는다`() =
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

            assertEquals(
                listOf(BOUNDS_FIXTURE.expandBy(0.5)),
                ramenShopRepository.requestedBoundsHistory,
            )
        }

    @Test
    fun `마지막 성공 선조회 영역을 벗어나면 새 확장 영역을 조회한다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository()
            val viewModel = mapViewModel(ramenShopRepository)
            val changedBounds =
                BOUNDS_FIXTURE.copy(
                    minLat = BOUNDS_FIXTURE.minLat + BOUNDS_FIXTURE.latSpan,
                    maxLat = BOUNDS_FIXTURE.maxLat + BOUNDS_FIXTURE.latSpan,
                )

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(OnBoundsChanged(changedBounds))
            advanceTimeBy(350)
            runCurrent()

            assertEquals(
                listOf(BOUNDS_FIXTURE.expandBy(0.5), changedBounds.expandBy(0.5)),
                ramenShopRepository.requestedBoundsHistory,
            )
        }

    @Test
    fun `지도 영역 조회 실패 후 재시도하면 실패 상태를 지우고 현재 영역을 다시 조회한다`() =
        coroutinesTest {
            val shops = RamenShops(listOf(ramenShopFixture()).associateBy { it.id })
            val ramenShopRepository =
                FakeRamenShopRepository(
                    result = shops,
                    error = RamapError.Unknown(IllegalStateException("failed")),
                )
            val viewModel = mapViewModel(ramenShopRepository)
            val expandedBounds = BOUNDS_FIXTURE.expandBy(0.5)

            viewModel.dispatch(OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()

            assertEquals(true, viewModel.uiState.value.hasViewportLoadFailed)
            assertEquals(listOf(expandedBounds), ramenShopRepository.requestedBoundsHistory)

            ramenShopRepository.error = null
            viewModel.dispatch(OnViewportLoadRetry)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.hasViewportLoadFailed)

            advanceTimeBy(350)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.hasViewportLoadFailed)
            assertEquals(shops, viewModel.uiState.value.shops)
            assertEquals(
                listOf(expandedBounds, expandedBounds),
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
                    minLat = BOUNDS_FIXTURE.minLat + BOUNDS_FIXTURE.latSpan,
                    maxLat = BOUNDS_FIXTURE.maxLat + BOUNDS_FIXTURE.latSpan,
                )

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertEquals(RamenShops(emptyMap()), initialState.shops)
                assertEquals(MapBounds(), initialState.bounds)

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
    fun `선택 매장 카메라 이동이 수행되면 포커스를 소비한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val cameraPosition =
                CameraPosition(
                    center = Location(lat = 37.5665, lng = 126.9780),
                    zoom = 14.5,
                )
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        ),
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.focusShops)

            viewModel.dispatch(OnCameraPositionChanged(cameraPosition))
            runCurrent()

            assertEquals(cameraPosition, viewModel.uiState.value.cameraPosition)
            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.focusShops)

            viewModel.dispatch(OnSelectedShopFocusConsumed)
            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.focusShops)
            assertEquals(shop, viewModel.uiState.value.selectedShop)
        }

    @Test
    fun `랭킹 상세에서 지도 진입시 초기 카메라 갱신보다 요청 매장 포커스를 우선한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "ranking-requested-shop")
            val currentLocation = Location(lat = 37.275, lng = 127.009)
            val currentLocationCamera =
                CameraPosition(
                    center = currentLocation,
                    zoom = 14.0,
                )
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        ),
                )

            viewModel.dispatch(OnShopIdSelected(shop.id))
            runCurrent()

            viewModel.dispatch(OnShopDetailDismissed)
            viewModel.dispatch(OnShopIdSelected(shop.id))
            viewModel.dispatch(OnCameraPositionChanged(currentLocationCamera))
            runCurrent()

            assertEquals(currentLocationCamera, viewModel.uiState.value.cameraPosition)
            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.focusShops)

            viewModel.dispatch(OnSelectedShopFocusConsumed)
            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.focusShops)
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
    fun `매장 상세 조회에 실패하면 선택을 유지하고 바텀시트 오류 상태를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            error = RamapError.Unknown(IllegalStateException("failed")),
                        ),
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            assertEquals(
                ShopDetailSheetUiState.Error(shop.id, shop),
                viewModel.uiState.value.shopDetailState,
            )
            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(null, viewModel.uiState.value.shopDetail)
            assertEquals(true, viewModel.uiState.value.hasShopDetailLoadFailed)
            assertEquals(true, viewModel.uiState.value.showBottomSheet)
            assertEquals(false, viewModel.uiState.value.isShopDetailLoading)

            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()

            assertEquals(ShopDetailSheetUiState.Closed, viewModel.uiState.value.shopDetailState)
            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(false, viewModel.uiState.value.hasShopDetailLoadFailed)
            assertEquals(false, viewModel.uiState.value.showBottomSheet)
        }

    @Test
    fun `매장 상세 실패 후 재시도하면 같은 매장을 조회하고 상세를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val repository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                    error = RamapError.Unknown(IllegalStateException("failed")),
                )
            val viewModel = mapViewModel(ramenShopRepository = repository)

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            repository.error = null
            viewModel.dispatch(OnShopDetailRetry)
            runCurrent()

            assertEquals(
                shop.id,
                viewModel.uiState.value
                    .shopDetail
                    ?.shop
                    ?.id,
            )
            assertEquals(false, viewModel.uiState.value.hasShopDetailLoadFailed)
            assertEquals(false, viewModel.uiState.value.isShopDetailLoading)
            assertEquals(3, repository.requestedShopIdsHistory.size)
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
    fun `매장 상세를 닫으면 진행 중인 상세 조회를 취소한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val waitingSystem = waitingSystemFixture(shop.id)
            var didCompleteWaitingSystemLoad = false
            val waitingSystemRepository =
                object : ShopWaitingSystemRepository {
                    override suspend fun fetchShopWaitingSystem(shopId: String) =
                        delay(1_000).let {
                            didCompleteWaitingSystemLoad = true
                            RamapResult.Success(waitingSystem)
                        }
                }
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

            assertEquals(true, viewModel.uiState.value.isShopDetailLoading)

            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(null, viewModel.uiState.value.shopDetail)
            assertEquals(false, viewModel.uiState.value.isShopDetailLoading)
            assertEquals(false, didCompleteWaitingSystemLoad)
        }

    @Test
    fun `서로 다른 가게의 상세는 각각 한 번씩 캐싱한다`() =
        coroutinesTest {
            val firstShop = ramenShopFixture(id = "shop-1")
            val secondShop = ramenShopFixture(id = "shop-2")
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult =
                        RamenShops(
                            mapOf(
                                firstShop.id to firstShop,
                                secondShop.id to secondShop,
                            ),
                        ),
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
            assertEquals(
                listOf(firstShop.id, secondShop.id),
                waitingSystemRepository.requestedShopIds,
            )
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
    fun `아이디로 상세를 조회하는 동안 로딩하고 성공하면 매장 전체 상세를 선택하고 포커스한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "requested-shop")
            val waitingSystem = waitingSystemFixture(shop.id)
            val delegate =
                FakeRamenShopRepository(fetchByIdsResult = RamenShops(mapOf(shop.id to shop)))
            val waitingSystemRepository = FakeShopWaitingSystemRepository(result = waitingSystem)
            val repository =
                object : RamenShopRepository by delegate {
                    override suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops> {
                        delay(1_000)
                        return delegate.fetchRamenShops(shopIds)
                    }
                }
            val viewModel =
                mapViewModel(
                    ramenShopRepository = repository,
                    shopWaitingSystemRepository = waitingSystemRepository,
                )

            viewModel.dispatch(OnShopIdSelected(shop.id))
            runCurrent()

            assertEquals(
                ShopDetailSheetUiState.Loading(shop.id, shop = null),
                viewModel.uiState.value.shopDetailState,
            )
            assertEquals(true, viewModel.uiState.value.isShopDetailLoading)
            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(false, viewModel.uiState.value.hasShopDetailLoadFailed)

            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.isShopDetailLoading)
            assertEquals(false, viewModel.uiState.value.hasShopDetailLoadFailed)
            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(
                shop,
                viewModel.uiState.value
                    .shopDetail
                    ?.shop,
            )
            assertEquals(waitingSystem, viewModel.uiState.value.shopWaiting[shop.id])
            assertEquals(
                listOf(shop),
                viewModel.uiState.value
                    .focusShops
                    .values
                    .toList(),
            )
            assertEquals(listOf(setOf(shop.id)), delegate.requestedShopIdsHistory)
            assertEquals(listOf(shop.id), waitingSystemRepository.requestedShopIds)
            assertEquals(listOf(shop.id), delegate.requestedActiveEventShopIds)

            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()
        }

    @Test
    fun `아이디로 요청한 매장 포커스는 이후 수신한 최초 현재 위치로 덮어쓰지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "requested-shop")
            val currentLocation = Location(lat = 37.275, lng = 127.009)
            val repository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                )
            val viewModel = mapViewModel(ramenShopRepository = repository)

            viewModel.dispatch(OnShopIdSelected(shop.id))
            runCurrent()
            viewModel.dispatch(OnMyLocationChanged(currentLocation))
            runCurrent()

            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(
                listOf(shop),
                viewModel.uiState.value.focusShops.values
                    .toList(),
            )
            assertEquals(currentLocation, viewModel.uiState.value.currentLocation)
            assertEquals(null, viewModel.uiState.value.initialFocusLocation)
            assertEquals(
                LocationFocusStatus.Consumed,
                viewModel.uiState.value.locationFocusStatus,
            )
            assertEquals(false, viewModel.uiState.value.shouldBootstrapLocationFocusStatus)
        }

    @Test
    fun `아이디 매장 조회 실패를 표시하고 재시도하면 같은 아이디를 다시 조회한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "requested-shop")
            val repository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                    error = RamapError.Unknown(IllegalStateException("failed")),
                )
            val viewModel = mapViewModel(ramenShopRepository = repository)

            viewModel.dispatch(OnShopIdSelected(shop.id))
            runCurrent()

            assertEquals(true, viewModel.uiState.value.hasShopDetailLoadFailed)

            repository.error = null
            viewModel.dispatch(OnShopDetailRetry)
            runCurrent()

            assertEquals(
                listOf(setOf(shop.id), setOf(shop.id), setOf(shop.id)),
                repository.requestedShopIdsHistory,
            )
            assertEquals(false, viewModel.uiState.value.hasShopDetailLoadFailed)
            assertEquals(shop, viewModel.uiState.value.selectedShop)
        }

    @Test
    fun `빈 아이디 요청은 진행 중인 유효한 아이디 조회에 영향을 주지 않는다`() =
        coroutinesTest {
            val previousShop = ramenShopFixture(id = "previous-shop")
            val delegate =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(previousShop.id to previousShop)),
                )
            val repository =
                object : RamenShopRepository by delegate {
                    override suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops> {
                        delay(1_000)
                        return delegate.fetchRamenShops(shopIds)
                    }
                }
            val viewModel = mapViewModel(ramenShopRepository = repository)

            viewModel.dispatch(OnShopIdSelected(previousShop.id))
            runCurrent()
            viewModel.dispatch(OnShopIdSelected(""))
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.isShopDetailLoading)
            assertEquals(false, viewModel.uiState.value.hasShopDetailLoadFailed)
            assertEquals(previousShop, viewModel.uiState.value.selectedShop)

            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()
        }

    @Test
    fun `아이디 상세 조회 중 직접 선택하면 이전 workflow를 교체하고 늦은 결과를 무시한다`() =
        coroutinesTest {
            val requestedShop = ramenShopFixture(id = "requested-shop")
            val selectedShop = ramenShopFixture(id = "selected-shop")
            val delegate =
                FakeRamenShopRepository(
                    fetchByIdsResult =
                        RamenShops(
                            mapOf(
                                requestedShop.id to requestedShop,
                                selectedShop.id to selectedShop,
                            ),
                        ),
                )
            val repository =
                object : RamenShopRepository by delegate {
                    override suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops> {
                        if (requestedShop.id in shopIds) {
                            withContext(NonCancellable) { delay(1_000) }
                        }
                        return delegate.fetchRamenShops(shopIds)
                    }
                }
            val viewModel = mapViewModel(ramenShopRepository = repository)

            viewModel.dispatch(OnShopIdSelected(requestedShop.id))
            runCurrent()
            assertEquals(true, viewModel.uiState.value.isShopDetailLoading)

            viewModel.dispatch(OnShopSelected(selectedShop))
            runCurrent()

            assertEquals(selectedShop, viewModel.uiState.value.selectedShop)
            assertEquals(
                selectedShop,
                viewModel.uiState.value
                    .shopDetail
                    ?.shop,
            )

            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(selectedShop, viewModel.uiState.value.selectedShop)
            assertEquals(
                selectedShop,
                viewModel.uiState.value
                    .shopDetail
                    ?.shop,
            )
            assertEquals(false, viewModel.uiState.value.hasShopDetailLoadFailed)
            assertEquals(false, viewModel.uiState.value.isShopDetailLoading)
        }

    @Test
    fun `아이디 상세 조회 중 닫으면 workflow와 로딩을 취소한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "requested-shop")
            val delegate =
                FakeRamenShopRepository(fetchByIdsResult = RamenShops(mapOf(shop.id to shop)))
            val repository =
                object : RamenShopRepository by delegate {
                    override suspend fun fetchRamenShops(shopIds: Set<String>): RamapResult<RamenShops> {
                        delay(1_000)
                        return delegate.fetchRamenShops(shopIds)
                    }
                }
            val viewModel = mapViewModel(ramenShopRepository = repository)

            viewModel.dispatch(OnShopIdSelected(shop.id))
            runCurrent()
            assertEquals(true, viewModel.uiState.value.isShopDetailLoading)

            viewModel.dispatch(OnRequestedShopDismissed)
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.isShopDetailLoading)
            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(null, viewModel.uiState.value.shopDetail)
            assertEquals(false, viewModel.uiState.value.hasShopDetailLoadFailed)
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

            assertEquals(
                listOf(BOUNDS_FIXTURE.expandBy(0.5)),
                ramenShopRepository.requestedBoundsHistory,
            )
            assertEquals(emptyList(), waitingSystemRepository.requestedShopIds)
        }

    @Test
    fun `비로그인 상태에서도 매장 정보 제보를 제출한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "shop-1", name = "라멘집")
            val reportRepository = FakeShopReportRepository()
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult =
                                RamenShops(
                                    mapOf(shop.id to shop),
                                ),
                        ),
                    shopReportRepository = reportRepository,
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    OnShopReportSubmitted(
                        wrongFields =
                            setOf(
                                ShopInformationField.ADDRESS,
                                ShopInformationField.OTHER,
                            ),
                        description = " 주소가 달라요 ",
                    ),
                )
                runCurrent()

                assertEquals(
                    listOf(
                        ShopInformationReport(
                            shopId = "shop-1",
                            shopName = "라멘집",
                            wrongFields =
                                setOf(
                                    ShopInformationField.ADDRESS,
                                    ShopInformationField.OTHER,
                                ),
                            description = "주소가 달라요",
                        ),
                    ),
                    reportRepository.reports,
                )
                assertEquals(
                    showToastSideEffect(Res.string.shop_information_report_success_message),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `매장 정보 제보 실패시 실패 토스트를 표시한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult =
                                RamenShops(
                                    mapOf(shop.id to shop),
                                ),
                        ),
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
                        wrongFields = setOf(ShopInformationField.ADDRESS),
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
    fun `좋아요 토글 인텐트는 북마크 보기와 전체 보기를 전환한다`() =
        coroutinesTest {
            val viewModel = mapViewModel(loginRepository = loggedInRepository())
            runCurrent()

            viewModel.dispatch(OnBookmarkedShopsToggled)
            runCurrent()

            assertEquals(true, viewModel.uiState.value.isBookmarkedView)

            viewModel.dispatch(OnBookmarkedShopsToggled)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.isBookmarkedView)
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
    fun `첫 현재 위치 수신시 초기 위치 포커스를 한 번 요청한다`() =
        coroutinesTest {
            val viewModel = mapViewModel()
            val location = Location(lat = 37.275, lng = 127.009)

            assertEquals(
                LocationFocusStatus.AwaitingLocationStatus,
                viewModel.uiState.value.locationFocusStatus,
            )
            assertEquals(true, viewModel.uiState.value.shouldBootstrapLocationFocusStatus)

            viewModel.dispatch(OnMyLocationChanged(location))
            runCurrent()

            assertEquals(location, viewModel.uiState.value.currentLocation)
            assertEquals(location, viewModel.uiState.value.initialFocusLocation)
            assertEquals(
                LocationFocusStatus.Pending(location),
                viewModel.uiState.value.locationFocusStatus,
            )
            assertEquals(false, viewModel.uiState.value.shouldBootstrapLocationFocusStatus)
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
            assertEquals(
                LocationFocusStatus.Consumed,
                viewModel.uiState.value.locationFocusStatus,
            )
            assertEquals(false, viewModel.uiState.value.shouldBootstrapLocationFocusStatus)
        }

    @Test
    fun `검색어가 변경되면 즉시 정규화한 검색어로 가게를 검색한다`() =
        coroutinesTest {
            val shops = RamenShops(listOf(ramenShopFixture()).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(searchResult = shops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnQueryChanged("  RAMEN   SHOP  "))
            runCurrent()

            assertEquals(
                listOf(SearchQuery("ramen shop")),
                ramenShopRepository.requestedSearchQueries,
            )
            assertEquals(listOf(50), ramenShopRepository.requestedSearchLimits)
            assertEquals(shops, viewModel.uiState.value.search.results)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
            assertEquals(shops, viewModel.uiState.value.markerShops)
            assertEquals(shops, viewModel.uiState.value.focusShops)
        }

    @Test
    fun `검색 결과가 로드되면 검색 결과 매장만 마커로 보여준다`() =
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
            assertEquals(searchShops, viewModel.uiState.value.markerShops)
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
            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.focusShops)
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
                assertEquals(
                    SearchResultGuide.HiddenOnly,
                    viewModel.uiState.value.searchResultGuide,
                )
                assertEquals(searchShops, viewModel.uiState.value.search.results)
                assertEquals(searchShops, viewModel.uiState.value.markerShops)
                assertEquals(RamenShops(listOf(hiddenShop)), viewModel.uiState.value.focusShops)
                assertEquals(emptyList(), waitingSystemRepository.requestedShopIds)
                assertEquals(
                    showToastSideEffect(Res.string.hidden_shop_search_result_message),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `여러 검색 결과에 숨김 매장이 포함되면 안내 토스트와 결과 목록을 보여준다`() =
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
                assertEquals(
                    RamenShops(listOf(visibleShop, hiddenShop)),
                    viewModel.uiState.value.searchResultShops,
                )
                assertEquals(searchShops, viewModel.uiState.value.markerShops)
                assertEquals(
                    showToastSideEffect(Res.string.search_result_hidden_only_message),
                    awaitItem(),
                )
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

                assertEquals("", viewModel.uiState.value.search.input)
                assertEquals(null, viewModel.uiState.value.searchResultGuide)
                assertEquals(
                    showToastSideEffect(Res.string.search_result_empty_message),
                    awaitItem(),
                )
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
                shopDetailState = ShopDetailSheetUiState.Loading(hiddenShop.id, hiddenShop),
                hiddenShopIds = setOf(hiddenShop.id),
                isBookmarkedView = true,
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
                    loadedSearchUiModel(
                        input = "사용자 숨김",
                        results = RamenShops(mapOf(hiddenShop.id to hiddenShop)),
                    ),
                hiddenShopIds = setOf(hiddenShop.id),
            )

        assertEquals(RamenShops(listOf(displayShop)), uiState.searchResultShops)
        assertEquals(RamenShops(mapOf(displayShop.id to displayShop)), uiState.markerShops)
        assertEquals(SearchResultGuide.HiddenOnly, uiState.searchResultGuide)
        assertEquals(RamenShops(listOf(displayShop)), uiState.focusShops)
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
                    loadedSearchUiModel(
                        input = "숨김 해제",
                        results = RamenShops(mapOf(shop.id to shop)),
                    ),
                hiddenShopIds = emptySet(),
            )

        assertEquals(RamenShops(listOf(shop)), uiState.searchResultShops)
        assertEquals(RamenShops(mapOf(shop.id to shop)), uiState.markerShops)
    }

    @Test
    fun `검색 결과가 도착하면 기존 지도 영역 매장 대신 검색 결과만 마커로 보여준다`() =
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
            runCurrent()

            assertEquals(searchShops, viewModel.uiState.value.markerShops)
        }

    @Test
    fun `검색어가 연속으로 변경되면 마지막 검색어만 검색한다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository()
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(OnQueryChanged("라멘"))
            viewModel.dispatch(OnQueryChanged("라멘집"))
            runCurrent()

            assertEquals(listOf(SearchQuery("라멘집")), ramenShopRepository.requestedSearchQueries)
        }

    @Test
    fun `검색 중에는 로딩 상태를 표시하고 검색이 끝나면 해제한다`() =
        coroutinesTest {
            val viewModel =
                mapViewModel(
                    ramenShopRepository = FakeRamenShopRepository(searchDelayMillis = 1_000),
                )

            viewModel.dispatch(OnQueryChanged("라멘"))
            runCurrent()

            assertEquals(true, viewModel.uiState.value.isSearchLoading)
            assertEquals(
                true,
                viewModel.uiState.value.loadState
                    .isLoading(MapLoadKey.Search),
            )

            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.isSearchLoading)
            assertEquals(
                false,
                viewModel.uiState.value.loadState
                    .isLoading(MapLoadKey.Search),
            )
        }

    @Test
    fun `검색 중 새 검색어를 입력하면 이전 결과를 무시하고 검색 로딩을 유지한다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository(searchDelayMillis = 1_000)
            val viewModel = mapViewModel(ramenShopRepository = ramenShopRepository)

            viewModel.dispatch(OnQueryChanged("이전 검색"))
            runCurrent()
            advanceTimeBy(500)

            viewModel.dispatch(OnQueryChanged("새 검색"))
            runCurrent()
            advanceTimeBy(500)
            runCurrent()

            assertEquals("새 검색", viewModel.uiState.value.search.input)
            assertEquals(true, viewModel.uiState.value.isSearchLoading)

            advanceTimeBy(500)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.isSearchLoading)
            assertEquals(
                listOf(SearchQuery("이전 검색"), SearchQuery("새 검색")),
                ramenShopRepository.requestedSearchQueries,
            )
        }

    @Test
    fun `단일 검색 결과 상세를 닫아도 검색어와 검색 결과를 유지한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "single-search-shop")
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            searchResult = RamenShops(mapOf(shop.id to shop)),
                        ),
                )

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(true, viewModel.uiState.value.showBottomSheet)

            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()

            assertEquals("라멘", viewModel.uiState.value.search.input)
            assertEquals(RamenShops(mapOf(shop.id to shop)), viewModel.uiState.value.search.results)
            assertEquals(true, viewModel.uiState.value.search.isResultsDismissed)
            assertEquals(false, viewModel.uiState.value.showBottomSheet)
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
            assertEquals(
                RamenShops(listOf(hiddenDisplayShop)),
                viewModel.uiState.value.searchResultShops,
            )
            assertEquals(
                RamenShops(mapOf(shop.id to hiddenDisplayShop)),
                viewModel.uiState.value.markerShops,
            )
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.focusShops)
            assertEquals(false, viewModel.uiState.value.showBottomSheet)
            assertEquals(SearchResultGuide.HiddenOnly, viewModel.uiState.value.searchResultGuide)
        }

    @Test
    fun `선택한 숨김 매장을 해제하면 선택 상태의 가시성을 복구한다`() =
        coroutinesTest {
            val shop =
                ramenShopFixture(
                    id = "selected-hidden-shop",
                    isVisible = false,
                )
            val personalizationStore = FakePersonalizationRepository()
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        ),
                    personalizationRepository = personalizationStore,
                    loginRepository = loggedInRepository(),
                )
            runCurrent()

            personalizationStore.hideShop(shop.id)
            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            personalizationStore.unhideShop(shop.id)
            runCurrent()

            assertEquals(shop.copy(isVisible = true), viewModel.uiState.value.selectedShop)
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
    fun `알림 구독 매장을 숨기면 구독을 해제한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "subscribed-shop-to-hide")
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(notificationShopIds = setOf(shop.id)),
                )
            val viewModel =
                mapViewModel(
                    loginRepository = loggedInRepository(),
                    personalizationRepository = personalizationRepository,
                )
            runCurrent()

            viewModel.dispatch(OnHiddenToggled(shop))
            runCurrent()

            assertEquals(setOf(shop.id), viewModel.uiState.value.hiddenShopIds)
            assertEquals(emptySet(), viewModel.uiState.value.notificationShopIds)
            assertEquals(emptySet(), personalization(personalizationRepository).notificationShopIds)
        }

    @Test
    fun `숨김 매장의 알림 설정 변경을 차단한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "hidden-notification-shop")
            val notificationRepository = FakeNotificationSettingsRepository()
            val viewModel =
                mapViewModel(
                    personalizationRepository =
                        FakePersonalizationRepository(
                            ShopPersonalization(hiddenShopIds = setOf(shop.id)),
                        ),
                    loginRepository = loggedInRepository(),
                    notificationSettingsRepository = notificationRepository,
                )
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnShopNotificationToggled(shop))
                runCurrent()

                assertEquals(emptySet(), viewModel.uiState.value.notificationShopIds)
                assertEquals(emptySet(), notificationRepository.shopIds)
                assertEquals(
                    showToastSideEffect(Res.string.hidden_shop_notification_unavailable_message),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `매장 알림을 활성화하면 저장한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "permission-granted-shop")
            val personalizationRepository = FakePersonalizationRepository()
            val viewModel =
                mapViewModel(
                    loginRepository = loggedInRepository(),
                    personalizationRepository = personalizationRepository,
                )
            runCurrent()

            viewModel.dispatch(OnShopNotificationToggled(shop))
            runCurrent()

            assertEquals(setOf(shop.id), viewModel.uiState.value.notificationShopIds)
            assertEquals(setOf(shop.id), personalization(personalizationRepository).notificationShopIds)
        }

    @Test
    fun `매장 알림을 비활성화하면 저장한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "notification-disable-shop")
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(notificationShopIds = setOf(shop.id)),
                )
            val viewModel =
                mapViewModel(
                    loginRepository = loggedInRepository(),
                    personalizationRepository = personalizationRepository,
                )
            runCurrent()

            viewModel.dispatch(OnShopNotificationToggled(shop))
            runCurrent()

            assertEquals(emptySet(), viewModel.uiState.value.notificationShopIds)
            assertEquals(emptySet(), personalization(personalizationRepository).notificationShopIds)
        }

    @Test
    fun `다른 화면에서 매장 알림을 해제하면 지도 알림 상태도 갱신한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "notification-disabled-externally")
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(notificationShopIds = setOf(shop.id)),
                )
            val viewModel =
                mapViewModel(
                    loginRepository = loggedInRepository(),
                    personalizationRepository = personalizationRepository,
                )
            runCurrent()
            assertEquals(setOf(shop.id), viewModel.uiState.value.notificationShopIds)

            personalizationRepository.updateShopNotification(shop.id, false)
            runCurrent()

            assertEquals(emptySet(), viewModel.uiState.value.notificationShopIds)
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
                    ShopPersonalization(hiddenShopIds = setOf(hiddenShop.id)),
                )
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult =
                                RamenShops(
                                    mapOf(
                                        hiddenShop.id to
                                            hiddenShop.copy(
                                                isVisible = true,
                                            ),
                                    ),
                                ),
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
            assertEquals(RamenShops(listOf(hiddenShop)), viewModel.uiState.value.focusShops)
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
                assertEquals(
                    showToastSideEffect(Res.string.hidden_shop_search_result_message),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `검색 결과 마커를 선택하면 선택한 매장만 포커스하고 근거리 자동 이동을 끈다`() {
        val selectedShop = ramenShopFixture(id = "oreno-lotte-world-mall")
        val otherShop = ramenShopFixture(id = "oreno-gangnam")
        val uiState =
            MapUiState(
                search =
                    loadedSearchUiModel(
                        input = "오레노",
                        results = RamenShops(listOf(selectedShop, otherShop).associateBy { it.id }),
                    ),
                shopDetailState = ShopDetailSheetUiState.Loading(selectedShop.id, selectedShop),
            )

        assertEquals(RamenShops(listOf(selectedShop)), uiState.focusShops)
        assertEquals(false, uiState.shouldFocusNearestSearchResult)
    }

    @Test
    fun `지도 마커를 선택하면 상세를 열고 매장으로 포커스하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "marker-shop")
            val viewModel = mapViewModel()

            viewModel.dispatch(OnShopSelected(shop, shouldFocus = false))
            runCurrent()

            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.focusShops)
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

            assertEquals(searchShops, viewModel.uiState.value.focusShops)
            assertEquals(true, viewModel.uiState.value.shouldFocusNearestSearchResult)

            viewModel.dispatch(OnShopSelected(selectedShop))
            runCurrent()

            assertEquals(true, viewModel.uiState.value.search.isResultFocusConsumed)
            assertEquals(RamenShops(listOf(selectedShop)), viewModel.uiState.value.focusShops)
            assertEquals(false, viewModel.uiState.value.shouldFocusNearestSearchResult)

            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(true, viewModel.uiState.value.showSearchResults)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.focusShops)
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
                    loadedSearchUiModel(
                        input = "오레노",
                        results = searchShops,
                    ),
            )

        assertEquals(searchShops, uiState.focusShops)
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
                    loadedSearchUiModel(
                        input = "오레노",
                        results = RamenShops(listOf(farShop, nearShop).associateBy { it.id }),
                    ),
                currentLocation = Location(lat = 37.55, lng = 126.92),
            )

        assertEquals(RamenShops(listOf(nearShop, farShop)), uiState.searchResultShops)
        assertEquals(RamenShops(listOf(nearShop, farShop)), uiState.focusShops)
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
    fun `두 글자 미만 검색어로 바꾸면 이전 결과를 지우고 새 검색을 요청하지 않는다`() =
        coroutinesTest {
            val searchShop = ramenShopFixture(id = "search-shop")
            val ramenShopRepository =
                FakeRamenShopRepository(
                    searchResult = RamenShops(listOf(searchShop)),
                )
            val viewModel = mapViewModel(ramenShopRepository)
            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()
            assertEquals(RamenShops(listOf(searchShop)), viewModel.uiState.value.search.results)

            viewModel.dispatch(OnQueryChanged("라"))
            runCurrent()

            assertEquals("라", viewModel.uiState.value.search.input)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.search.results)
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
    fun `영업시간 필터와 맞지 않는 선택 매장은 닫는다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "regular-shop")
            val viewModel = mapViewModel()

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            viewModel.dispatch(OnOpenFilterToggled)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.selectedShop)

            viewModel.dispatch(OnOpenFilterToggled)
            runCurrent()
        }

    @Test
    fun `검색어를 입력하면 카테고리 영업중 좋아요 필터를 해제한다`() =
        coroutinesTest {
            val viewModel = mapViewModel(loginRepository = loggedInRepository())
            runCurrent()
            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            viewModel.dispatch(OnOpenFilterToggled)
            viewModel.dispatch(OnBookmarkedShopsToggled)
            runCurrent()

            viewModel.dispatch(OnQueryChanged("라멘"))
            runCurrent()

            assertEquals(RamenShopFilter(), viewModel.uiState.value.filters)
            assertEquals(false, viewModel.uiState.value.isBookmarkedView)
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

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()
            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            runCurrent()

            assertEquals(
                RamenShops(listOf(mazesobaShop)),
                viewModel.uiState.value.searchResultShops,
            )
            assertEquals(
                RamenShops(mapOf(mazesobaShop.id to mazesobaShop)),
                viewModel.uiState.value.markerShops,
            )
        }

    @Test
    fun `검색 결과도 영업중 필터가 적용된 목록을 보여준다`() {
        val openHours =
            BusinessHours(
                weekly =
                    listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
                        .associateWith {
                            BusinessHoursDay(
                                closed = false,
                                open = "00:00",
                                close = "23:59:59",
                                closeNextDay = false,
                                label = null,
                            )
                        },
                breakTimes = emptyMap(),
                lastOrders = emptyMap(),
                notice = null,
            )
        val openShop = ramenShopFixture(id = "open-search-shop").copy(businessHoursDetails = openHours)
        val anotherOpenShop =
            ramenShopFixture(id = "another-open-search-shop").copy(businessHoursDetails = openHours)
        val closedShop = ramenShopFixture(id = "closed-search-shop")
        val openShops = RamenShops(listOf(openShop, anotherOpenShop))
        val uiState =
            MapUiState(
                search =
                    loadedSearchUiModel(
                        input = "라멘",
                        results = RamenShops(listOf(openShop, anotherOpenShop, closedShop)),
                    ),
                filters = RamenShopFilter(isOpenSelected = true),
            )

        assertEquals(openShops, uiState.searchResultShops)
        assertEquals(openShops, uiState.markerShops)
        assertEquals(true, uiState.showSearchResults)
    }

    @Test
    fun `정기 휴무로 선택된 매장은 영업중 필터의 마커에 포함하지 않는다`() {
        val closedShop =
            ramenShopFixture(id = "regularly-closed-shop").copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly =
                            listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun").associateWith {
                                BusinessHoursDay(true, null, null, false, "정기휴무")
                            },
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )
        val uiState =
            MapUiState(
                shopDetailState = ShopDetailSheetUiState.Loading(closedShop.id, closedShop),
                filters = RamenShopFilter(isOpenSelected = true),
            )

        assertTrue(uiState.markerShops.isEmpty())
    }

    @Test
    fun `검색 결과가 없으면 안내 바텀시트를 보여주지 않는다`() {
        val uiState =
            MapUiState(
                search =
                    loadedSearchUiModel(
                        input = "없는매장",
                        results = RamenShops(emptyMap()),
                    ),
            )

        assertEquals(SearchResultGuide.SearchEmpty, uiState.searchResultGuide)
        assertEquals(false, uiState.showSearchResults)
        assertEquals(false, uiState.showBottomSheet)
    }

    @Test
    fun `북마크 보기에서 검색 결과 목록과 마커에 북마크 필터를 적용한다`() {
        val bookmarkedShop = ramenShopFixture(id = "bookmarked-search-shop")
        val anotherBookmarkedShop = ramenShopFixture(id = "another-bookmarked-search-shop")
        val unbookmarkedShop = ramenShopFixture(id = "unbookmarked-search-shop")
        val uiState =
            MapUiState(
                search =
                    loadedSearchUiModel(
                        input = "북마크",
                        results =
                            RamenShops(
                                listOf(bookmarkedShop, anotherBookmarkedShop, unbookmarkedShop)
                                    .associateBy { it.id },
                            ),
                    ),
                bookmarkedShopIds = setOf(bookmarkedShop.id, anotherBookmarkedShop.id),
                isBookmarkedView = true,
            )

        val bookmarkedShops = RamenShops(listOf(bookmarkedShop, anotherBookmarkedShop))
        assertEquals(bookmarkedShops, uiState.searchResultShops)
        assertEquals(bookmarkedShops, uiState.markerShops)
        assertEquals(bookmarkedShops, uiState.focusShops)
        assertEquals(true, uiState.showSearchResults)
    }

    @Test
    fun `검색 결과 매장을 숨기면 전체 보기 검색 결과에서 투명 표시 대상으로 유지한다`() {
        val hiddenShop = ramenShopFixture(id = "hidden-after-search-shop")
        val visibleShop = ramenShopFixture(id = "visible-search-shop")
        val displayHiddenShop = hiddenShop.copy(isVisible = false)
        val uiState =
            MapUiState(
                search =
                    loadedSearchUiModel(
                        input = "라멘",
                        results =
                            RamenShops(
                                listOf(hiddenShop, visibleShop).associateBy { it.id },
                            ),
                    ),
                hiddenShopIds = setOf(hiddenShop.id),
            )

        assertEquals(RamenShops(listOf(displayHiddenShop, visibleShop)), uiState.searchResultShops)
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
                    loadedSearchUiModel(
                        input = "오레노",
                        results =
                            RamenShops(
                                listOf(selectedShop, otherShop).associateBy { it.id },
                            ),
                        isResultFocusConsumed = true,
                    ),
                filters = RamenShopFilter(setOf(Category.MAZESOBA)),
            )

        assertEquals(RamenShops(listOf(selectedShop, otherShop)), uiState.searchResultShops)
        assertEquals(RamenShops(emptyMap()), uiState.focusShops)
        assertEquals(false, uiState.shouldFocusNearestSearchResult)
    }

    @Test
    fun `새 검색 결과가 로드되기 전에는 이전 검색 결과를 표시하지 않는다`() {
        val previousResults =
            RamenShops(
                listOf(
                    ramenShopFixture(id = "previous-shop-1"),
                    ramenShopFixture(id = "previous-shop-2"),
                ),
            )
        val uiState =
            MapUiState(
                search = loadedSearchUiModel(input = "이전검색", results = previousResults).updateInput("새검색"),
            )

        assertEquals(RamenShops(emptyMap()), uiState.searchResultShops)
        assertEquals(RamenShops(emptyMap()), uiState.focusShops)
        assertEquals(false, uiState.showSearchResults)
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
    fun `매장 상세에서 북마크를 저장하거나 해제하면 좋아요 수를 갱신한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                            shopLikeCount = 3L,
                        ),
                    loginRepository = loggedInRepository(),
                )

            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            assertEquals(
                3L,
                viewModel.uiState.value
                    .shopDetail
                    ?.likeCount,
            )

            viewModel.dispatch(OnBookmarkToggled(shop))
            runCurrent()
            assertEquals(
                4L,
                viewModel.uiState.value
                    .shopDetail
                    ?.likeCount,
            )

            viewModel.dispatch(OnBookmarkToggled(shop))
            runCurrent()
            assertEquals(
                3L,
                viewModel.uiState.value
                    .shopDetail
                    ?.likeCount,
            )
        }

    @Test
    fun `다른 화면에서 저장을 해제하면 열린 상세와 캐시의 좋아요 수를 갱신한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
                )
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                            shopLikeCount = 3L,
                        ),
                    personalizationRepository = personalizationRepository,
                    loginRepository = loggedInRepository(),
                )

            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            personalizationRepository.updateBookmark(shop.id, enabled = false)
            runCurrent()

            assertEquals(
                2L,
                viewModel.uiState.value.shopDetail
                    ?.likeCount,
            )

            viewModel.dispatch(OnShopDetailDismissed)
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            assertEquals(
                2L,
                viewModel.uiState.value.shopDetail
                    ?.likeCount,
            )
        }

    @Test
    fun `가져오기로 저장하면 열린 상세와 캐시의 좋아요 수를 갱신한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val personalizationRepository = FakePersonalizationRepository()
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                            shopLikeCount = 3L,
                        ),
                    personalizationRepository = personalizationRepository,
                    loginRepository = loggedInRepository(),
                )

            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            personalizationRepository.addBookmarks(setOf(shop.id))
            runCurrent()

            assertEquals(
                4L,
                viewModel.uiState.value.shopDetail
                    ?.likeCount,
            )

            viewModel.dispatch(OnShopDetailDismissed)
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            assertEquals(
                4L,
                viewModel.uiState.value.shopDetail
                    ?.likeCount,
            )
        }

    @Test
    fun `숨김으로 저장이 해제되면 상세 캐시의 좋아요 수를 갱신한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
                )
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                            shopLikeCount = 3L,
                        ),
                    personalizationRepository = personalizationRepository,
                    loginRepository = loggedInRepository(),
                )

            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            personalizationRepository.hideShop(shop.id)
            runCurrent()
            personalizationRepository.unhideShop(shop.id)
            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            assertEquals(
                2L,
                viewModel.uiState.value.shopDetail
                    ?.likeCount,
            )
        }

    @Test
    fun `로그아웃 후 로그인하면 이미 저장된 매장을 다시 저장하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val personalizationRepository = FakePersonalizationRepository()
            val viewModel =
                mapViewModel(
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        ),
                    personalizationRepository = personalizationRepository,
                    loginRepository = loginRepository,
                )

            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()
            viewModel.dispatch(OnBookmarkToggled(shop))
            runCurrent()
            assertEquals(
                1L,
                viewModel.uiState.value
                    .shopDetail
                    ?.likeCount,
            )

            loginRepository.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            personalizationRepository.clear()
            runCurrent()

            viewModel.dispatch(OnShopDetailDismissed)
            runCurrent()
            viewModel.dispatch(OnShopSelected(shop))
            runCurrent()

            viewModel.dispatch(OnBookmarkToggled(shop))
            runCurrent()
            viewModel.dispatch(OnLoginTypeSelected(LoginType.APPLE))
            runCurrent()

            loginRepository.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            personalizationRepository.updateBookmarkedShopIds(setOf(shop.id))
            runCurrent()

            assertEquals(listOf(shop.id to true), personalizationRepository.bookmarkUpdateRequests)
            assertEquals(
                1L,
                viewModel.uiState.value
                    .shopDetail
                    ?.likeCount,
            )
        }

    @Test
    fun `Apple 로그인 성공 후 대기 중인 북마크 요청을 재개한다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val loginRepository = FakeLoginRepository()
            val personalizationRepository = FakePersonalizationRepository()
            val viewModel =
                mapViewModel(
                    loginRepository = loginRepository,
                    personalizationRepository = personalizationRepository,
                )

            viewModel.dispatch(OnBookmarkToggled(shop))
            runCurrent()
            viewModel.dispatch(OnLoginTypeSelected(LoginType.APPLE))
            runCurrent()
            loginRepository.updateSessionState(LoginSessionState.AUTHENTICATED)
            personalizationRepository.updateBookmarkedShopIds(setOf("existing-shop"))
            runCurrent()

            assertEquals(1, loginRepository.signInWithAppleCallCount)
            assertEquals(listOf(shop.id to true), personalizationRepository.bookmarkUpdateRequests)
        }

    @Test
    fun `다른 화면에서 북마크 상태를 변경하면 지도 북마크 상태도 갱신한다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "externally-bookmarked-shop")
            val personalizationRepository = FakePersonalizationRepository()
            val viewModel =
                mapViewModel(
                    personalizationRepository = personalizationRepository,
                    loginRepository = loggedInRepository(),
                )
            runCurrent()

            personalizationRepository.updateBookmarkedShopIds(setOf(shop.id))
            runCurrent()

            assertEquals(setOf(shop.id), viewModel.uiState.value.bookmarkedShopIds)
        }

    @Test
    fun `비로그인 상태에서는 공유 저장소의 북마크 상태를 노출하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "cached-bookmarked-shop")
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(shop.id)),
                )
            val viewModel = mapViewModel(personalizationRepository = personalizationRepository)
            runCurrent()

            assertEquals(emptySet(), viewModel.uiState.value.bookmarkedShopIds)

            personalizationRepository.updateBookmarkedShopIds(setOf(shop.id, "another-shop"))
            runCurrent()

            assertEquals(emptySet(), viewModel.uiState.value.bookmarkedShopIds)
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
    fun `검색 후 필터 적용으로 숨김 검색 결과만 남으면 상세를 열지 않는다`() =
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

            viewModel.dispatch(OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()
            viewModel.dispatch(OnCategoryFilterToggled(Category.MAZESOBA))
            runCurrent()

            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(false, viewModel.uiState.value.showBottomSheet)
            assertEquals(false, viewModel.uiState.value.showSearchResults)
            assertEquals(
                SearchResultGuide.HiddenOnly,
                viewModel.uiState.value.searchResultGuide,
            )
            assertEquals(
                RamenShops(listOf(hiddenShop)),
                viewModel.uiState.value.searchResultShops,
            )
            assertEquals(RamenShops(listOf(hiddenShop)), viewModel.uiState.value.focusShops)
            assertEquals(emptyList(), waitingSystemRepository.requestedShopIds)
        }

    @Test
    fun `매장 검색 결과가 없으면 외부 장소 검색 없이 빈 결과를 표시한다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository()
            val viewModel = mapViewModel(ramenShopRepository = ramenShopRepository)

            viewModel.dispatch(OnQueryChanged("강남구"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(listOf(SearchQuery("강남구")), ramenShopRepository.requestedSearchQueries)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.searchResultShops)
            assertEquals("", viewModel.uiState.value.search.input)
            assertEquals(null, viewModel.uiState.value.searchResultGuide)
            assertEquals(false, viewModel.uiState.value.showSearchResults)
        }
}

private fun loadedSearchUiModel(
    input: String,
    results: RamenShops,
    isResultFocusConsumed: Boolean = false,
): SearchUiModel =
    SearchUiModel(input = input)
        .updateResults(
            query = SearchQuery(input).normalizeShopSearchQuery(),
            results = results,
        ).consumeResultFocus(isResultFocusConsumed)

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
    personalizationRepository: ShopPersonalizationStore = FakePersonalizationRepository(),
    loginRepository: FakeLoginRepository = FakeLoginRepository(),
    shopReportRepository: ShopReportRepository = FakeShopReportRepository(),
    notificationSettingsRepository: FakeNotificationSettingsRepository = FakeNotificationSettingsRepository(),
): MapViewModel =
    MapViewModel(
        ramenShopRepository,
        loginRepository,
        CurrentLocationStore(),
        shopReportRepository,
        personalizationRepository,
        FakeFetchShopDetailUseCase(
            ramenShopRepository,
            shopWaitingSystemRepository,
            FakeOperatingNoticeRepository(),
        ),
        FakeMapSearchHistoryStorage(),
        MapAnalytics(FakeAnalyticsTracker()),
        LoginAnalytics(FakeAnalyticsTracker(), FakeCrashReporter()),
        FakeOperatingNoticeRepository(),
    )

private fun loggedInRepository(): FakeLoginRepository =
    FakeLoginRepository(
        initialSessionState = LoginSessionState.AUTHENTICATED,
        userEmail = "test@ramap.com",
    )

private fun personalization(repository: FakePersonalizationRepository): ShopPersonalization = (repository.state.value as PersonalizationBootstrapState.Success).value
