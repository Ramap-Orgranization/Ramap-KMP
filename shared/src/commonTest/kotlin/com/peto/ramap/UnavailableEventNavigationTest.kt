package com.peto.ramap

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.ScreenRoutes
import com.peto.ramap.navigation.TabStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_not_found_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class UnavailableEventNavigationTest {
    @Test
    fun `이용할 수 없는 이벤트는 새소식으로 돌아가며 이유를 안내한다`() =
        runTest {
            val navigation =
                NavigationState(
                    selectedTabState = mutableStateOf(TabStatus.FEED),
                    backStacks =
                        mapOf(
                            TabStatus.MAP to NavBackStack<NavKey>(ScreenRoutes.MapRoutes()),
                            TabStatus.RANKING to NavBackStack<NavKey>(ScreenRoutes.RankingTabRoutes),
                            TabStatus.EVENT to NavBackStack<NavKey>(ScreenRoutes.EventTabRoutes()),
                            TabStatus.FEED to NavBackStack<NavKey>(ScreenRoutes.FeedTabRoutes),
                            TabStatus.MY to NavBackStack<NavKey>(ScreenRoutes.MyTabRoutes),
                        ),
                )
            val toasts = mutableListOf<ToastData>()
            val toastManager = ToastManager()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { toastManager.toasts.collect { toasts += it } }
            navigation.showEvent("missing-event")

            returnFromUnavailableEvent(navigation, toastManager)

            assertEquals(TabStatus.EVENT, navigation.selectedTab)
            assertIs<ScreenRoutes.EventTabRoutes>(navigation.currentRoute)
            assertEquals(Res.string.event_not_found_message, toasts.single().message)
        }
}
