package com.peto.ramap.ui.bookmark.importation

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.importation.ImportationPreview
import com.peto.ramap.domain.model.importation.ImportationProvider
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.ImportationRepository
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.bookmark.importation.contract.ImportationError
import com.peto.ramap.ui.bookmark.importation.contract.ImportationIntent
import com.peto.ramap.ui.bookmark.importation.contract.ImportationSideEffect
import com.peto.ramap.ui.bookmark.importation.log.ImportationAnalytics
import com.peto.ramap.ui.bookmark.importation.log.event.ImportationMatchFailed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.importation_completed
import ramap.shared.generated.resources.importation_error_analyze
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ImportationViewModelTest {
    @Test
    fun `네이버와 카카오 공유 텍스트에서는 지원 URL만 저장한다`() =
        coroutinesTest {
            val viewModel =
                ImportationViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    ramenShopRepository = FakeRamenShopRepository(),
                    importationRepository = importationRepository(RamapResult.Success(importationPreview(emptySet()))),
                    importationAnalytics = ImportationAnalytics(FakeAnalyticsTracker()),
                )

            viewModel.dispatch(
                ImportationIntent.UrlChanged(
                    """[네이버지도]
                    |라멘
                    |https://naver.me/xtNICP0g
                    """.trimMargin(),
                ),
            )
            runCurrent()
            assertEquals("https://naver.me/xtNICP0g", viewModel.uiState.value.url)

            viewModel.dispatch(
                ImportationIntent.UrlChanged(
                    """[카카오맵] 라멘
                    |그룹
                    |
                    |https://kko.to/-ltZZdxL0H
                    """.trimMargin(),
                ),
            )
            runCurrent()
            assertEquals("https://kko.to/-ltZZdxL0H", viewModel.uiState.value.url)
        }

    @Test
    fun `목록 분석은 이미 좋아요하거나 숨긴 매장을 제외한 실제 후보를 표시한다`() =
        coroutinesTest {
            val importableShop = ramenShopFixture(id = "importable-shop")
            val bookmarkedShop = ramenShopFixture(id = "bookmarked-shop")
            val hiddenShop = ramenShopFixture(id = "hidden-shop")
            val preview =
                importationPreview(
                    matchedShopIds = setOf(importableShop.id, bookmarkedShop.id, hiddenShop.id),
                    unmatchedPlaceNames = listOf("매칭되지 않은 매장"),
                )
            val analyticsTracker = FakeAnalyticsTracker()
            val viewModel =
                ImportationViewModel(
                    personalizationStore =
                        FakePersonalizationRepository(
                            ShopPersonalization(
                                bookmarkedShopIds = setOf(bookmarkedShop.id),
                                hiddenShopIds = setOf(hiddenShop.id),
                            ),
                        ),
                    ramenShopRepository = FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(importableShop))),
                    importationRepository = importationRepository(RamapResult.Success(preview)),
                    importationAnalytics = ImportationAnalytics(analyticsTracker),
                )

            viewModel.dispatch(ImportationIntent.UrlChanged("https://map.kakao.com/example"))
            viewModel.dispatch(ImportationIntent.Analyze)
            runCurrent()

            assertEquals(preview, viewModel.uiState.value.preview)
            assertEquals(RamenShops(listOf(importableShop)), viewModel.uiState.value.candidates)
            assertEquals(1, viewModel.uiState.value.alreadyBookmarkedCount)
            assertEquals(1, viewModel.uiState.value.hiddenCount)
            assertEquals(1, analyticsTracker.events.size)
            assertEquals(
                ImportationMatchFailed(
                    provider = "kakao",
                    placeName = "매칭되지 않은 매장",
                ),
                analyticsTracker.events.single(),
            )
        }

    @Test
    fun `후보 제거 후 완료하면 남은 아이디만 한 번에 추가하고 성공 효과를 보낸다`() =
        coroutinesTest {
            val removedShop = ramenShopFixture(id = "removed-shop")
            val remainingShop = ramenShopFixture(id = "remaining-shop")
            val personalizationRepository = FakePersonalizationRepository()
            val preview = importationPreview(setOf(removedShop.id, remainingShop.id))
            val viewModel =
                ImportationViewModel(
                    personalizationStore = personalizationRepository,
                    ramenShopRepository =
                        FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(removedShop, remainingShop))),
                    importationRepository = importationRepository(RamapResult.Success(preview)),
                    importationAnalytics = ImportationAnalytics(FakeAnalyticsTracker()),
                )

            viewModel.dispatch(ImportationIntent.UrlChanged("https://naver.me/example"))
            viewModel.dispatch(ImportationIntent.Analyze)
            runCurrent()
            viewModel.dispatch(ImportationIntent.CandidateRemoved(removedShop.id))
            runCurrent()

            assertEquals(setOf(remainingShop.id), viewModel.uiState.value.candidates.keys)
            viewModel.sideEffect.test {
                viewModel.dispatch(ImportationIntent.Confirm)

                assertEquals(
                    ImportationSideEffect.ImportCompleted(
                        ToastData(Res.string.importation_completed, ToastType.SUCCESS),
                    ),
                    awaitItem(),
                )
                assertEquals(listOf(setOf(remainingShop.id)), personalizationRepository.bookmarkBulkAddRequests)
                assertEquals(
                    RamapResult.Success(ShopPersonalization(bookmarkedShopIds = setOf(remainingShop.id))),
                    personalizationRepository.fetchPersonalization(),
                )
            }
        }

    @Test
    fun `빈 후보 완료는 무시하고 초기화는 입력과 분석 결과를 지운다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "candidate-shop")
            val personalizationRepository = FakePersonalizationRepository()
            val preview = importationPreview(setOf(shop.id))
            val viewModel =
                ImportationViewModel(
                    personalizationStore = personalizationRepository,
                    ramenShopRepository = FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                    importationRepository = importationRepository(RamapResult.Success(preview)),
                    importationAnalytics = ImportationAnalytics(FakeAnalyticsTracker()),
                )

            viewModel.dispatch(ImportationIntent.UrlChanged("https://map.kakao.com/example"))
            viewModel.dispatch(ImportationIntent.Analyze)
            runCurrent()
            viewModel.dispatch(ImportationIntent.CandidateRemoved(shop.id))
            viewModel.dispatch(ImportationIntent.Confirm)
            runCurrent()

            assertEquals(emptyList(), personalizationRepository.bookmarkBulkAddRequests)
            assertEquals(preview, viewModel.uiState.value.preview)
            viewModel.dispatch(ImportationIntent.Reset)
            runCurrent()

            assertEquals("", viewModel.uiState.value.url)
            assertEquals(null, viewModel.uiState.value.preview)
            assertEquals(RamenShops(emptyMap()), viewModel.uiState.value.candidates)
            assertEquals(null, viewModel.uiState.value.error)
        }

    @Test
    fun `좋아요 추가 실패는 후보 화면과 기존 좋아요를 유지한다`() =
        coroutinesTest {
            val existingShopId = "existing-shop"
            val candidateShop = ramenShopFixture(id = "candidate-shop")
            val personalizationRepository =
                FakePersonalizationRepository(
                    ShopPersonalization(bookmarkedShopIds = setOf(existingShopId)),
                ).apply {
                    bookmarkUpdateError = RamapError.Unknown(IllegalStateException("failure"))
                }
            val preview = importationPreview(setOf(candidateShop.id))
            val viewModel =
                ImportationViewModel(
                    personalizationStore = personalizationRepository,
                    ramenShopRepository = FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(candidateShop))),
                    importationRepository = importationRepository(RamapResult.Success(preview)),
                    importationAnalytics = ImportationAnalytics(FakeAnalyticsTracker()),
                )

            viewModel.dispatch(ImportationIntent.UrlChanged("https://map.kakao.com/example"))
            viewModel.dispatch(ImportationIntent.Analyze)
            runCurrent()
            viewModel.dispatch(ImportationIntent.Confirm)
            runCurrent()

            assertEquals(ImportationError.CONFIRM_FAILED, viewModel.uiState.value.error)
            assertEquals(RamenShops(listOf(candidateShop)), viewModel.uiState.value.candidates)
            assertEquals(
                RamapResult.Success(ShopPersonalization(bookmarkedShopIds = setOf(existingShopId))),
                personalizationRepository.fetchPersonalization(),
            )
        }

    @Test
    fun `분석 실패 후 재시도하면 같은 링크로 후보를 다시 불러온다`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "retry-shop")
            val preview = importationPreview(setOf(shop.id))
            var requestCount = 0
            val repository =
                object : ImportationRepository {
                    override suspend fun analyze(url: String): RamapResult<ImportationPreview> {
                        requestCount += 1
                        return if (requestCount == 1) {
                            RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
                        } else {
                            RamapResult.Success(preview)
                        }
                    }
                }
            val viewModel =
                ImportationViewModel(
                    personalizationStore = FakePersonalizationRepository(),
                    ramenShopRepository = FakeRamenShopRepository(fetchByIdsResult = RamenShops(listOf(shop))),
                    importationRepository = repository,
                    importationAnalytics = ImportationAnalytics(FakeAnalyticsTracker()),
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(ImportationIntent.UrlChanged("https://map.kakao.com/example"))
                viewModel.dispatch(ImportationIntent.Analyze)
                runCurrent()

                assertEquals(
                    ImportationSideEffect.showToast(
                        ToastData(Res.string.importation_error_analyze, ToastType.ERROR),
                    ),
                    awaitItem(),
                )
                assertEquals(ImportationError.ANALYZE_FAILED, viewModel.uiState.value.error)
            }

            viewModel.dispatch(ImportationIntent.Retry)
            runCurrent()

            assertEquals(2, requestCount)
            assertEquals(RamenShops(listOf(shop)), viewModel.uiState.value.candidates)
            assertEquals(null, viewModel.uiState.value.error)
        }
}

private fun importationPreview(
    matchedShopIds: Set<String>,
    unmatchedPlaceNames: List<String> = emptyList(),
): ImportationPreview =
    ImportationPreview(
        provider = ImportationProvider.KAKAO,
        totalPlaceCount = matchedShopIds.size + unmatchedPlaceNames.size,
        matchedShopIds = matchedShopIds,
        unmatchedPlaceNames = unmatchedPlaceNames,
    )

private fun importationRepository(result: RamapResult<ImportationPreview>): ImportationRepository =
    object : ImportationRepository {
        override suspend fun analyze(url: String): RamapResult<ImportationPreview> = result
    }
