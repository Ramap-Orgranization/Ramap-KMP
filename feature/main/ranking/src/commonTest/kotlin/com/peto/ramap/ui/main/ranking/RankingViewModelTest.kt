package com.peto.ramap.ui.main.ranking

import app.cash.turbine.test
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.ShopRanking
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AdministrativeDistricts
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.ui.main.ranking.contract.RankingIntent
import com.peto.ramap.ui.main.ranking.contract.RankingSideEffect
import com.peto.ramap.ui.main.ranking.log.RankingAnalytics
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.yield
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message
import ramap.shared.generated.resources.ranking_refresh_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {
    @Test
    fun `수동 새로고침은 첫 페이지를 즉시 적용한다`() =
        coroutinesTest {
            val repository = FakeShopRankingRepository(pageOf(shopRanking("old")))
            val viewModel = rankingViewModel(repository = repository)
            runCurrent()
            repository.page = pageOf(shopRanking("fresh"))

            viewModel.dispatch(RankingIntent.OnRefreshed)
            runCurrent()

            assertEquals(listOf("fresh"), shopIds(viewModel))
        }

    @Test
    fun `공통 상세에서 바뀐 좋아요는 랭킹 표시 수에 한 번만 반영한다`() =
        coroutinesTest {
            val personalizationStore = FakePersonalizationRepository()
            val viewModel =
                rankingViewModel(
                    repository = FakeShopRankingRepository(pageOf(shopRanking(likeCount = 3))),
                    personalizationStore = personalizationStore,
                )
            runCurrent()

            personalizationStore.updateBookmark("shop-id", true)
            runCurrent()

            assertEquals(
                4L,
                viewModel.uiState.value.displayedLikeCount(viewModel.uiState.value.shops[0]),
            )
        }

    @Test
    fun `시도를 선택하면 시군구 옵션을 조회한다`() =
        coroutinesTest {
            val districts =
                AdministrativeDistricts(
                    listOf(
                        AdministrativeDistrict("수원시"),
                        AdministrativeDistrict("용인시"),
                    ),
                )
            val repository =
                FakeShopRankingRepository().apply {
                    this.districts = districts
                }
            val viewModel = rankingViewModel(repository = repository)

            viewModel.dispatch(
                RankingIntent.OnAdministrativeAreaSelected(AdministrativeArea.GYEONGGI),
            )
            runCurrent()

            assertEquals(listOf(AdministrativeArea.GYEONGGI), repository.districtQueries)
            assertEquals(AdministrativeArea.GYEONGGI, viewModel.uiState.value.areaSelectionArea)
            assertEquals(districts, viewModel.uiState.value.administrativeDistricts)
        }

    @Test
    fun `세종을 선택하면 하위 조회 없이 세종 전체 필터를 적용한다`() =
        coroutinesTest {
            val repository = FakeShopRankingRepository()
            val viewModel = rankingViewModel(repository = repository)

            viewModel.dispatch(
                RankingIntent.OnAdministrativeAreaSelected(AdministrativeArea.SEJONG),
            )
            runCurrent()

            assertEquals(emptyList(), repository.districtQueries)
            assertEquals(
                AreaFilter.Province(AdministrativeArea.SEJONG),
                repository.queries.last().areaFilter,
            )
        }

    @Test
    fun `지역 시트를 다시 열면 현재 시군구의 시도를 복원하고 옵션을 조회한다`() =
        coroutinesTest {
            val repository =
                FakeShopRankingRepository().apply {
                    districts =
                        AdministrativeDistricts(
                            listOf(AdministrativeDistrict("수원시")),
                        )
                }
            val viewModel = rankingViewModel(repository = repository)
            val districtFilter =
                AreaFilter.District(
                    AdministrativeArea.GYEONGGI,
                    AdministrativeDistrict("수원시"),
                )

            viewModel.dispatch(RankingIntent.OnAreaFilterSelected(districtFilter))
            runCurrent()
            viewModel.dispatch(RankingIntent.OnAreaSelectionBack)
            runCurrent()
            viewModel.dispatch(RankingIntent.OnAreaSheetOpened)
            runCurrent()

            assertEquals(AdministrativeArea.GYEONGGI, viewModel.uiState.value.areaSelectionArea)
            assertEquals(repository.districts, viewModel.uiState.value.administrativeDistricts)
            assertEquals(districtFilter, viewModel.uiState.value.areaFilter)
            assertEquals(listOf(AdministrativeArea.GYEONGGI), repository.districtQueries)
        }

    @Test
    fun `숨긴 매장 상태와 무관하게 서버 랭킹을 표시한다`() =
        coroutinesTest {
            val repository =
                FakeShopRankingRepository(
                    page = pageOf(shopRanking()),
                )
            val personalizationStore =
                FakePersonalizationRepository(
                    ShopPersonalization(
                        hiddenShopIds = setOf("shop-id"),
                    ),
                )
            val viewModel =
                rankingViewModel(
                    repository = repository,
                    personalizationStore = personalizationStore,
                )

            runCurrent()

            assertEquals(
                listOf("shop-id"),
                shopIds(viewModel),
            )
        }

    @Test
    fun `지역과 카테고리 변경은 커서 없이 첫 페이지를 다시 요청한다`() =
        coroutinesTest {
            val repository = FakeShopRankingRepository()
            val viewModel =
                rankingViewModel(
                    repository = repository,
                )

            viewModel.dispatch(
                RankingIntent.OnAreaFilterSelected(
                    AreaFilter.Province(AdministrativeArea.SEOUL),
                ),
            )
            runCurrent()

            viewModel.dispatch(
                RankingIntent.OnCategoryToggled(Category.MISO),
            )
            runCurrent()

            val query = repository.queries.last()

            assertEquals(
                AreaFilter.Province(AdministrativeArea.SEOUL),
                query.areaFilter,
            )
            assertEquals(
                setOf(Category.MISO),
                query.categories,
            )
            assertEquals(
                null,
                query.cursor,
            )
        }

    @Test
    fun `다음 페이지는 기존 ID를 유지하고 신규 중복을 제외해 dense rank를 잇는다`() =
        coroutinesTest {
            val cursor = RankingCursor(3, "나", "second")
            val repository =
                FakeShopRankingRepository(
                    RankingPage(
                        items =
                            ShopRankings(
                                listOf(
                                    shopRanking("first", 5),
                                    shopRanking("second", 3),
                                ),
                            ),
                        nextCursor = cursor,
                    ),
                )
            val viewModel =
                rankingViewModel(
                    repository = repository,
                )

            runCurrent()

            repository.page =
                RankingPage(
                    items =
                        ShopRankings(
                            listOf(
                                shopRanking("second", 3),
                                shopRanking("third", 3),
                                shopRanking("fourth", 1),
                            ),
                        ),
                    nextCursor = null,
                )

            viewModel.dispatch(RankingIntent.OnNextPageRequested)
            runCurrent()

            assertEquals(
                listOf("first", "second", "third", "fourth"),
                shopIds(viewModel),
            )
            assertEquals(
                listOf(1, 2, 2, 3),
                viewModel.uiState.value.shops.map { item ->
                    item.rank
                },
            )
            assertEquals(
                cursor,
                repository.queries.last().cursor,
            )
        }

    @Test
    fun `다음 페이지 실패는 기존 목록과 커서를 유지하고 재시도 상태를 표시한다`() =
        coroutinesTest {
            val cursor = RankingCursor(3, "매장", "shop-id")
            val repository =
                FakeShopRankingRepository(
                    RankingPage(
                        items = ShopRankings(listOf(shopRanking())),
                        nextCursor = cursor,
                    ),
                )
            val viewModel =
                rankingViewModel(
                    repository = repository,
                )

            runCurrent()

            repository.error =
                RamapError.Unknown(
                    IllegalStateException("failure"),
                )

            viewModel.dispatch(RankingIntent.OnNextPageRequested)
            runCurrent()

            assertEquals(
                listOf("shop-id"),
                shopIds(viewModel),
            )
            assertEquals(
                cursor,
                viewModel.uiState.value.nextCursor,
            )
            assertTrue(
                viewModel.uiState.value.showNextPageError,
            )
        }

    @Test
    fun `새로고침 실패는 기존 목록과 커서를 유지하고 오류 토스트를 보낸다`() =
        coroutinesTest {
            val cursor = RankingCursor(3, "매장", "shop-id")
            val repository =
                FakeShopRankingRepository(
                    RankingPage(
                        items = ShopRankings(listOf(shopRanking())),
                        nextCursor = cursor,
                    ),
                )
            val viewModel =
                rankingViewModel(
                    repository = repository,
                )

            runCurrent()

            repository.error =
                RamapError.Unknown(
                    IllegalStateException("failure"),
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(RankingIntent.OnRefreshed)
                runCurrent()

                assertEquals(
                    listOf("shop-id"),
                    shopIds(viewModel),
                )
                assertEquals(
                    cursor,
                    viewModel.uiState.value.nextCursor,
                )
                assertEquals(
                    RankingSideEffect.ShowToast(
                        ToastData(
                            Res.string.ranking_refresh_failure_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `좋아요 변경은 표시 수만 갱신하고 순서와 rank를 유지한다`() =
        coroutinesTest {
            val secondShop = ramenShop(id = "second")
            val repository =
                FakeShopRankingRepository(
                    pageOf(
                        shopRanking("first", 3),
                        ShopRanking(
                            shop = secondShop,
                            likeCount = 2,
                        ),
                    ),
                )
            val personalizationStore = FakePersonalizationRepository()
            val viewModel =
                rankingViewModel(
                    repository = repository,
                    personalizationStore = personalizationStore,
                )

            runCurrent()

            viewModel.dispatch(
                RankingIntent.OnBookmarkChanged(
                    shop = secondShop,
                    enabled = true,
                ),
            )
            runCurrent()

            assertEquals(
                listOf("first", "second"),
                shopIds(viewModel),
            )
            assertEquals(
                listOf(1, 2),
                viewModel.uiState.value.shops.map { item ->
                    item.rank
                },
            )

            assertEquals(
                3L,
                viewModel.uiState.value.displayedLikeCount(
                    viewModel.uiState.value.shops[1],
                ),
            )
            assertEquals(
                1,
                repository.queries.size,
            )
            assertEquals(
                listOf("second" to true),
                personalizationStore.bookmarkUpdateRequests,
            )
        }

    @Test
    fun `좋아요 저장 실패는 표시 수를 원복한다`() =
        coroutinesTest {
            val shop = ramenShop()
            val personalizationStore =
                FakePersonalizationRepository().apply {
                    bookmarkUpdateError =
                        RamapError.Unknown(
                            IllegalStateException("failure"),
                        )
                }
            val viewModel =
                rankingViewModel(
                    personalizationStore = personalizationStore,
                )

            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    RankingIntent.OnBookmarkChanged(
                        shop = shop,
                        enabled = true,
                    ),
                )
                runCurrent()

                assertEquals(
                    RankingSideEffect.ShowToast(
                        ToastData(
                            Res.string.personalization_update_failure_message,
                            ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
                assertFalse(
                    viewModel.uiState.value.bookmarkLikeCountDeltas
                        .containsKey(shop.id),
                )
                assertFalse(
                    shop.id in viewModel.uiState.value.bookmarkUpdatingShopIds,
                )
            }
        }

    @Test
    fun `저장소가 낙관적 상태와 롤백을 차례로 발행하면 좋아요 수를 한 번만 원복한다`() =
        coroutinesTest {
            val mutablePersonalization = MutableStateFlow(ShopPersonalization())
            val store =
                object : ShopPersonalizationStore by FakePersonalizationRepository() {
                    override val state = mutablePersonalization.asStateFlow()

                    override suspend fun updateBookmark(
                        shopId: String,
                        enabled: Boolean,
                    ): RamapResult<Unit> {
                        mutablePersonalization.value =
                            ShopPersonalization(bookmarkedShopIds = setOf(shopId))
                        yield()
                        mutablePersonalization.value = ShopPersonalization()
                        return RamapResult.Error(
                            RamapError.Unknown(IllegalStateException("failure")),
                        )
                    }
                }
            val viewModel =
                rankingViewModel(
                    repository = FakeShopRankingRepository(pageOf(shopRanking(likeCount = 3))),
                    personalizationStore = store,
                )
            runCurrent()

            viewModel.dispatch(
                RankingIntent.OnBookmarkChanged(
                    shop = ramenShop(),
                    enabled = true,
                ),
            )
            runCurrent()

            assertEquals(
                3L,
                viewModel.uiState.value
                    .displayedLikeCount(viewModel.uiState.value.shops[0]),
            )
            assertFalse(
                viewModel.uiState.value.bookmarkLikeCountDeltas
                    .containsKey("shop-id"),
            )
        }

    @Test
    fun `북마크 추가 중 빠른 제거 입력은 버튼 잠금 계약에 따라 새 요청을 시작하지 않는다`() =
        coroutinesTest {
            val mutablePersonalization = MutableStateFlow(ShopPersonalization())
            val allowCompletion = CompletableDeferred<Unit>()
            val requests = mutableListOf<Pair<String, Boolean>>()
            val store =
                object : ShopPersonalizationStore by FakePersonalizationRepository() {
                    override val state = mutablePersonalization.asStateFlow()

                    override suspend fun updateBookmark(
                        shopId: String,
                        enabled: Boolean,
                    ): RamapResult<Unit> {
                        requests += shopId to enabled
                        mutablePersonalization.value =
                            ShopPersonalization(bookmarkedShopIds = setOf(shopId))
                        allowCompletion.await()
                        return RamapResult.Success(Unit)
                    }
                }
            val shop = ramenShop()
            val viewModel = rankingViewModel(personalizationStore = store)
            runCurrent()

            viewModel.dispatch(RankingIntent.OnBookmarkChanged(shop, enabled = true))
            runCurrent()
            viewModel.dispatch(RankingIntent.OnBookmarkChanged(shop, enabled = false))
            runCurrent()

            assertEquals(listOf(shop.id to true), requests)
            assertTrue(shop.id in viewModel.uiState.value.bookmarkUpdatingShopIds)

            allowCompletion.complete(Unit)
            runCurrent()

            assertEquals(listOf(shop.id to true), requests)
            assertTrue(shop.id in viewModel.uiState.value.bookmarkedShopIds)
        }

    @Test
    fun `비로그인 사용자의 좋아요 요청은 로그인 안내만 보낸다`() =
        coroutinesTest {
            val shop = ramenShop()
            val personalizationStore = FakePersonalizationRepository()
            val loginRepository =
                FakeLoginRepository(
                    LoginSessionState.NOT_AUTHENTICATED,
                )
            val viewModel =
                rankingViewModel(
                    personalizationStore = personalizationStore,
                    loginRepository = loginRepository,
                )

            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(
                    RankingIntent.OnBookmarkChanged(
                        shop = shop,
                        enabled = true,
                    ),
                )

                assertEquals(
                    RankingSideEffect.ShowLoginGuide,
                    awaitItem(),
                )
                assertEquals(
                    emptyList(),
                    personalizationStore.bookmarkUpdateRequests,
                )
            }
        }

    @Test
    fun `이미 좋아요한 매장에 좋아요 요청을 하면 저장 요청하지 않는다`() =
        coroutinesTest {
            val shop = ramenShop()
            val personalizationStore =
                FakePersonalizationRepository(
                    ShopPersonalization(
                        bookmarkedShopIds = setOf(shop.id),
                    ),
                )
            val viewModel =
                rankingViewModel(
                    personalizationStore = personalizationStore,
                )

            runCurrent()

            viewModel.dispatch(
                RankingIntent.OnBookmarkChanged(
                    shop = shop,
                    enabled = true,
                ),
            )
            runCurrent()

            assertEquals(
                emptyList(),
                personalizationStore.bookmarkUpdateRequests,
            )
        }

    private fun shopIds(viewModel: RankingViewModel): List<String> =
        viewModel.uiState.value.shops.map { item ->
            item.ranking.shop.id
        }

    private fun pageOf(vararg rankings: ShopRanking): RankingPage =
        RankingPage(
            items = ShopRankings(rankings.toList()),
            nextCursor = null,
        )

    private fun shopRanking(
        id: String = "shop-id",
        likeCount: Long = 3,
    ): ShopRanking =
        ShopRanking(
            shop = ramenShop(id),
            likeCount = likeCount,
        )

    private fun ramenShop(id: String = "shop-id"): RamenShop =
        RamenShop(
            id = id,
            kakaoPlaceId = null,
            name = "매장-$id",
            address = "서울특별시 마포구",
            location = Location(37.0, 127.0),
            kakaoPlaceUrl = null,
            phone = null,
            businessHours = null,
            instagramUrl = null,
            kakaoRating = null,
            menuCategories =
                MenuCategories(
                    listOf(Category.SHOYU),
                ),
            isVisible = true,
            createdAt = "",
            updatedAt = "",
        )
}

private fun rankingViewModel(
    repository: FakeShopRankingRepository = FakeShopRankingRepository(),
    personalizationStore: ShopPersonalizationStore =
        FakePersonalizationRepository(),
    loginRepository: LoginRepository =
        FakeLoginRepository(
            LoginSessionState.AUTHENTICATED,
        ),
    analyticsTracker: FakeAnalyticsTracker = FakeAnalyticsTracker(),
): RankingViewModel =
    RankingViewModel(
        shopRankRepository = repository,
        personalizationStore = personalizationStore,
        loginRepository = loginRepository,
        rankingAnalytics =
            RankingAnalytics(
                analyticsTracker,
            ),
        loginAnalytics =
            LoginAnalytics(
                analyticsTracker,
            ),
    )
