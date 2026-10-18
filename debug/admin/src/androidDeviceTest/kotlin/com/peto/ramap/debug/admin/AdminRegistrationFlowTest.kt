package com.peto.ramap.debug.admin

import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelStore
import com.peto.ramap.debug.admin.ui.registration.AdminRegistrationRoute
import com.peto.ramap.debug.admin.ui.registration.AdminRegistrationViewModel
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationIntent
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.theme.RamapTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminRegistrationFlowTest {
    @get:Rule
    val compose = createComposeRule()
    private val data = FakeAdminRegistrationDataSource()
    private val store = ViewModelStore()
    private lateinit var viewModel: AdminRegistrationViewModel
    private lateinit var calendarLocale: Locale

    @Before
    fun showAdmin() {
        viewModel = AdminRegistrationViewModel(data) { LocalDate.parse(FakeAdminRegistrationDataSource.TODAY) }
        store.put("admin", viewModel)
        compose.setContent {
            calendarLocale = LocalLocale.current.platformLocale
            RamapTheme { AdminRegistrationRoute(toastManager = ToastManager(), viewModel = viewModel) }
        }
        compose.waitUntil {
            viewModel.uiState.value.managedEvents
                .isNotEmpty()
        }
    }

    @After
    fun clearViewModel() {
        compose.runOnIdle { store.clear() }
    }

    @Test
    fun expiredEventsAreHiddenAndSoldOutSaveRefreshesCard() {
        compose.onNodeWithText("이벤트 ended").assertDoesNotExist()
        compose.onNodeWithText("이벤트 old-renewal").assertDoesNotExist()
        compose.onNodeWithText("이벤트 active").performClick()
        compose.onNodeWithText("저장").assertIsDisplayed().performClick()
        compose.waitUntil { data.saves == 1 && viewModel.uiState.value.selectedManagedEventId == null }
        compose.onNodeWithText("품절").assertIsDisplayed()
    }

    @Test
    fun cancellationRequiresReasonAndKeepsSelectionWhenChangingAction() {
        compose.onNodeWithText("이벤트 active").performClick()
        compose.onAllNodesWithText("취소 처리").onLast().performClick()
        compose.onNodeWithText("저장").assertIsNotEnabled()
        compose.onNode(hasSetTextAction() and hasText("사유")).performTextInput("테스트 사유")
        compose.onNodeWithText("저장").assertIsEnabled().performClick()
        compose.waitUntil { data.saves == 1 }
        compose.runOnIdle { assertEquals(listOf(FakeAdminRegistrationDataSource.TODAY), data.events.first().cancelledDates) }
    }

    @Test
    fun editSavesTitleAndReturnsToUpdatedList() {
        tab("이벤트 수정")
        compose.onNodeWithText("이벤트 active").performClick()
        compose.onNodeWithText("수정").assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("이벤트 active")).performScrollTo().performTextReplacement("변경된 테스트 이벤트")
        compose.onNodeWithText("외부 행사 장소명 (등록 매장이면 비워 두세요)").assertDoesNotExist()
        compose.onNodeWithText("수정").performClick()
        compose.waitUntil { data.edits == 1 && viewModel.uiState.value.editingEventId == null }
        compose.onNodeWithText("변경된 테스트 이벤트").assertIsDisplayed()
    }

    @Test
    fun registrationFormSurvivesTabSwitchAndPreviewCanBeRegistered() {
        tab("이벤트 등록")
        compose.onNode(hasSetTextAction() and hasText("https://instagram.com/p/...")).performScrollTo().performTextInput("https://instagram.com/p/test/")
        tab("영업 변동")
        tab("이벤트 등록")
        compose.onNode(hasSetTextAction() and hasText("https://instagram.com/p/test/")).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("미리보기", useUnmergedTree = false).performClick()
        compose.waitUntil { viewModel.uiState.value.draft != null }
        compose.onNodeWithText("등록").assertIsDisplayed().performClick()
        compose.waitUntil { data.registrations == 1 }
        compose.runOnIdle { assertNull(viewModel.uiState.value.draft) }
        tab("이벤트 수정")
        compose.onNodeWithText("등록 테스트").assertIsDisplayed()
    }

    @Test
    fun failedListCanBeRetriedFromScreen() {
        compose.runOnIdle {
            data.eventsFailure = IllegalStateException("offline")
            viewModel.dispatch(AdminRegistrationIntent.OnManagedEventsRefreshed)
        }
        compose.waitUntil { viewModel.uiState.value.managedEventsLoadFailed }
        compose.runOnIdle { data.eventsFailure = null }
        compose
            .onNode(
                androidx.compose.ui.test
                    .hasContentDescription("새로고침"),
            ).performClick()
        compose.waitUntil { !viewModel.uiState.value.managedEventsLoadFailed && !viewModel.uiState.value.isLoadingEvents }
        compose.onNodeWithText("이벤트 active").assertIsDisplayed()
    }

    @Test
    fun customCancellationDatePickerAndTodaySelectionWork() {
        compose.onNodeWithText("이벤트 active").performClick()
        compose.onAllNodesWithText("취소 처리").onLast().performClick()
        compose.onNodeWithText("기간 지정").performClick()
        compose.onNodeWithText("날짜를 선택해 주세요").performClick()
        compose.onNodeWithText("날짜 선택").assertIsDisplayed()
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithText("오늘", substring = false).performClick()
        compose.onNode(hasSetTextAction() and hasText("사유")).performTextInput("테스트 사유")
        compose
            .onNodeWithText("저장")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        compose.waitUntil { data.saves == 1 }
    }

    @Test
    fun correctionPreviewRequiresConfirmationAndRefreshesEditedEvent() {
        tab("이벤트 수정")
        compose
            .onNode(
                androidx.compose.ui.test
                    .hasScrollAction(),
            ).performScrollToNode(hasText("등록 내용 수정"))
        compose.onNode(hasSetTextAction()).performTextInput("테스트 매장 이벤트 제목 수정")
        compose.onNodeWithText("수정 내용 확인").performScrollTo().performClick()
        compose.onNodeWithText("이대로 수정할까요?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, data.correctionApplications) }
        compose.onNodeWithText("수정 적용").performClick()
        compose.waitUntil { data.correctionApplications == 1 }
        compose.runOnIdle {
            assertTrue(
                viewModel.uiState.value.managedEvents
                    .any { it.title == "수정된 제목" },
            )
        }
    }

    @Test
    fun eventRegistrationKeepsTheCalendarDateRange() {
        registerDateRange("이벤트 등록", isOperatingNotice = false)
    }

    @Test
    fun operatingNoticeRegistrationKeepsTheCalendarDateRangeAndHours() {
        registerDateRange("영업 변동", isOperatingNotice = true)
        compose.runOnIdle { assertEquals("11:00", data.savedDraft?.scheduleOverride?.open) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun registerDateRange(
        label: String,
        isOperatingNotice: Boolean,
    ) {
        tab(label)
        compose.onNode(hasSetTextAction() and hasText("https://instagram.com/p/...")).performScrollTo().performTextInput("https://instagram.com/p/range/")
        compose.onNodeWithText("오늘").performScrollTo().performClick()
        compose.onNodeWithText(FakeAdminRegistrationDataSource.TODAY).performClick()
        val formatter = DatePickerDefaults.dateFormatter()
        for (date in listOf("2026-10-12", "2026-10-14")) {
            val millis =
                LocalDate
                    .parse(date)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            val description = requireNotNull(formatter.formatDate(millis, calendarLocale, forContentDescription = true))
            compose.onNode(hasText(description, substring = true) or hasContentDescription(description, substring = true)).performScrollTo().performClick()
        }
        compose.onNodeWithText("확인").performClick()
        compose.onNodeWithText("2026-10-12 ~ 2026-10-14").assertIsDisplayed()
        compose.onNodeWithText("미리보기").performClick()
        compose.waitUntil { viewModel.uiState.value.draft != null }
        compose.runOnIdle {
            assertEquals(
                "2026-10-12",
                viewModel.uiState.value.draft
                    ?.startDate,
            )
            assertEquals(
                "2026-10-14",
                viewModel.uiState.value.draft
                    ?.endDate,
            )
        }
        compose.onNodeWithText("등록").assertIsDisplayed().performClick()
        compose.waitUntil { data.registrations == 1 }
        compose.runOnIdle {
            assertEquals("2026-10-12", data.savedDraft?.startDate)
            assertEquals("2026-10-14", data.savedDraft?.endDate)
            assertEquals(isOperatingNotice, data.registeredAsOperatingNotice)
        }
    }

    private fun tab(label: String) {
        compose.onNode(hasText(label) and hasClickAction()).performClick()
    }
}
