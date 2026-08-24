package com.peto.ramap.debug.admin.ui.registration

import androidx.lifecycle.viewModelScope
import com.peto.ramap.debug.admin.data.datasource.AdminRegistrationDataSource
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatus
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatusScope
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationIntent
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
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

internal class AdminRegistrationViewModel(
    private val dataSource: AdminRegistrationDataSource,
) : BaseViewModel<AdminRegistrationUiState, AdminRegistrationIntent, AdminRegistrationSideEffect>(AdminRegistrationUiState()) {
    init {
        loadShopNames()
        loadManagedEvents()
    }

    override suspend fun handleIntent(intent: AdminRegistrationIntent) {
        when (intent) {
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
                    copy(
                        selectedNoticeType = intent.noticeType,
                        draft = draft?.copy(noticeType = intent.noticeType.toRequestValue()),
                        message = null,
                    )
                }

            is AdminRegistrationIntent.OnEventTypeSelected ->
                reduce {
                    copy(
                        selectedEventType = intent.eventType,
                        selectedEndDate = if (intent.eventType == ShopEventType.STORE_RENEWAL) null else selectedEndDate,
                        draft =
                            draft?.copy(
                                endDate = if (intent.eventType == ShopEventType.STORE_RENEWAL) null else draft.endDate,
                            ),
                        message = null,
                    )
                }

            is AdminRegistrationIntent.OnShopNameChanged -> reduce { copy(shopName = intent.value, draft = null, message = null) }
            is AdminRegistrationIntent.OnSourceUrlChanged -> reduce { copy(sourceUrl = intent.value, draft = null, message = null) }
            is AdminRegistrationIntent.OnFeedbackChanged -> reduce { copy(feedback = intent.value, draft = null, message = null) }
            AdminRegistrationIntent.OnImageOnlyRegistrationClicked ->
                if (!currentState.isOperatingNotice) {
                    reduce { copy(isImageOnly = !isImageOnly, draft = null, message = null) }
                }
            is AdminRegistrationIntent.OnImageOnlyTitleChanged -> reduce { copy(imageOnlyTitle = intent.value, message = null) }
            is AdminRegistrationIntent.OnDraftTitleChanged ->
                reduce { copy(draft = draft?.copy(title = intent.value), message = null) }
            is AdminRegistrationIntent.OnDraftDescriptionChanged ->
                reduce { copy(draft = draft?.copy(description = intent.value), message = null) }
            is AdminRegistrationIntent.OnEvidenceSelected -> reduce { copy(evidence = intent.evidence, draft = null, message = null) }
            is AdminRegistrationIntent.OnDateRangeSelected ->
                reduce {
                    copy(
                        selectedStartDate = intent.startDate,
                        selectedEndDate = intent.endDate,
                        draft = draft?.copy(startDate = intent.startDate, endDate = intent.endDate),
                        message = null,
                    )
                }

            AdminRegistrationIntent.OnTodaySelected -> selectToday()
            AdminRegistrationIntent.OnPreviewOrRegisterClicked -> previewOrRegister()
            AdminRegistrationIntent.OnManagedEventsRefreshed -> loadManagedEvents()
            is AdminRegistrationIntent.OnManagedEventSelected ->
                reduce {
                    copy(
                        selectedManagedEventId = if (selectedManagedEventId == intent.eventId) null else intent.eventId,
                        eventStatusReason = "",
                        eventStatusStartDate = null,
                        eventStatusEndDate = null,
                    )
                }
            is AdminRegistrationIntent.OnEventStatusSelected ->
                reduce {
                    copy(
                        selectedEventStatus = intent.status,
                        selectedEventStatusScope = AdminEventStatusScope.TODAY,
                        eventStatusReason = "",
                        selectedManagedEventId = null,
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
            is AdminRegistrationIntent.OnTabSelected -> selectTab(intent.tab)
        }
    }

    private fun selectTab(tab: AdminRegistrationTab) {
        reduce {
            copy(
                selectedTab = tab,
                isOperatingNotice = tab == AdminRegistrationTab.OPERATING_NOTICE,
                selectedNoticeType = null,
                selectedEventType = ShopEventType.LIMITED_MENU,
                isImageOnly = false,
                imageOnlyTitle = "",
                draft = null,
                selectedStartDate = null,
                selectedEndDate = null,
                message = null,
                selectedManagedEventId = null,
                eventStatusReason = "",
                eventStatusStartDate = null,
                eventStatusEndDate = null,
                selectedEventStatus = AdminEventStatus.SOLD_OUT,
                selectedEventStatusScope = AdminEventStatusScope.TODAY,
            )
        }
    }

    private fun selectToday() {
        val today = LocalDate.now().toString()
        reduce {
            copy(
                selectedStartDate = today,
                selectedEndDate = today,
                draft = draft?.copy(startDate = today, endDate = today),
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
            } catch (_: Throwable) {
                showToast(Res.string.admin_registration_shop_names_load_failure)
            }
        }
    }

    private fun loadManagedEvents() {
        launchTask(taskKey = MANAGED_EVENTS_TASK_KEY, policy = TaskPolicy.CancelPrevious) {
            try {
                val events = dataSource.fetchManagedEvents()
                reduce { copy(managedEvents = events) }
            } catch (_: Throwable) {
                showToast(Res.string.admin_registration_managed_events_load_failure)
            }
        }
    }

    private fun saveEventStatus() {
        val eventId = currentState.selectedManagedEventId
        if (eventId == null) {
            showToast(Res.string.admin_event_status_event_required)
            return
        }
        val reason = currentState.eventStatusReason.trim().takeIf { currentState.selectedEventStatus == AdminEventStatus.CANCELLED }
        if (currentState.selectedEventStatus == AdminEventStatus.CANCELLED && reason.isNullOrEmpty()) {
            showToast(Res.string.admin_event_status_reason_required)
            return
        }
        if (currentState.selectedEventStatusScope == AdminEventStatusScope.CUSTOM_PERIOD &&
            (currentState.eventStatusStartDate == null || currentState.eventStatusEndDate == null)
        ) {
            showToast(Res.string.admin_event_status_period_required)
            return
        }
        launchTask(taskKey = EVENT_STATUS_SAVE_TASK_KEY, policy = TaskPolicy.IgnoreNew) {
            reduce { copy(isSavingEventStatus = true) }
            try {
                dataSource.saveEventStatus(
                    eventId = eventId,
                    status = currentState.selectedEventStatus.requestValue(),
                    scope =
                        if (currentState.selectedEventStatus ==
                            AdminEventStatus.SOLD_OUT
                        ) {
                            "today"
                        } else {
                            currentState.selectedEventStatusScope.requestValue()
                        },
                    reason = reason,
                    startDate = currentState.eventStatusStartDate,
                    endDate = currentState.eventStatusEndDate,
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
                reduce { copy(message = AdminRegistrationMessage.SUCCESS) }
            } catch (_: Throwable) {
                showToast(Res.string.admin_event_status_save_failure)
            } finally {
                reduce { copy(isSavingEventStatus = false) }
            }
        }
    }

    private fun selectEventStatusToday() {
        val today = LocalDate.now().toString()
        reduce { copy(eventStatusStartDate = today, eventStatusEndDate = today) }
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
            policy = TaskPolicy.IgnoreNew,
        ) {
            reduce { copy(isSubmitting = true, message = null) }
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
                    copy(
                        draft =
                            result.copy(
                                shopName = result.shopName ?: currentState.shopName.ifBlank { null },
                                sourceUrl = result.sourceUrl ?: currentState.sourceUrl.ifBlank { null },
                                startDate = selectedStartDate ?: result.startDate,
                                endDate =
                                    if (selectedEventType == ShopEventType.STORE_RENEWAL) {
                                        null
                                    } else {
                                        selectedEndDate ?: result.endDate
                                    },
                                noticeType = selectedNoticeType?.toRequestValue() ?: result.noticeType,
                            ),
                        message = null,
                    )
                }
            } catch (_: Throwable) {
                showToast(Res.string.admin_registration_preview_failure)
            } finally {
                reduce { copy(isSubmitting = false) }
            }
        }
    }

    private fun register() {
        val draft = currentState.draft
        if (draft == null) {
            showToast(Res.string.admin_registration_draft_required)
            return
        }
        launchTask(
            taskKey = SUBMIT_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
        ) {
            reduce { copy(isSubmitting = true, message = null) }
            try {
                dataSource.register(draft, currentState.isOperatingNotice, currentState.selectedEventType)
                reduce { copy(draft = null, message = AdminRegistrationMessage.SUCCESS) }
                showToast(Res.string.admin_registration_success, ToastType.SUCCESS)
            } catch (_: Throwable) {
                showToast(Res.string.admin_registration_register_failure)
            } finally {
                reduce { copy(isSubmitting = false) }
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

        launchTask(taskKey = SUBMIT_TASK_KEY, policy = TaskPolicy.IgnoreNew) {
            reduce { copy(isSubmitting = true, message = null) }
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
                showToast(Res.string.admin_registration_success, ToastType.SUCCESS)
            } catch (_: Throwable) {
                showToast(Res.string.admin_registration_register_failure)
            } finally {
                reduce { copy(isSubmitting = false) }
            }
        }
    }

    private fun showToast(
        message: StringResource,
        type: ToastType = ToastType.ERROR,
    ) {
        viewModelScope.launch {
            trySideEffect(AdminRegistrationSideEffect.ShowToast(ToastData(message, type)))
        }
    }

    private companion object {
        const val SHOP_NAMES_TASK_KEY = "admin-registration-shop-names"
        const val SUBMIT_TASK_KEY = "admin-registration-submit"
        const val MANAGED_EVENTS_TASK_KEY = "admin-managed-events"
        const val EVENT_STATUS_SAVE_TASK_KEY = "admin-event-status-save"
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
        OperatingNoticeType.OPERATING_NOTICE -> "operating_notice"
        OperatingNoticeType.TEMPORARY_CLOSURE -> "full_close"
        OperatingNoticeType.EARLY_CLOSING -> "early_close"
        OperatingNoticeType.LATE_OPENING -> "late_opening"
    }
