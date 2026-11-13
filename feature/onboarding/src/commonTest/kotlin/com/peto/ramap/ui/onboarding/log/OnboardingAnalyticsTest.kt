package com.peto.ramap.ui.onboarding.log

import app.cash.turbine.test
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopSuggestionsRepository
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeOnboardingStorage
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopSuggestionsRepository
import com.peto.ramap.platform.permission.NotificationPermissionOutcome
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import com.peto.ramap.ui.onboarding.OnboardingViewModel
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.di.onboardingModule
import com.peto.ramap.ui.onboarding.guide.log.OnboardingAnalyticsStage
import com.peto.ramap.ui.onboarding.permission.contract.OnboardingPermission
import com.peto.ramap.ui.onboarding.shopselection.ShopSelectionViewModel
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionIntent
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OnboardingAnalyticsTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `DI로 생성한 매장 선택과 온보딩 진입은 같은 시도에 기록한다`() =
        coroutinesTest {
            val tracker = FakeAnalyticsTracker()
            val application =
                koinApplication {
                    modules(
                        onboardingModule,
                        module {
                            single<AnalyticsTracker> { tracker }
                            single<OnboardingStorage> { FakeOnboardingStorage().also { it.confirmed = true } }
                            single<LoginRepository> { FakeLoginRepository() }
                            single<RamenShopRepository> { FakeRamenShopRepository() }
                            single<ShopSuggestionsRepository> { FakeShopSuggestionsRepository(RamenShops(emptyMap())) }
                            single<PersonalizationStore> {
                                FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("one", "two", "three")))
                            }
                            single { SearchRamenShopsUseCase(get()) }
                        },
                    )
                }
            try {
                val onboarding = application.koin.get<OnboardingViewModel>()
                runCurrent()
                val selection = application.koin.get<ShopSelectionViewModel> { parametersOf(onboarding.analytics) }
                runCurrent()
                selection.sideEffect.test {
                    selection.dispatch(ShopSelectionIntent.Complete)
                    val effect = awaitItem() as ShopSelectionSideEffect.Completed
                    onboarding.dispatch(OnboardingIntent.ShopSelectionCompleted(effect.savedShopCount))
                    runCurrent()
                }
                assertFalse(tracker.events.any { it.name == "onboarding_completed" })
                onboarding.analytics.viewStage(OnboardingAnalyticsStage.Login)
                onboarding.dispatch(OnboardingIntent.LoginStepCompleted)
                runCurrent()

                val started = tracker.events.single { it.name == "onboarding_started" }.params()
                val completed = tracker.events.single { it.name == "onboarding_completed" }.params()
                assertEquals(started["attempt_id"], completed["attempt_id"])
                assertEquals(3, completed["saved_count"])
                assertEquals("login", completed["stage"])
            } finally {
                application.close()
            }
        }

    @Test
    fun `같은 화면의 중복 노출과 복귀는 조회 수를 부풀리지 않고 완료 후에는 기록하지 않는다`() {
        val tracker = FakeAnalyticsTracker()
        val analytics = OnboardingAnalytics(tracker)

        analytics.start(OnboardingStep.Guide)
        analytics.viewStage(OnboardingAnalyticsStage.NotificationMenu)
        analytics.viewStage(OnboardingAnalyticsStage.NotificationMenu)
        analytics.viewStage(OnboardingAnalyticsStage.NotificationTypes)
        analytics.viewStage(OnboardingAnalyticsStage.NotificationMenu)
        analytics.viewStage(OnboardingAnalyticsStage.ReviewMap)
        analytics.viewStage(OnboardingAnalyticsStage.ShopSelection)
        analytics.viewStage(OnboardingAnalyticsStage.Login)
        analytics.shopGoalReached(3)
        analytics.shopGoalReached(4)
        analytics.completed(4)
        analytics.backgrounded()

        val views = tracker.events.filter { it.name == "onboarding_stage_viewed" }.map { it.params() }
        assertEquals(listOf("notification_menu", "notification_types", "notification_menu", "review_map", "shop_selection", "login"), views.map { it["stage"] })
        assertEquals(listOf(1, 2, 2, 3, 3, 3), views.map { it["guide_pages_viewed"] })
        assertEquals(1, tracker.events.count { it.name == "onboarding_shop_goal_reached" })
        assertEquals("onboarding_completed", tracker.events.last().name)
        assertEquals(
            1,
            tracker.events
                .map { it.params()["attempt_id"] }
                .distinct()
                .size,
        )
        assertEquals((1L..tracker.events.size.toLong()).toList(), tracker.events.map { it.params()["event_index"] })
    }

    @Test
    fun `권한 응답을 기다리는 동안 백그라운드 전환을 기록하고 실패를 미허용으로 집계하지 않는다`() {
        val tracker = FakeAnalyticsTracker()
        val analytics = OnboardingAnalytics(tracker)

        analytics.start(OnboardingStep.PermissionGuide)
        analytics.viewStage(OnboardingAnalyticsStage.PermissionGuide)
        analytics.permissionRequested(OnboardingPermission.Notification)
        analytics.backgrounded()
        analytics.backgrounded()
        analytics.foregrounded()
        analytics.notificationResult(NotificationPermissionOutcome.Failed)

        val backgrounded = tracker.events.single { it.name == "onboarding_backgrounded" }.params()
        assertEquals("permission_guide", backgrounded["stage"])
        assertEquals("notification", backgrounded["pending_permission"])
        assertEquals(1, tracker.events.count { it.name == "onboarding_foregrounded" })
        val result = tracker.events.single { it.name == "onboarding_permission_result" }.params()
        assertEquals("failed", result["result"])
        assertFalse("granted" in result)
        assertFalse(tracker.events.any { it.name == "onboarding_completed" })
    }
}
