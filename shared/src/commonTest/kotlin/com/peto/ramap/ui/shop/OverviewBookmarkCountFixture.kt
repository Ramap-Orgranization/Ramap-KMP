package com.peto.ramap.ui.shop

import androidx.lifecycle.ViewModelStore
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.usecase.di.useCaseModule
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.FetchShopOverviewUseCase
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeOperatingNoticeRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopReportRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.FakeMapSearchHistoryStorage
import com.peto.ramap.ui.shop.overview.OverviewViewModel
import com.peto.ramap.ui.shop.overview.log.ShopAnalytics
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class OverviewBookmarkCountFixture {
    val shop = ramenShopFixture(id = "shop")
    val initialOverview = ShopOverView(shop, 22L, null, null, null)
    val login = FakeLoginRepository()
    val personalization = FakePersonalizationRepository(initialState = PersonalizationBootstrapState.Loading)
    val requestedShopIds = mutableListOf<String>()
    var fetchOverview: suspend () -> RamapResult<ShopOverView> = { RamapResult.Success(initialOverview) }
    var bookmarkResponse: suspend () -> RamapResult<Unit> = { RamapResult.Success(Unit) }

    private val personalizationStore =
        object : PersonalizationStore by personalization {
            override suspend fun updateBookmark(
                shopId: String,
                enabled: Boolean,
            ): RamapResult<Unit> {
                val previousIds = personalization.bookmarkedShopIds.value
                val result = personalization.updateBookmark(shopId, enabled)
                if (result is RamapResult.Error) return result
                return bookmarkResponse().also {
                    if (it is RamapResult.Error) personalization.updateBookmarkedShopIds(previousIds)
                }
            }
        }

    private val repository =
        object : RamenShopRepository by FakeRamenShopRepository() {
            override suspend fun fetchShopOverview(shopId: String): RamapResult<ShopOverView> {
                requestedShopIds += shopId
                return fetchOverview()
            }
        }
    private val dependencies =
        koinApplication {
            modules(
                useCaseModule,
                module {
                    single<RamenShopRepository> { repository }
                    single<OperatingNoticeRepository> { FakeOperatingNoticeRepository() }
                },
            )
        }
    val query = dependencies.koin.get<FetchShopOverviewUseCase>()
    private val analytics = FakeAnalyticsTracker()
    val viewModel =
        OverviewViewModel(
            loginRepository = login,
            shopReportRepository = FakeShopReportRepository(),
            personalizationStore = personalizationStore,
            fetchShopOverviewUseCase = query,
            mapSearchHistoryStorage = FakeMapSearchHistoryStorage(),
            mapAnalytics = ShopAnalytics(analytics),
            signInUseCase = SignInUseCase(login, LoginAnalytics(analytics, FakeCrashReporter())),
        )
    private val store = ViewModelStore().apply { put("overview", viewModel) }

    fun authenticate() {
        login.updateSessionState(LoginSessionState.AUTHENTICATED)
    }

    fun close() {
        store.clear()
        dependencies.close()
    }
}
