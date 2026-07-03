package com.peto.ramap.ui.map

import app.cash.turbine.test
import com.peto.ramap.core.config.DefaultMapConfig
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopWaitingSystemRepository
import com.peto.ramap.fixture.BOUNDS_FIXTURE
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.fixture.waitingSystemFixture
import com.peto.ramap.ui.map.contract.MapIntent
import com.peto.ramap.ui.map.contract.MapSideEffect
import com.peto.ramap.ui.map.contract.MapUiState
import com.peto.ramap.ui.map.model.MapPersonalization
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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

            viewModel.dispatch(MapIntent.OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(200)
            viewModel.dispatch(MapIntent.OnBoundsChanged(lastBounds))
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

            viewModel.dispatch(MapIntent.OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(MapIntent.OnBoundsChanged(nearbyBounds))
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

            viewModel.dispatch(MapIntent.OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(MapIntent.OnBoundsChanged(changedBounds))
            advanceTimeBy(350)
            runCurrent()

            assertEquals(
                listOf(BOUNDS_FIXTURE, changedBounds),
                ramenShopRepository.requestedBoundsHistory,
            )
        }

    @Test
    fun `지도 이동만으로는 클러스터 기준 영역을 변경하지 않는다`() =
        coroutinesTest {
            val viewModel = mapViewModel()
            val pannedBounds =
                DefaultMapConfig.bounds.copy(
                    minLat = DefaultMapConfig.bounds.minLat + 0.01,
                    maxLat = DefaultMapConfig.bounds.maxLat + 0.01,
                    minLng = DefaultMapConfig.bounds.minLng + 0.01,
                    maxLng = DefaultMapConfig.bounds.maxLng + 0.01,
                )

            viewModel.dispatch(MapIntent.OnBoundsChanged(pannedBounds))
            runCurrent()

            assertEquals(pannedBounds, viewModel.uiState.value.bounds)
            assertEquals(DefaultMapConfig.bounds, viewModel.uiState.value.clusterBounds)
            advanceTimeBy(350)
            runCurrent()
        }

    @Test
    fun `줌 변경시에는 클러스터 기준 영역을 변경한다`() =
        coroutinesTest {
            val viewModel = mapViewModel()
            val zoomedBounds =
                DefaultMapConfig.bounds.copy(
                    maxLat = DefaultMapConfig.bounds.minLat + DefaultMapConfig.bounds.latSpan * 0.8,
                    maxLng = DefaultMapConfig.bounds.minLng + DefaultMapConfig.bounds.lngSpan * 0.8,
                )

            viewModel.dispatch(MapIntent.OnBoundsChanged(zoomedBounds))
            runCurrent()

            assertEquals(zoomedBounds, viewModel.uiState.value.clusterBounds)
            advanceTimeBy(350)
            runCurrent()
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

                viewModel.dispatch(MapIntent.OnBoundsChanged(BOUNDS_FIXTURE))
                assertEquals(BOUNDS_FIXTURE, awaitItem().bounds)
                advanceTimeBy(350)
                runCurrent()
                assertEquals(shops, awaitItem().shops)

                viewModel.dispatch(MapIntent.OnBoundsChanged(changedBounds))
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
            val viewModel = mapViewModel(shopWaitingSystemRepository = waitingSystemRepository)

            viewModel.dispatch(MapIntent.OnShopSelected(shop))
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
            val viewModel = mapViewModel(shopWaitingSystemRepository = waitingSystemRepository)

            viewModel.dispatch(MapIntent.OnShopSelected(shop))
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
    fun `이미 조회한 가게를 다시 선택하면 웨이팅 시스템을 중복 조회하지 않는다`() =
        coroutinesTest {
            val shop = ramenShopFixture()
            val waitingSystemRepository =
                FakeShopWaitingSystemRepository(result = waitingSystemFixture(shop.id))
            val viewModel = mapViewModel(shopWaitingSystemRepository = waitingSystemRepository)

            viewModel.dispatch(MapIntent.OnShopSelected(shop))
            runCurrent()
            viewModel.dispatch(MapIntent.OnShopSelected(shop))
            runCurrent()

            assertEquals(listOf(shop.id), waitingSystemRepository.requestedShopIds)
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

            viewModel.dispatch(MapIntent.OnShopSelected(shop))
            runCurrent()
            waitingSystemRepository.requestedShopIds.clear()

            viewModel.dispatch(MapIntent.OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()

            assertEquals(listOf(BOUNDS_FIXTURE), ramenShopRepository.requestedBoundsHistory)
            assertEquals(emptyList(), waitingSystemRepository.requestedShopIds)
        }

    @Test
    fun `검색어가 변경되면 지연 후 정규화한 검색어로 가게를 검색한다`() =
        coroutinesTest {
            val shops = RamenShops(listOf(ramenShopFixture()).associateBy { it.id })
            val ramenShopRepository = FakeRamenShopRepository(searchResult = shops)
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(MapIntent.OnQueryChanged("  RAMEN   SHOP  "))
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
            assertEquals(shops, viewModel.uiState.value.searchResults)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.shops)
            assertEquals(shops, viewModel.uiState.value.markerShops)
            assertEquals(shops.values.toList(), viewModel.uiState.value.focusShops)
        }

    @Test
    fun `검색 중에는 지도 영역 매장이 아닌 검색 결과만 마커로 보여준다`() =
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

            viewModel.dispatch(MapIntent.OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(MapIntent.OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(mapShops, viewModel.uiState.value.shops)
            assertEquals(searchShops, viewModel.uiState.value.searchResults)
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

            viewModel.dispatch(MapIntent.OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(shop, viewModel.uiState.value.selectedShop)
            assertEquals(true, viewModel.uiState.value.showBottomSheet)
            assertEquals(false, viewModel.uiState.value.showSearchResults)
            assertEquals(searchShops, viewModel.uiState.value.searchResults)
            assertEquals(searchShops, viewModel.uiState.value.markerShops)
            assertEquals(listOf(shop), viewModel.uiState.value.focusShops)
            assertEquals(listOf(shop.id), waitingSystemRepository.requestedShopIds)
            assertEquals(waitingSystem, viewModel.uiState.value.shopWaiting[shop.id])
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

            viewModel.dispatch(MapIntent.OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(MapIntent.OnQueryChanged("라멘"))
            advanceTimeBy(299)
            runCurrent()

            assertEquals(mapShops, viewModel.uiState.value.markerShops)

            advanceTimeBy(1)
            runCurrent()

            assertEquals(searchShops, viewModel.uiState.value.markerShops)
        }

    @Test
    fun `검색어가 연속으로 변경되면 마지막 검색어만 검색한다`() =
        coroutinesTest {
            val ramenShopRepository = FakeRamenShopRepository()
            val viewModel = mapViewModel(ramenShopRepository)

            viewModel.dispatch(MapIntent.OnQueryChanged("라멘"))
            advanceTimeBy(150)
            viewModel.dispatch(MapIntent.OnQueryChanged("라멘집"))
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

            viewModel.dispatch(MapIntent.OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(true, viewModel.uiState.value.showSearchResults)
            assertEquals(true, viewModel.uiState.value.showBottomSheet)

            viewModel.dispatch(MapIntent.OnSearchResultsDismissed)
            runCurrent()

            assertEquals("라멘", viewModel.uiState.value.query)
            assertEquals(searchShops, viewModel.uiState.value.searchResults)
            assertEquals(searchShops, viewModel.uiState.value.markerShops)
            assertEquals(false, viewModel.uiState.value.showSearchResults)
            assertEquals(false, viewModel.uiState.value.showBottomSheet)
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

            viewModel.dispatch(MapIntent.OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(MapIntent.OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()
            viewModel.dispatch(MapIntent.OnQueryChanged("   "))
            runCurrent()

            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.searchResults)
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

            viewModel.dispatch(MapIntent.OnBoundsChanged(BOUNDS_FIXTURE))
            advanceTimeBy(350)
            runCurrent()
            viewModel.dispatch(MapIntent.OnCategoryFilterToggled(Category.MAZESOBA))
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

            viewModel.dispatch(MapIntent.OnCategoryFilterToggled(Category.MAZESOBA))
            runCurrent()
            viewModel.dispatch(MapIntent.OnCategoryFilterToggled(Category.MAZESOBA))
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

            viewModel.dispatch(MapIntent.OnCategoryFilterToggled(Category.MAZESOBA))
            viewModel.dispatch(MapIntent.OnCategoryFilterToggled(Category.JIRO))
            runCurrent()
            viewModel.dispatch(MapIntent.OnFilterCleared)
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

            viewModel.dispatch(MapIntent.OnShopSelected(shop))
            runCurrent()
            viewModel.dispatch(MapIntent.OnCategoryFilterToggled(Category.MAZESOBA))
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

            viewModel.dispatch(MapIntent.OnCategoryFilterToggled(Category.MAZESOBA))
            viewModel.dispatch(MapIntent.OnQueryChanged("라멘"))
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

            viewModel.dispatch(MapIntent.OnCategoryFilterToggled(Category.MAZESOBA))
            viewModel.dispatch(MapIntent.OnQueryChanged("라멘"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.selectedShop)
            assertEquals(emptyList(), viewModel.uiState.value.searchResultShops)
        }

    @Test
    fun `숨긴 매장 보기에서는 숨긴 매장만 마커로 보여준다`() {
        val visibleShop = ramenShopFixture(id = "visible-shop")
        val hiddenShop = ramenShopFixture(id = "hidden-shop")
        val uiState =
            MapUiState(
                shops = RamenShops(listOf(visibleShop, hiddenShop).associateBy { it.id }),
                hiddenShopIds = setOf(hiddenShop.id),
                personalizationView = MapPersonalization.HIDDEN,
            )

        assertEquals(RamenShops(mapOf(hiddenShop.id to hiddenShop)), uiState.markerShops)
    }

    @Test
    fun `비로그인 상태에서 북마크를 누르면 로그인 안내를 요청한다`() =
        coroutinesTest {
            val viewModel = mapViewModel()
            val shop = ramenShopFixture()

            viewModel.sideEffect.test {
                viewModel.dispatch(MapIntent.OnBookmarkToggled(shop))
                runCurrent()

                assertEquals(MapSideEffect.ShowLoginGuide, awaitItem())
            }
        }

    @Test
    fun `비로그인 상태에서 숨기기를 누르면 로그인 안내를 요청한다`() =
        coroutinesTest {
            val viewModel = mapViewModel()
            val shop = ramenShopFixture()

            viewModel.sideEffect.test {
                viewModel.dispatch(MapIntent.OnHiddenToggled(shop))
                runCurrent()

                assertEquals(MapSideEffect.ShowLoginGuide, awaitItem())
            }
        }
}

private fun mapViewModel(
    ramenShopRepository: RamenShopRepository = FakeRamenShopRepository(),
    shopWaitingSystemRepository: ShopWaitingSystemRepository = FakeShopWaitingSystemRepository(),
    personalizationRepository: PersonalizationRepository = FakePersonalizationRepository(),
    loginRepository: FakeLoginRepository = FakeLoginRepository(),
): MapViewModel = MapViewModel(ramenShopRepository, shopWaitingSystemRepository, personalizationRepository, loginRepository)
