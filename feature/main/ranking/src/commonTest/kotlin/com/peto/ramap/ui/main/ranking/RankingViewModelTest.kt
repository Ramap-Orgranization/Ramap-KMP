package com.peto.ramap.ui.main.ranking

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
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
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.ui.main.ranking.contract.RankingIntent
import com.peto.ramap.ui.main.ranking.contract.RankingSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
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
    fun `숨긴 매장 상태와 무관하게 서버 랭킹을 표시한다`() =
        coroutinesTest {
            val repository = FakeShopRankingRepository(page = pageOf(shopRanking()))
            val personalizationStore =
                FakePersonalizationRepository(ShopPersonalization(hiddenShopIds = setOf("shop-id")))
            val viewModel = rankingViewModel(repository, personalizationStore)

            runCurrent()

            assertEquals(
                listOf("shop-id"),
                viewModel.uiState.value.shops
                    .map { it.ranking.shop.id },
            )
        }

    @Test
    fun `지역과 카테고리 변경은 커서 없이 첫 페이지를 다시 요청한다`() =
        coroutinesTest {
            val repository = FakeShopRankingRepository()
            val viewModel = rankingViewModel(repository)

            viewModel.dispatch(
                RankingIntent.OnAreaFilterSelected(AreaFilter.Selected(AdministrativeArea.SEOUL)),
            )
            runCurrent()
            viewModel.dispatch(RankingIntent.OnCategoryToggled(Category.MISO))
            runCurrent()

            assertEquals(AdministrativeArea.SEOUL, repository.queries.last().area)
            assertEquals(setOf(Category.MISO), repository.queries.last().categories)
            assertEquals(null, repository.queries.last().cursor)
        }

    @Test
    fun `다음 페이지는 기존 ID를 유지하고 신규 중복을 제외해 dense rank를 잇는다`() =
        coroutinesTest {
            val cursor = RankingCursor(3, "나", "second")
            val repository =
                FakeShopRankingRepository(
                    RankingPage(
                        items = ShopRankings(listOf(shopRanking("first", 5), shopRanking("second", 3))),
                        nextCursor = cursor,
                    ),
                )
            val viewModel = rankingViewModel(repository)
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

            assertEquals(listOf("first", "second", "third", "fourth"), shopIds(viewModel))
            assertEquals(
                listOf(1, 2, 2, 3),
                viewModel.uiState.value.shops
                    .map { it.rank },
            )
            assertEquals(cursor, repository.queries.last().cursor)
        }

    @Test
    fun `다음 페이지 실패는 기존 목록과 커서를 유지하고 재시도 상태를 표시한다`() =
        coroutinesTest {
            val cursor = RankingCursor(3, "매장", "shop-id")
            val repository =
                FakeShopRankingRepository(
                    RankingPage(ShopRankings(listOf(shopRanking())), cursor),
                )
            val viewModel = rankingViewModel(repository)
            runCurrent()
            repository.error = RamapError.Unknown(IllegalStateException("failure"))

            viewModel.dispatch(RankingIntent.OnNextPageRequested)
            runCurrent()

            assertEquals(listOf("shop-id"), shopIds(viewModel))
            assertEquals(cursor, viewModel.uiState.value.nextCursor)
            assertTrue(viewModel.uiState.value.showNextPageError)
        }

    @Test
    fun `새로고침 실패는 기존 목록과 커서를 유지하고 오류 토스트를 보낸다`() =
        coroutinesTest {
            val cursor = RankingCursor(3, "매장", "shop-id")
            val repository =
                FakeShopRankingRepository(
                    RankingPage(ShopRankings(listOf(shopRanking())), cursor),
                )
            val viewModel = rankingViewModel(repository)
            runCurrent()
            repository.error = RamapError.Unknown(IllegalStateException("failure"))

            viewModel.sideEffect.test {
                viewModel.dispatch(RankingIntent.OnRefreshed)
                runCurrent()

                assertEquals(listOf("shop-id"), shopIds(viewModel))
                assertEquals(cursor, viewModel.uiState.value.nextCursor)
                assertEquals(
                    RankingSideEffect.ShowToast(
                        ToastData(Res.string.ranking_refresh_failure_message, ToastType.ERROR),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `좋아요 변경은 표시 수만 갱신하고 순서와 rank를 유지한다`() =
        coroutinesTest {
            val repository =
                FakeShopRankingRepository(
                    pageOf(shopRanking("first", 3), shopRanking("second", 2)),
                )
            val personalizationStore = FakePersonalizationRepository()
            val viewModel = rankingViewModel(repository, personalizationStore)
            runCurrent()

            viewModel.dispatch(RankingIntent.OnBookmarkChanged("second", true))
            runCurrent()

            assertEquals(listOf("first", "second"), shopIds(viewModel))
            assertEquals(
                listOf(1, 2),
                viewModel.uiState.value.shops
                    .map { it.rank },
            )
            assertEquals(3L, viewModel.uiState.value.displayedLikeCount(viewModel.uiState.value.shops[1]))
            assertEquals(1, repository.queries.size)
        }

    @Test
    fun `좋아요 저장 실패는 표시 수를 원복한다`() =
        coroutinesTest {
            val personalizationStore = FakePersonalizationRepository()
            personalizationStore.bookmarkUpdateError = RamapError.Unknown(IllegalStateException("failure"))
            val viewModel = rankingViewModel(personalizationStore = personalizationStore)

            viewModel.sideEffect.test {
                viewModel.dispatch(RankingIntent.OnBookmarkChanged("shop-id", true))
                runCurrent()

                assertEquals(
                    RankingSideEffect.ShowToast(
                        ToastData(Res.string.personalization_update_failure_message, ToastType.ERROR),
                    ),
                    awaitItem(),
                )
                assertFalse(
                    viewModel.uiState.value.bookmarkLikeCountDeltas
                        .containsKey("shop-id"),
                )
            }
        }

    @Test
    fun `비로그인 사용자의 좋아요 요청은 로그인 안내만 보낸다`() =
        coroutinesTest {
            val personalizationStore = FakePersonalizationRepository()
            val viewModel =
                rankingViewModel(
                    personalizationStore = personalizationStore,
                    loginRepository = FakeLoginRepository(LoginSessionState.NOT_AUTHENTICATED),
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(RankingIntent.OnBookmarkChanged("shop-id", true))

                assertEquals(RankingSideEffect.ShowLoginGuide, awaitItem())
                assertEquals(emptyList(), personalizationStore.bookmarkUpdateRequests)
            }
        }

    private fun shopIds(viewModel: RankingViewModel): List<String> =
        viewModel.uiState.value.shops
            .map { item -> item.ranking.shop.id }

    private fun pageOf(vararg rankings: ShopRanking): RankingPage = RankingPage(ShopRankings(rankings.toList()), null)

    private fun shopRanking(
        id: String = "shop-id",
        likeCount: Long = 3,
    ): ShopRanking =
        ShopRanking(
            shop =
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
                    menuCategories = MenuCategories(listOf(Category.SHOYU)),
                    isVisible = true,
                    createdAt = "",
                    updatedAt = "",
                ),
            likeCount = likeCount,
        )
}

private fun rankingViewModel(
    repository: FakeShopRankingRepository = FakeShopRankingRepository(),
    personalizationStore: ShopPersonalizationStore = FakePersonalizationRepository(),
    loginRepository: FakeLoginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED),
) = RankingViewModel(repository, personalizationStore, loginRepository, FakeAnalyticsTracker())
