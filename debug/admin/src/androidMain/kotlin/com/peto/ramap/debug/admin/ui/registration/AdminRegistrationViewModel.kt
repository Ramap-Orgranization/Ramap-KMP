package com.peto.ramap.debug.admin.ui.registration

import com.peto.ramap.debug.admin.data.datasource.AdminRegistrationDataSource
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatus
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatusScope
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationIntent
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationLoadKey
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationMessage
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationSideEffect
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationTab
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationUiState
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.admin_correction_failure
import ramap.shared.generated.resources.admin_correction_required
import ramap.shared.generated.resources.admin_correction_success
import ramap.shared.generated.resources.admin_event_status_event_required
import ramap.shared.generated.resources.admin_event_status_period_required
import ramap.shared.generated.resources.admin_event_status_reason_required
import ramap.shared.generated.resources.admin_event_status_save_failure
import ramap.shared.generated.resources.admin_registration_draft_required
import ramap.shared.generated.resources.admin_registration_image_only_required
import ramap.shared.generated.resources.admin_registration_image_required
import ramap.shared.generated.resources.admin_registration_managed_events_load_failure
import ramap.shared.generated.resources.admin_registration_preview_failure
import ramap.shared.generated.resources.admin_registration_preview_required
import ramap.shared.generated.resources.admin_registration_register_failure
import ramap.shared.generated.resources.admin_registration_shop_names_load_failure
import ramap.shared.generated.resources.admin_registration_success
import java.time.LocalDate
import java.time.ZoneId

internal class AdminRegistrationViewModel(
    private val dataSource: AdminRegistrationDataSource,
    private val today: () -> LocalDate = { LocalDate.now(ZoneId.of("Asia/Seoul")) },
) : BaseViewModel<AdminRegistrationUiState, AdminRegistrationIntent, AdminRegistrationSideEffect>(AdminRegistrationUiState()) {
    private val savedForms = mutableMapOf<AdminRegistrationTab, AdminRegistrationUiState>()

    init {
        loadShopNames()
        loadExternalVenues()
        loadManagedEvents()
    }

    override suspend fun handleIntent(intent: AdminRegistrationIntent) {
        if (currentState.isBusy) return
        when (intent) {
            AdminRegistrationIntent.OnResumed -> {
                loadManagedEvents()
            }
            AdminRegistrationIntent.OnEvidenceReadFailed -> showToast(Res.string.admin_registration_image_required)
            is AdminRegistrationIntent.OnRegistrationTypeSelected ->
                reduce {
                    copy(
                        isOperatingNotice = intent.isOperatingNotice,
                        selectedNoticeType = null,
                        selectedEventType = ShopEventType.LIMITED_MENU,
                        isImageOnly = false,
                        imageOnlyTitle = "",
                        draft = null,
                        selectedStartDate = null,
                        selectedEndDate = null,
                        message = null,
                    )
                }

            is AdminRegistrationIntent.OnNoticeTypeSelected ->
                reduce {
                    if (draft?.dailySchedules?.isNotEmpty() == true && intent.noticeType != OperatingNoticeType.TEMPORARY_CLOSURE) {
                        return@reduce copy(selectedNoticeType = intent.noticeType, draft = null, selectedStartDate = null, selectedEndDate = null, message = null)
                    }
                    copy(
                        selectedNoticeType = intent.noticeType,
                        draft =
                            draft?.copy(
                                noticeType = intent.noticeType.toRequestValue(),
                                startTime = null,
                                endTime = if (intent.noticeType == OperatingNoticeType.EARLY_CLOSING) draft.endTime else null,
                                scheduleOverride = null,
                                dailySchedules = if (intent.noticeType == OperatingNoticeType.TEMPORARY_CLOSURE) draft.dailySchedules else emptyList(),
                            ),
                        message = null,
                    )
                }

            is AdminRegistrationIntent.OnEventTypeSelected ->
                reduce {
                    copy(
                        selectedEventType = intent.eventType,
                        hasSelectedEventType = true,
                        selectedEndDate = normalizedEndDate(intent.eventType, selectedStartDate, selectedEndDate),
                        draft =
                            draft?.copy(
                                endDate = normalizedEndDate(intent.eventType, draft.startDate, draft.endDate),
                            ),
                        message = null,
                    )
                }

            is AdminRegistrationIntent.OnShopNameChanged -> {
                cancelTask(SHOP_HOURS_TASK_KEY)
                reduce { copy(shopName = intent.value, selectedShopHours = null, draft = if (editingEventId == null) null else draft, message = null) }
                loadShopHours(intent.value)
            }
            is AdminRegistrationIntent.OnSourceUrlChanged -> reduce { copy(sourceUrl = intent.value, draft = if (editingEventId == null) null else draft, message = null) }
            is AdminRegistrationIntent.OnFeedbackChanged -> reduce { copy(feedback = intent.value, draft = null, message = null) }
            is AdminRegistrationIntent.OnDraftVenueChanged ->
                reduce {
                    copy(draft = draft?.copy(venueName = intent.name.ifBlank { null }, venueAddress = intent.address.ifBlank { null }, externalVenueId = intent.externalVenueId.ifBlank { null }, venueInstagramUrl = intent.instagramUrl.ifBlank { null }, venueNaverMapUrl = intent.naverMapUrl.ifBlank { null }, venueKakaoMapUrl = intent.kakaoMapUrl.ifBlank { null }), message = null)
                }
            AdminRegistrationIntent.OnImageOnlyRegistrationClicked ->
                if (!currentState.isOperatingNotice) {
                    reduce { copy(isImageOnly = !isImageOnly, draft = null, message = null) }
                }
            is AdminRegistrationIntent.OnImageOnlyTitleChanged -> reduce { copy(imageOnlyTitle = intent.value, message = null) }
            is AdminRegistrationIntent.OnDraftTitleChanged ->
                reduce { copy(draft = draft?.copy(title = intent.value), message = null) }
            is AdminRegistrationIntent.OnDraftDescriptionChanged ->
                reduce { copy(draft = draft?.copy(description = intent.value), message = null) }
            is AdminRegistrationIntent.OnDraftNoticeTimesChanged ->
                reduce { copy(draft = draft?.copy(startTime = intent.startTime, endTime = intent.endTime), message = null) }
            is AdminRegistrationIntent.OnRegularSegmentSelected ->
                reduce {
                    copy(draft = draft?.copy(endTime = intent.close, startTime = null, scheduleOverride = null), message = null)
                }
            is AdminRegistrationIntent.OnEvidenceSelected -> reduce { copy(evidence = intent.evidence, draft = null, message = null) }
            is AdminRegistrationIntent.OnDateRangeSelected -> changeDates(intent.startDate, intent.endDate)

            AdminRegistrationIntent.OnTodaySelected -> selectToday()
            AdminRegistrationIntent.OnPreviewOrRegisterClicked -> previewOrRegister()
            AdminRegistrationIntent.OnManagedEventsRefreshed -> loadManagedEvents()
            is AdminRegistrationIntent.OnManagedEventSelected ->
                reduce {
                    copy(
                        selectedManagedEventId = if (selectedManagedEventId == intent.eventId) null else intent.eventId,
                        selectedEventStatusScope = if (managedEvents.firstOrNull { it.id == intent.eventId }?.occursOn(today()) == true) AdminEventStatusScope.TODAY else AdminEventStatusScope.ENTIRE_PERIOD,
                        eventStatusReason = "",
                        eventStatusStartDate = null,
                        eventStatusEndDate = null,
                    )
                }
            is AdminRegistrationIntent.OnEventStatusSelected ->
                reduce {
                    copy(
                        selectedEventStatus = intent.status,
                        selectedEventStatusScope = if (managedEvents.firstOrNull { it.id == selectedManagedEventId }?.occursOn(today()) == false) AdminEventStatusScope.ENTIRE_PERIOD else AdminEventStatusScope.TODAY,
                        eventStatusReason = "",
                        eventStatusStartDate = null,
                        eventStatusEndDate = null,
                    )
                }
            is AdminRegistrationIntent.OnEventStatusScopeSelected ->
                reduce {
                    copy(
                        selectedEventStatusScope = intent.scope,
                        eventStatusStartDate = if (intent.scope == AdminEventStatusScope.CUSTOM_PERIOD) eventStatusStartDate else null,
                        eventStatusEndDate = if (intent.scope == AdminEventStatusScope.CUSTOM_PERIOD) eventStatusEndDate else null,
                    )
                }
            is AdminRegistrationIntent.OnEventStatusReasonChanged -> reduce { copy(eventStatusReason = intent.value) }
            is AdminRegistrationIntent.OnEventStatusDateRangeSelected ->
                reduce {
                    copy(eventStatusStartDate = intent.startDate, eventStatusEndDate = intent.endDate)
                }
            AdminRegistrationIntent.OnEventStatusTodaySelected -> selectEventStatusToday()
            AdminRegistrationIntent.OnEventStatusSaved -> saveEventStatus()
            is AdminRegistrationIntent.OnManagedEventEditRequested -> startEventEditing(intent.eventId)
            is AdminRegistrationIntent.OnCorrectionRequestChanged ->
                reduce { copy(correctionRequest = intent.value, correctionPreview = null) }
            AdminRegistrationIntent.OnCorrectionPreviewRequested -> previewCorrection()
            AdminRegistrationIntent.OnCorrectionConfirmed -> applyCorrection()
            AdminRegistrationIntent.OnCorrectionPreviewDismissed -> reduce { copy(correctionPreview = null) }
            is AdminRegistrationIntent.OnTabSelected -> selectTab(intent.tab)
        }
    }

    private fun selectTab(tab: AdminRegistrationTab) {
        val state = currentState
        if (state.selectedTab == tab && state.editingEventId == null) return
        cancelTask(SHOP_HOURS_TASK_KEY)
        if (state.editingEventId == null && state.selectedTab in FORM_TABS) {
            savedForms[state.selectedTab] = state
        }
        val form = savedForms[tab] ?: AdminRegistrationUiState()
        reduce {
            restoreForm(form).copy(
                selectedTab = tab,
                isOperatingNotice = tab == AdminRegistrationTab.OPERATING_NOTICE,
                editingEventId = null,
                selectedManagedEventId = null,
                eventStatusReason = "",
                eventStatusStartDate = null,
                eventStatusEndDate = null,
                message = null,
            )
        }
        if (tab == AdminRegistrationTab.EVENT_MANAGEMENT || tab == AdminRegistrationTab.EVENT_EDIT) loadManagedEvents()
    }

    private fun selectToday() {
        val date = today().toString()
        changeDates(date, date)
    }

    private fun changeDates(
        startDate: String,
        endDate: String,
    ) {
        reduce {
            val resolvedEndDate = if (isOperatingNotice) endDate else normalizedEndDate(selectedEventType, startDate, endDate)
            copy(
                selectedStartDate = startDate,
                selectedEndDate = resolvedEndDate,
                draft = draft?.takeIf { it.dailySchedules.isEmpty() }?.copy(startDate = startDate, endDate = resolvedEndDate),
                message = null,
            )
        }
    }

    private fun loadShopNames() {
        launchTask(
            taskKey = SHOP_NAMES_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
        ) {
            try {
                val shopNames = dataSource.fetchShopNames()
                reduce { copy(shopNames = shopNames) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleError(exception)
                showToast(Res.string.admin_registration_shop_names_load_failure)
            }
        }
    }

    private fun loadExternalVenues() {
        launchTask(taskKey = EXTERNAL_VENUES_TASK_KEY, policy = TaskPolicy.IgnoreNew) {
            val venues = dataSource.fetchExternalVenues()
            reduce { copy(externalVenues = venues) }
        }
    }

    private fun loadShopHours(shopName: String) {
        if (shopName.isBlank()) return
        launchTask(taskKey = SHOP_HOURS_TASK_KEY, policy = TaskPolicy.CancelPrevious) {
            val hours = dataSource.fetchShopHours(shopName)
            if (currentState.shopName == shopName) reduce { copy(selectedShopHours = hours) }
        }
    }

    private fun loadManagedEvents() {
        launchTask(taskKey = MANAGED_EVENTS_TASK_KEY, loadKey = AdminRegistrationLoadKey.ManagedEvents, policy = TaskPolicy.CancelPrevious, onStart = { copy(managedEventsLoadFailed = false, today = today()) }) {
            try {
                val events = dataSource.fetchManagedEvents()
                val date = today()
                val activeEvents = events.filter { it.isManageableOn(date) }.sortedWith(compareBy<AdminManagedEvent> { it.effectiveEndDate ?: "9999-12-31" }.thenBy(AdminManagedEvent::startDate).thenBy(AdminManagedEvent::shopName))
                reduce { copy(managedEvents = activeEvents, today = date, selectedManagedEventId = selectedManagedEventId?.takeIf { id -> activeEvents.any { it.id == id } }) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleError(exception)
                reduce { copy(managedEventsLoadFailed = true) }
                showToast(Res.string.admin_registration_managed_events_load_failure)
            }
        }
    }

    private fun previewCorrection() {
        val request = currentState.correctionRequest.trim()
        if (request.isEmpty()) {
            showToast(Res.string.admin_correction_required)
            return
        }
        launchTask(taskKey = CORRECTION_TASK_KEY, loadKey = AdminRegistrationLoadKey.Correction, policy = TaskPolicy.IgnoreNew) {
            try {
                val preview = dataSource.previewCorrection(request)
                reduce { copy(correctionPreview = preview) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleError(exception)
                showToast(Res.string.admin_correction_failure)
            }
        }
    }

    private fun applyCorrection() {
        val preview = currentState.correctionPreview ?: return
        launchTask(taskKey = CORRECTION_TASK_KEY, loadKey = AdminRegistrationLoadKey.Correction, policy = TaskPolicy.IgnoreNew) {
            try {
                dataSource.applyCorrection(preview, preview.changes)
                reduce { copy(correctionPreview = null, correctionRequest = "") }
                loadManagedEvents()
                showToast(Res.string.admin_correction_success, ToastType.SUCCESS)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleError(exception)
                showToast(Res.string.admin_correction_failure)
            }
        }
    }

    private fun saveEventStatus() {
        val state = currentState
        val eventId = state.selectedManagedEventId
        if (eventId == null) {
            showToast(Res.string.admin_event_status_event_required)
            return
        }
        val event = state.managedEvents.firstOrNull { it.id == eventId && it.canChangeStatusOn(today()) }
        if (event == null || (state.selectedEventStatus == AdminEventStatus.SOLD_OUT && (!event.occursOn(today()) || event.isCancelledOn(today()) || event.isSoldOutOn(today())))) {
            showToast(Res.string.admin_event_status_event_required)
            loadManagedEvents()
            return
        }
        val reason = state.eventStatusReason.trim().takeIf { state.selectedEventStatus == AdminEventStatus.CANCELLED }
        if (state.selectedEventStatus == AdminEventStatus.CANCELLED && reason.isNullOrEmpty()) {
            showToast(Res.string.admin_event_status_reason_required)
            return
        }
        if (state.selectedEventStatusScope == AdminEventStatusScope.CUSTOM_PERIOD &&
            (state.eventStatusStartDate == null || state.eventStatusEndDate == null || state.eventStatusStartDate > state.eventStatusEndDate || state.eventStatusStartDate < event.startDate || (event.effectiveEndDate != null && state.eventStatusEndDate > requireNotNull(event.effectiveEndDate)))
        ) {
            showToast(Res.string.admin_event_status_period_required)
            return
        }
        launchTask(taskKey = EVENT_STATUS_SAVE_TASK_KEY, loadKey = AdminRegistrationLoadKey.EventStatus, policy = TaskPolicy.IgnoreNew) {
            try {
                dataSource.saveEventStatus(
                    eventId = eventId,
                    status = state.selectedEventStatus.requestValue(),
                    scope =
                        if (state.selectedEventStatus ==
                            AdminEventStatus.SOLD_OUT
                        ) {
                            "today"
                        } else {
                            state.selectedEventStatusScope.requestValue()
                        },
                    reason = reason,
                    startDate = state.eventStatusStartDate,
                    endDate = state.eventStatusEndDate,
                )
                reduce {
                    copy(
                        eventStatusReason = "",
                        eventStatusStartDate = null,
                        eventStatusEndDate = null,
                        selectedManagedEventId = null,
                    )
                }
                loadManagedEvents()
                showToast(Res.string.admin_correction_success, ToastType.SUCCESS)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleError(exception)
                showToast(Res.string.admin_event_status_save_failure)
            }
        }
    }

    private fun selectEventStatusToday() {
        val today = today().toString()
        reduce { copy(eventStatusStartDate = today, eventStatusEndDate = today) }
    }

    private fun startEventEditing(eventId: String) {
        val event = currentState.managedEvents.firstOrNull { it.id == eventId } ?: return
        val eventType = runCatching { ShopEventType.from(event.eventType) }.getOrNull() ?: return
        reduce {
            copy(
                selectedTab = AdminRegistrationTab.EVENT_EDIT,
                isOperatingNotice = false,
                isImageOnly = false,
                editingEventId = event.id,
                selectedEventType = eventType,
                shopName = event.shopName,
                selectedStartDate = event.startDate,
                selectedEndDate = event.endDate,
                draft = createEventDraft(event),
                message = null,
            )
        }
    }

    private fun previewOrRegister() {
        if (currentState.isImageOnly) {
            registerImageOnly()
        } else if (currentState.draft == null) {
            preview()
        } else {
            register()
        }
    }

    private fun preview() {
        if (currentState.sourceUrl.isBlank() && currentState.evidence == null) {
            showToast(Res.string.admin_registration_preview_required)
            return
        }

        launchTask(
            taskKey = SUBMIT_TASK_KEY,
            loadKey = AdminRegistrationLoadKey.Submission,
            policy = TaskPolicy.IgnoreNew,
        ) {
            reduce { copy(message = null) }
            try {
                val result =
                    dataSource.preview(
                        shopName = currentState.shopName,
                        feedback = currentState.feedback,
                        sourceUrl = currentState.sourceUrl,
                        evidence = currentState.evidence,
                        isOperatingNotice = currentState.isOperatingNotice,
                    )
                reduce {
                    AdminRegistrationPreviewStateReducer.reduce(this, result)
                }
                loadShopHours(currentState.shopName)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleError(exception)
                showToast(Res.string.admin_registration_preview_failure)
            }
        }
    }

    private fun register() {
        val draft = currentState.draft
        if (draft == null) {
            showToast(Res.string.admin_registration_draft_required)
            return
        }
        if (currentState.isOperatingNotice && !draft.canRegisterOperatingNotice()) {
            showToast(Res.string.admin_registration_register_failure)
            return
        }
        launchTask(
            taskKey = SUBMIT_TASK_KEY,
            loadKey = AdminRegistrationLoadKey.Submission,
            policy = TaskPolicy.IgnoreNew,
        ) {
            reduce { copy(message = null) }
            try {
                val editingEventId = currentState.editingEventId
                if (editingEventId == null) {
                    dataSource.register(draft, currentState.isOperatingNotice, currentState.selectedEventType)
                } else {
                    dataSource.updateEvent(editingEventId, draft, currentState.selectedEventType)
                }
                reduce { restoreForm(AdminRegistrationUiState()).copy(editingEventId = null, message = AdminRegistrationMessage.SUCCESS) }
                savedForms.remove(currentState.selectedTab)
                loadManagedEvents()
                if (!currentState.isOperatingNotice && editingEventId == null) loadExternalVenues()
                showToast(if (editingEventId == null) Res.string.admin_registration_success else Res.string.admin_correction_success, ToastType.SUCCESS)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleError(exception)
                showToast(Res.string.admin_registration_register_failure)
            }
        }
    }

    private fun registerImageOnly() {
        val state = currentState
        val startDate = state.selectedStartDate
        val endDate = state.selectedEndDate
        if (
            state.shopName.isBlank() ||
            state.imageOnlyTitle.isBlank() ||
            startDate == null ||
            (state.selectedEventType != ShopEventType.STORE_RENEWAL && endDate == null)
        ) {
            showToast(Res.string.admin_registration_image_only_required)
            return
        }
        val evidence = state.evidence
        if (evidence == null) {
            showToast(Res.string.admin_registration_image_required)
            return
        }

        launchTask(taskKey = SUBMIT_TASK_KEY, loadKey = AdminRegistrationLoadKey.Submission, policy = TaskPolicy.IgnoreNew) {
            reduce { copy(message = null) }
            try {
                dataSource.registerImageOnly(
                    shopName = state.shopName,
                    title = state.imageOnlyTitle,
                    eventType = state.selectedEventType,
                    startDate = startDate,
                    endDate = if (state.selectedEventType == ShopEventType.STORE_RENEWAL) null else endDate,
                    evidence = evidence,
                )
                reduce { copy(imageOnlyTitle = "", evidence = null, message = AdminRegistrationMessage.SUCCESS) }
                loadManagedEvents()
                showToast(Res.string.admin_registration_success, ToastType.SUCCESS)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleError(exception)
                showToast(Res.string.admin_registration_register_failure)
            }
        }
    }

    private fun showToast(
        message: StringResource,
        type: ToastType = ToastType.ERROR,
    ) {
        trySideEffect(AdminRegistrationSideEffect.ShowToast(ToastData(message, type)))
    }

    private companion object {
        const val SHOP_NAMES_TASK_KEY = "admin-registration-shop-names"
        const val EXTERNAL_VENUES_TASK_KEY = "admin-registration-external-venues"
        const val SHOP_HOURS_TASK_KEY = "admin-registration-shop-hours"
        const val SUBMIT_TASK_KEY = "admin-registration-submit"
        const val MANAGED_EVENTS_TASK_KEY = "admin-managed-events"
        const val EVENT_STATUS_SAVE_TASK_KEY = "admin-event-status-save"
        const val CORRECTION_TASK_KEY = "admin-correction"
        val FORM_TABS = setOf(AdminRegistrationTab.EVENT_REGISTRATION, AdminRegistrationTab.OPERATING_NOTICE)
    }
}

private fun AdminEventStatus.requestValue(): String = if (this == AdminEventStatus.SOLD_OUT) "sold_out" else "cancelled"

private fun AdminEventStatusScope.requestValue(): String =
    when (this) {
        AdminEventStatusScope.TODAY -> "today"
        AdminEventStatusScope.ENTIRE_PERIOD -> "entire_period"
        AdminEventStatusScope.CUSTOM_PERIOD -> "custom_period"
    }

private fun OperatingNoticeType.toRequestValue(): String =
    when (this) {
        OperatingNoticeType.TEMPORARY_CLOSURE -> "full_close"
        OperatingNoticeType.EARLY_CLOSING -> "early_close"
    }

private fun createEventDraft(event: AdminManagedEvent): AdminDraft =
    AdminDraft(
        shopName = event.shopName,
        title = event.title,
        eventType = event.eventType,
        startDate = event.startDate,
        endDate = event.endDate,
        description = event.description,
    )

private fun dayKey(day: java.time.DayOfWeek): String =
    when (day) {
        java.time.DayOfWeek.MONDAY -> "mon"
        java.time.DayOfWeek.TUESDAY -> "tue"
        java.time.DayOfWeek.WEDNESDAY -> "wed"
        java.time.DayOfWeek.THURSDAY -> "thu"
        java.time.DayOfWeek.FRIDAY -> "fri"
        java.time.DayOfWeek.SATURDAY -> "sat"
        java.time.DayOfWeek.SUNDAY -> "sun"
    }

private fun normalizedEndDate(
    type: ShopEventType,
    startDate: String?,
    endDate: String?,
): String? =
    when (type) {
        ShopEventType.STORE_RENEWAL -> null
        ShopEventType.NEW_MENU -> startDate?.let { LocalDate.parse(it).plusDays(6).toString() }
        else -> endDate
    }
