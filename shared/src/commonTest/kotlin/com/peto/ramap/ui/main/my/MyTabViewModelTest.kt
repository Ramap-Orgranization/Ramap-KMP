package com.peto.ramap.ui.main.my

import app.cash.turbine.test
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.Location
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopReportRepository
import com.peto.ramap.ui.common.CurrentLocationStore
import com.peto.ramap.ui.main.my.contract.NavigateToHiddenShops
import com.peto.ramap.ui.main.my.contract.OnCurrentLocationReportSubmit
import com.peto.ramap.ui.main.my.contract.OnHiddenShopsClick
import com.peto.ramap.ui.main.my.contract.ShowMyLoginGuide
import com.peto.ramap.ui.main.my.contract.ShowMyToast
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.place_report_location_unavailable_message
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MyTabViewModelTest {
    @Test
    fun `비로그인 사용자가 숨김 매장을 누르면 로그인 가이드를 보여준다`() =
        coroutinesTest {
            val viewModel = myTabViewModel()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnHiddenShopsClick)
                assertEquals(ShowMyLoginGuide, awaitItem())
            }

            assertEquals(true, viewModel.uiState.value.showLoginGuideDialog)
        }

    @Test
    fun `로그인 사용자가 숨김 매장을 누르면 숨김 매장 화면으로 이동한다`() =
        coroutinesTest {
            val viewModel = myTabViewModel(loginRepository = loggedInRepository())
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnHiddenShopsClick)
                assertEquals(NavigateToHiddenShops, awaitItem())
            }
        }

    @Test
    fun `현재 위치가 없으면 현재 위치 제보 시 에러 토스트를 보여준다`() =
        coroutinesTest {
            val viewModel = myTabViewModel()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnCurrentLocationReportSubmit)
                assertEquals(showMyToastSideEffect(Res.string.place_report_location_unavailable_message), awaitItem())
            }
        }

    @Test
    fun `공유 위치 저장소가 갱신되면 내 화면 상태에 현재 위치를 반영한다`() =
        coroutinesTest {
            val currentLocationStore = CurrentLocationStore()
            val viewModel = myTabViewModel(currentLocationStore = currentLocationStore)
            val location = Location(lat = 37.551, lng = 126.921)

            currentLocationStore.update(location)
            runCurrent()

            assertEquals(location, viewModel.uiState.value.currentLocation)
        }
}

private fun showMyToastSideEffect(message: StringResource): ShowMyToast =
    ShowMyToast(
        ToastData(
            message = message,
            type = ToastType.ERROR,
        ),
    )

private fun myTabViewModel(
    loginRepository: FakeLoginRepository = FakeLoginRepository(),
    ramenShopRepository: FakeRamenShopRepository = FakeRamenShopRepository(),
    shopReportRepository: FakeShopReportRepository = FakeShopReportRepository(),
    currentLocationStore: CurrentLocationStore = CurrentLocationStore(),
): MyTabViewModel =
    MyTabViewModel(
        loginRepository = loginRepository,
        ramenShopRepository = ramenShopRepository,
        reportRepository = shopReportRepository,
        currentLocationStore = currentLocationStore,
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
