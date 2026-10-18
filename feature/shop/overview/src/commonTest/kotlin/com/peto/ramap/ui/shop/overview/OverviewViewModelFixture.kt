package com.peto.ramap.ui.shop.overview

import androidx.lifecycle.ViewModelStore
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.FetchShopOverviewUseCase
import com.peto.ramap.domain.usecase.ShopOverviewCacheLookup
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeShopReportRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.platform.storage.SearchHistoryStorage
import com.peto.ramap.ui.shop.overview.log.ShopAnalytics
import kotlinx.coroutines.flow.MutableStateFlow

class OverviewViewModelFixture(
    initialSessionState: LoginSessionState = LoginSessionState.NOT_AUTHENTICATED,
) {
    val login = FakeLoginRepository(initialSessionState)
    val personalization = FakePersonalizationRepository(initialState = PersonalizationBootstrapState.Loading)
    val requestedShopIds = mutableListOf<String>()
    var fetchOverview: suspend (String) -> RamapResult<ShopOverView> = { shopId ->
        RamapResult.Success(ShopOverView(ramenShopFixture(id = shopId), 0L, null, null, null))
    }
    var awaitPersonalizationResponse: suspend () -> Unit = {}

    private val queries =
        object : FetchShopOverviewUseCase {
            override suspend fun invoke(shopId: String): RamapResult<ShopOverView> {
                requestedShopIds += shopId
                return fetchOverview(shopId)
            }

            override fun findCached(shopId: String): ShopOverviewCacheLookup = ShopOverviewCacheLookup.Miss

            override fun clearCache() = Unit

            override fun updateCachedLikeCount(
                shopId: String,
                enabled: Boolean,
            ) = Unit
        }
    private val personalizationStore =
        object : PersonalizationStore by personalization {
            override suspend fun updateBookmark(
                shopId: String,
                enabled: Boolean,
            ): RamapResult<Unit> = personalization.updateBookmark(shopId, enabled).also { awaitPersonalizationResponse() }

            override suspend fun updateShopNotification(
                shopId: String,
                enabled: Boolean,
            ): RamapResult<Unit> = personalization.updateShopNotification(shopId, enabled).also { awaitPersonalizationResponse() }

            override suspend fun hideShop(shopId: String): RamapResult<Unit> = personalization.hideShop(shopId).also { awaitPersonalizationResponse() }

            override suspend fun unhideShop(shopId: String): RamapResult<Unit> = personalization.unhideShop(shopId).also { awaitPersonalizationResponse() }
        }
    private val history =
        object : SearchHistoryStorage {
            override val recentSearches = MutableStateFlow(emptyList<String>())
            override val recentlyViewedShopIds = MutableStateFlow(emptyList<String>())

            override suspend fun addRecentSearch(query: String) = Unit

            override suspend fun removeRecentSearch(query: String) = Unit

            override suspend fun clearRecentSearches() = Unit

            override suspend fun addRecentlyViewedShop(shopId: String) = Unit
        }
    private val analytics = FakeAnalyticsTracker()
    val viewModel =
        OverviewViewModel(
            loginRepository = login,
            shopReportRepository = FakeShopReportRepository(),
            personalizationStore = personalizationStore,
            fetchShopOverviewUseCase = queries,
            mapSearchHistoryStorage = history,
            mapAnalytics = ShopAnalytics(analytics),
            signInUseCase = SignInUseCase(login, LoginAnalytics(analytics, FakeCrashReporter())),
        )
    val store = ViewModelStore().apply { put("overview", viewModel) }

    fun authenticate() {
        login.updateSessionState(LoginSessionState.AUTHENTICATED)
        personalization.updateBookmarkedShopIds(setOf("existing"))
    }
}
