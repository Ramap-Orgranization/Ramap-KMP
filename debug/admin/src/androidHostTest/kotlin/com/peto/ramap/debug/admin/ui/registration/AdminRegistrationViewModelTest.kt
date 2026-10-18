package com.peto.ramap.debug.admin.ui.registration

import com.peto.ramap.coroutinesTest
import com.peto.ramap.debug.admin.FakeAdminRegistrationDataSource
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminNoticeDay
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatus
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationIntent
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationTab
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AdminRegistrationViewModelTest {
    @Test
    fun `expired undated and dated events are excluded and upcoming events remain editable`() =
        coroutinesTest {
            val viewModel = viewModel()
            runCurrent()
            assertEquals(
                listOf("active", "upcoming"),
                viewModel.uiState.value.managedEvents
                    .map { it.id },
            )
            assertFalse(viewModel.uiState.value.isLoadingEvents)
        }

    @Test
    fun `reselecting and switching tabs preserves separate registration forms`() =
        coroutinesTest {
            val viewModel = viewModel()
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.EVENT_REGISTRATION))
            viewModel.dispatch(AdminRegistrationIntent.OnShopNameChanged("이벤트 매장"))
            viewModel.dispatch(AdminRegistrationIntent.OnSourceUrlChanged("https://instagram.com/p/event"))
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.EVENT_REGISTRATION))
            runCurrent()
            assertEquals("이벤트 매장", viewModel.uiState.value.shopName)
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.OPERATING_NOTICE))
            runCurrent()
            assertEquals("", viewModel.uiState.value.shopName)
            assertEquals("", viewModel.uiState.value.sourceUrl)
            viewModel.dispatch(AdminRegistrationIntent.OnShopNameChanged("공지 매장"))
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.EVENT_REGISTRATION))
            runCurrent()
            assertEquals("이벤트 매장", viewModel.uiState.value.shopName)
            assertEquals("https://instagram.com/p/event", viewModel.uiState.value.sourceUrl)
        }

    @Test
    fun `editing remains in edit tab and cancel returns to list`() =
        coroutinesTest {
            val viewModel = viewModel()
            runCurrent()
            viewModel.dispatch(AdminRegistrationIntent.OnManagedEventEditRequested("active"))
            runCurrent()
            assertEquals(AdminRegistrationTab.EVENT_EDIT, viewModel.uiState.value.selectedTab)
            assertEquals("active", viewModel.uiState.value.editingEventId)
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.EVENT_EDIT))
            runCurrent()
            assertNull(viewModel.uiState.value.editingEventId)
            assertNull(viewModel.uiState.value.draft)
        }

    @Test
    fun `renewal today has no end date and new menu uses seven days`() =
        coroutinesTest {
            val viewModel = viewModel()
            viewModel.dispatch(AdminRegistrationIntent.OnEventTypeSelected(ShopEventType.STORE_RENEWAL))
            viewModel.dispatch(AdminRegistrationIntent.OnTodaySelected)
            runCurrent()
            assertNull(viewModel.uiState.value.selectedEndDate)
            viewModel.dispatch(AdminRegistrationIntent.OnEventTypeSelected(ShopEventType.NEW_MENU))
            runCurrent()
            assertEquals("2026-10-17", viewModel.uiState.value.selectedEndDate)
        }

    @Test
    fun `status type change keeps selected event`() =
        coroutinesTest {
            val viewModel = viewModel()
            runCurrent()
            viewModel.dispatch(AdminRegistrationIntent.OnManagedEventSelected("active"))
            viewModel.dispatch(AdminRegistrationIntent.OnEventStatusSelected(AdminEventStatus.CANCELLED))
            runCurrent()
            assertEquals("active", viewModel.uiState.value.selectedManagedEventId)
        }

    @Test
    fun `save blocks duplicate requests and changes to in flight form`() =
        coroutinesTest {
            val data = FakeAdminRegistrationDataSource()
            val viewModel = viewModel(data)
            runCurrent()
            data.gate = CompletableDeferred()
            viewModel.dispatch(AdminRegistrationIntent.OnManagedEventSelected("active"))
            viewModel.dispatch(AdminRegistrationIntent.OnEventStatusSaved)
            runCurrent()
            assertTrue(viewModel.uiState.value.isSavingEventStatus)
            viewModel.dispatch(AdminRegistrationIntent.OnEventStatusSaved)
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.EVENT_REGISTRATION))
            runCurrent()
            assertEquals(AdminRegistrationTab.EVENT_MANAGEMENT, viewModel.uiState.value.selectedTab)
            data.gate!!.complete(Unit)
            runCurrent()
            assertEquals(1, data.saves)
            assertFalse(viewModel.uiState.value.isSavingEventStatus)
            assertNull(viewModel.uiState.value.selectedManagedEventId)
            assertTrue(
                viewModel.uiState.value.managedEvents
                    .first()
                    .isSoldOutOn(LocalDate.parse(FakeAdminRegistrationDataSource.TODAY)),
            )
        }

    @Test
    fun `failed event fetch exposes retry and successful retry clears error`() =
        coroutinesTest {
            val data = FakeAdminRegistrationDataSource().apply { eventsFailure = IllegalStateException("offline") }
            val viewModel = viewModel(data)
            runCurrent()
            assertTrue(viewModel.uiState.value.managedEventsLoadFailed)
            assertFalse(viewModel.uiState.value.isLoadingEvents)
            data.eventsFailure = null
            viewModel.dispatch(AdminRegistrationIntent.OnManagedEventsRefreshed)
            runCurrent()
            assertFalse(viewModel.uiState.value.managedEventsLoadFailed)
            assertEquals(2, viewModel.uiState.value.managedEvents.size)
        }

    @Test
    fun `registration refreshes list and clears source to prevent duplicate submission`() =
        coroutinesTest {
            val data = FakeAdminRegistrationDataSource()
            val viewModel = viewModel(data)
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.EVENT_REGISTRATION))
            viewModel.dispatch(AdminRegistrationIntent.OnSourceUrlChanged("https://instagram.com/p/test"))
            viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
            runCurrent()
            assertEquals(FakeAdminRegistrationDataSource.TODAY, viewModel.uiState.value.selectedStartDate)
            viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
            runCurrent()
            assertEquals(1, data.registrations)
            assertEquals("", viewModel.uiState.value.sourceUrl)
            assertNull(viewModel.uiState.value.draft)
            assertTrue(
                viewModel.uiState.value.managedEvents
                    .any { it.id == "registered" },
            )
        }

    @Test
    fun `조기 마감 시각이 없으면 등록을 막고 확인한 시각을 입력하면 등록한다`() =
        coroutinesTest {
            val data = FakeAdminRegistrationDataSource()
            val viewModel = viewModel(data)
            runCurrent()
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.OPERATING_NOTICE))
            viewModel.dispatch(AdminRegistrationIntent.OnSourceUrlChanged("https://www.instagram.com/test_shop/"))
            viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
            runCurrent()
            viewModel.dispatch(AdminRegistrationIntent.OnDraftNoticeTimesChanged(null, null))
            viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
            runCurrent()
            assertEquals(0, data.registrations)
            viewModel.dispatch(AdminRegistrationIntent.OnDraftNoticeTimesChanged(null, "15:00"))
            viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
            runCurrent()
            assertEquals(1, data.registrations)
            assertEquals("15:00", data.savedDraft?.endTime)
        }

    @Test
    fun `달력 공지에서 조기 마감으로 바꾸면 월 전체 기간과 초안을 무효화한다`() =
        coroutinesTest {
            val data =
                FakeAdminRegistrationDataSource().apply {
                    previewResult =
                        AdminDraft(
                            noticeType = "full_close",
                            startDate = "2026-10-01",
                            endDate = "2026-10-31",
                            dailySchedules = listOf(AdminNoticeDay("2026-10-14", true), AdminNoticeDay("2026-10-21", true)),
                        )
                }
            val viewModel = viewModel(data)
            runCurrent()
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.OPERATING_NOTICE))
            viewModel.dispatch(AdminRegistrationIntent.OnSourceUrlChanged("https://www.instagram.com/test_shop/"))
            viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
            runCurrent()
            viewModel.dispatch(AdminRegistrationIntent.OnNoticeTypeSelected(OperatingNoticeType.EARLY_CLOSING))
            runCurrent()
            assertNull(viewModel.uiState.value.draft)
            assertNull(viewModel.uiState.value.selectedStartDate)
            assertNull(viewModel.uiState.value.selectedEndDate)
        }

    @Test
    fun `운영 변동으로 바꿔도 확인된 종료 시각을 유지하고 등록한다`() =
        coroutinesTest {
            val data = FakeAdminRegistrationDataSource()
            val viewModel = viewModel(data)
            runCurrent()
            viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(AdminRegistrationTab.OPERATING_NOTICE))
            viewModel.dispatch(AdminRegistrationIntent.OnSourceUrlChanged("https://www.instagram.com/test_shop/"))
            viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
            runCurrent()
            viewModel.dispatch(AdminRegistrationIntent.OnNoticeTypeSelected(OperatingNoticeType.PLANNED_CLOSING))
            runCurrent()
            assertEquals(
                "15:00",
                viewModel.uiState.value.draft
                    ?.endTime,
            )
            viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
            runCurrent()
            assertEquals("planned_close", data.savedDraft?.noticeType)
            assertEquals("15:00", data.savedDraft?.endTime)
        }

    private fun viewModel(data: FakeAdminRegistrationDataSource = FakeAdminRegistrationDataSource()) = AdminRegistrationViewModel(data) { LocalDate.parse(FakeAdminRegistrationDataSource.TODAY) }

    @Test
    fun `event and notice ranges survive preview and registration across months`() =
        coroutinesTest {
            for (tab in listOf(AdminRegistrationTab.EVENT_REGISTRATION, AdminRegistrationTab.OPERATING_NOTICE)) {
                val data = FakeAdminRegistrationDataSource()
                val viewModel = viewModel(data)
                viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(tab))
                viewModel.dispatch(AdminRegistrationIntent.OnSourceUrlChanged("https://instagram.com/p/range"))
                viewModel.dispatch(AdminRegistrationIntent.OnDateRangeSelected("2026-10-30", "2026-11-02"))
                viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
                runCurrent()
                assertEquals(
                    "2026-10-30",
                    viewModel.uiState.value.draft
                        ?.startDate,
                )
                assertEquals(
                    "2026-11-02",
                    viewModel.uiState.value.draft
                        ?.endDate,
                )
                viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
                runCurrent()
                assertEquals("2026-10-30", data.savedDraft?.startDate)
                assertEquals("2026-11-02", data.savedDraft?.endDate)
                assertEquals(tab == AdminRegistrationTab.OPERATING_NOTICE, data.registeredAsOperatingNotice)
                if (tab == AdminRegistrationTab.OPERATING_NOTICE) assertEquals("15:00", data.savedDraft?.endTime)
            }
        }

    @Test
    fun `changing a preview date range keeps its content and submits the new range`() =
        coroutinesTest {
            for (tab in listOf(AdminRegistrationTab.EVENT_REGISTRATION, AdminRegistrationTab.OPERATING_NOTICE)) {
                val data = FakeAdminRegistrationDataSource()
                val viewModel = viewModel(data)
                viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(tab))
                viewModel.dispatch(AdminRegistrationIntent.OnSourceUrlChanged("https://instagram.com/p/range"))
                viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
                runCurrent()
                val draft = requireNotNull(viewModel.uiState.value.draft)
                viewModel.dispatch(AdminRegistrationIntent.OnDateRangeSelected("2026-10-12", "2026-10-14"))
                runCurrent()
                assertEquals(draft.copy(startDate = "2026-10-12", endDate = "2026-10-14"), viewModel.uiState.value.draft)
                viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked)
                runCurrent()
                assertEquals("2026-10-12", data.savedDraft?.startDate)
                assertEquals("2026-10-14", data.savedDraft?.endDate)
            }
        }
}
