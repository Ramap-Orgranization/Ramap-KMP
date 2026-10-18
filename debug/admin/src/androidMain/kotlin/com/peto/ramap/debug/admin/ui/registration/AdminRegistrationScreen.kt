package com.peto.ramap.debug.admin.ui.registration

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminEvidence
import com.peto.ramap.debug.admin.ui.registration.component.AdminBottomNavigation
import com.peto.ramap.debug.admin.ui.registration.component.AdminCorrectionManager
import com.peto.ramap.debug.admin.ui.registration.component.AdminDraftPreview
import com.peto.ramap.debug.admin.ui.registration.component.AdminEventEditManager
import com.peto.ramap.debug.admin.ui.registration.component.AdminEventStatusManager
import com.peto.ramap.debug.admin.ui.registration.component.AdminEventTypeSelector
import com.peto.ramap.debug.admin.ui.registration.component.AdminEvidenceField
import com.peto.ramap.debug.admin.ui.registration.component.AdminFeedbackField
import com.peto.ramap.debug.admin.ui.registration.component.AdminFieldSection
import com.peto.ramap.debug.admin.ui.registration.component.AdminNoticeTimeFields
import com.peto.ramap.debug.admin.ui.registration.component.AdminNoticeTypeSelector
import com.peto.ramap.debug.admin.ui.registration.component.AdminRegistrationDateRangeField
import com.peto.ramap.debug.admin.ui.registration.component.AdminRegularScheduleSelector
import com.peto.ramap.debug.admin.ui.registration.component.AdminShopNameField
import com.peto.ramap.debug.admin.ui.registration.component.AdminSourceField
import com.peto.ramap.debug.admin.ui.registration.component.AdminTitleField
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatus
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatusScope
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationTab
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationUiState
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AdminRegistrationScreen(
    uiState: AdminRegistrationUiState,
    onNoticeTypeSelected: (OperatingNoticeType) -> Unit,
    onEventTypeSelected: (ShopEventType) -> Unit,
    onShopNameChanged: (String) -> Unit,
    onSourceUrlChanged: (String) -> Unit,
    onFeedbackChanged: (String) -> Unit,
    onImageOnlyRegistrationClick: () -> Unit,
    onImageOnlyTitleChanged: (String) -> Unit,
    onDraftTitleChanged: (String) -> Unit,
    onDraftDescriptionChanged: (String) -> Unit,
    onDraftVenueChanged: (String, String, String, String, String, String) -> Unit,
    onDraftNoticeTimesChanged: (String?, String?) -> Unit,
    onRegularSegmentSelected: (String, String) -> Unit,
    onEvidenceSelected: (AdminEvidence?) -> Unit,
    onDateRangeSelected: (String, String) -> Unit,
    onTodaySelected: () -> Unit,
    onPreviewOrRegisterClick: () -> Unit,
    onManagedEventsRefresh: () -> Unit,
    onManagedEventSelected: (String) -> Unit,
    onEventStatusSelected: (AdminEventStatus) -> Unit,
    onEventStatusScopeSelected: (AdminEventStatusScope) -> Unit,
    onEventStatusReasonChanged: (String) -> Unit,
    onEventStatusDateRangeSelected: (String, String) -> Unit,
    onEventStatusTodaySelected: () -> Unit,
    onEventStatusSave: () -> Unit,
    onManagedEventEdit: (String) -> Unit,
    onCorrectionRequestChanged: (String) -> Unit,
    onCorrectionPreviewRequested: () -> Unit,
    onCorrectionConfirmed: () -> Unit,
    onCorrectionPreviewDismissed: () -> Unit,
    onTabSelected: (AdminRegistrationTab) -> Unit,
    onEvidenceReadFailed: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val evidence =
                        withContext(Dispatchers.IO) {
                            val mimeType = context.contentResolver.getType(uri)
                            require(mimeType in SUPPORTED_MIME_TYPES)
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            require(bytes != null && bytes.isNotEmpty())
                            AdminEvidence(bytes, requireNotNull(mimeType))
                        }
                    onEvidenceSelected(evidence)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    onEvidenceReadFailed()
                }
            }
        }

    val isForm =
        uiState.selectedTab == AdminRegistrationTab.EVENT_REGISTRATION ||
            uiState.selectedTab == AdminRegistrationTab.OPERATING_NOTICE ||
            uiState.editingEventId != null
    BackHandler(enabled = uiState.editingEventId != null) {
        if (!uiState.isBusy) onTabSelected(AdminRegistrationTab.EVENT_EDIT)
    }
    Scaffold(
        containerColor = GrayColor.C050,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(
                modifier = Modifier.imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isForm) {
                    Column(
                        modifier =
                            Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        AppButton(
                            text =
                                stringResource(
                                    if (uiState.isImageOnly || uiState.draft != null) {
                                        if (uiState.editingEventId == null) R.string.admin_registration_register else R.string.admin_event_edit
                                    } else {
                                        R.string.admin_registration_preview
                                    },
                                ),
                            onClick = onPreviewOrRegisterClick,
                            enabled = !uiState.isSubmitting && (!uiState.isOperatingNotice || uiState.draft?.canRegisterOperatingNotice() != false),
                            isLoading = uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = CommonColor.Black,
                        )
                    }
                }
                AdminBottomNavigation(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = onTabSelected,
                    enabled = !uiState.isBusy,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            key(uiState.selectedTab, uiState.editingEventId) {
                Column(
                    modifier =
                        Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                            .then(if (isForm) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    AppText(
                        text =
                            stringResource(
                                when (uiState.selectedTab) {
                                    AdminRegistrationTab.EVENT_MANAGEMENT -> R.string.admin_tab_event_management
                                    AdminRegistrationTab.EVENT_EDIT -> R.string.admin_tab_event_edit
                                    AdminRegistrationTab.EVENT_REGISTRATION -> R.string.admin_tab_event_registration
                                    AdminRegistrationTab.OPERATING_NOTICE -> R.string.admin_tab_operating_notice
                                },
                            ),
                        style = AppTextStyle.T1,
                        color = GrayColor.C500,
                    )
                    if (uiState.editingEventId != null) {
                        AppButton(
                            text = stringResource(R.string.admin_registration_cancel),
                            onClick = { onTabSelected(AdminRegistrationTab.EVENT_EDIT) },
                            enabled = !uiState.isBusy,
                            backgroundColor = CommonColor.White,
                            textColor = GrayColor.C500,
                        )
                    }
                    if (uiState.selectedTab == AdminRegistrationTab.EVENT_MANAGEMENT) {
                        AdminFieldSection(label = "") {
                            AdminEventStatusManager(
                                events = uiState.managedEvents,
                                today = uiState.today,
                                isLoading = uiState.isLoadingEvents,
                                hasFailed = uiState.managedEventsLoadFailed,
                                selectedEventId = uiState.selectedManagedEventId,
                                status = uiState.selectedEventStatus,
                                scope = uiState.selectedEventStatusScope,
                                reason = uiState.eventStatusReason,
                                startDate = uiState.eventStatusStartDate,
                                endDate = uiState.eventStatusEndDate,
                                isSaving = uiState.isSavingEventStatus,
                                onRefresh = onManagedEventsRefresh,
                                onEventSelected = onManagedEventSelected,
                                onStatusSelected = onEventStatusSelected,
                                onScopeSelected = onEventStatusScopeSelected,
                                onReasonChanged = onEventStatusReasonChanged,
                                onDateRangeSelected = onEventStatusDateRangeSelected,
                                onTodaySelected = onEventStatusTodaySelected,
                                onSave = onEventStatusSave,
                            )
                        }
                    } else if (uiState.selectedTab == AdminRegistrationTab.EVENT_EDIT && uiState.editingEventId == null) {
                        AdminEventEditManager(
                            events = uiState.managedEvents,
                            today = uiState.today,
                            isLoading = uiState.isLoadingEvents,
                            hasFailed = uiState.managedEventsLoadFailed,
                            onRefresh = onManagedEventsRefresh,
                            onEdit = onManagedEventEdit,
                            correction = {
                                AdminFieldSection(label = "") {
                                    AdminCorrectionManager(
                                        request = uiState.correctionRequest,
                                        preview = uiState.correctionPreview,
                                        isLoading = uiState.isCorrecting,
                                        onRequestChanged = onCorrectionRequestChanged,
                                        onPreviewRequested = onCorrectionPreviewRequested,
                                        onConfirm = onCorrectionConfirmed,
                                        onDismiss = onCorrectionPreviewDismissed,
                                    )
                                }
                            },
                        )
                    } else {
                        if (uiState.isOperatingNotice) {
                            AdminFieldSection(label = stringResource(R.string.admin_registration_detailed_classification)) {
                                AdminNoticeTypeSelector(
                                    selectedNoticeType =
                                        uiState.selectedNoticeType
                                            ?: uiState.draft?.noticeType?.let(::toOperatingNoticeType),
                                    onNoticeTypeSelected = onNoticeTypeSelected,
                                )
                            }
                        } else {
                            AdminFieldSection(label = stringResource(R.string.admin_registration_event_type)) {
                                AdminEventTypeSelector(
                                    selectedEventType = uiState.selectedEventType,
                                    onEventTypeSelected = onEventTypeSelected,
                                )
                            }
                        }

                        AdminFieldSection(label = stringResource(R.string.admin_registration_shop)) {
                            AdminShopNameField(
                                shopName = uiState.shopName,
                                shopNames = uiState.shopNames,
                                onShopNameChanged = onShopNameChanged,
                                readOnly = uiState.editingEventId != null,
                            )
                        }

                        if (!uiState.isOperatingNotice && uiState.isImageOnly) {
                            AdminFieldSection(label = stringResource(R.string.admin_registration_title_label)) {
                                AdminTitleField(
                                    title = uiState.imageOnlyTitle,
                                    onTitleChanged = onImageOnlyTitleChanged,
                                )
                            }
                        } else {
                            if (uiState.editingEventId == null) {
                                AdminFieldSection(label = stringResource(R.string.admin_registration_source)) {
                                    AdminSourceField(
                                        sourceUrl = uiState.sourceUrl,
                                        onSourceUrlChanged = onSourceUrlChanged,
                                    )
                                }
                                AdminFieldSection(label = stringResource(R.string.admin_registration_feedback)) {
                                    AdminFeedbackField(
                                        feedback = uiState.feedback,
                                        onFeedbackChanged = onFeedbackChanged,
                                    )
                                }
                            }
                        }

                        AdminRegistrationDateRangeField(
                            label = stringResource(R.string.admin_registration_date_range),
                            startDate = uiState.selectedStartDate ?: uiState.draft?.startDate,
                            endDate = uiState.selectedEndDate ?: uiState.draft?.endDate,
                            onDateRangeSelected = onDateRangeSelected,
                            onTodayClick = onTodaySelected,
                        )

                        if (uiState.editingEventId == null) {
                            AdminFieldSection(label = stringResource(R.string.admin_registration_image)) {
                                AdminEvidenceField(
                                    evidence = uiState.evidence,
                                    onAddClick = { imagePicker.launch("image/*") },
                                    onRemoveClick = { onEvidenceSelected(null) },
                                )
                            }
                        }

                        if (!uiState.isOperatingNotice && uiState.draft == null) {
                            AppButton(
                                text =
                                    stringResource(
                                        if (uiState.isImageOnly) {
                                            R.string.admin_registration_url_mode
                                        } else {
                                            R.string.admin_registration_image_only
                                        },
                                    ),
                                onClick = onImageOnlyRegistrationClick,
                                enabled = !uiState.isSubmitting,
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = CommonColor.White,
                                textColor = GrayColor.C500,
                            )
                        }

                        uiState.message?.let { message ->
                            AppText(
                                text = stringResource(R.string.admin_registration_success),
                                style = AppTextStyle.B2,
                                color = SystemColor.Success,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        uiState.draft?.let { draft ->
                            if (uiState.isOperatingNotice && draft.noticeType in setOf("early_close", "planned_close")) {
                                val dayKey =
                                    (uiState.selectedStartDate ?: draft.startDate)
                                        ?.let { java.time.LocalDate.parse(it) }
                                        ?.dayOfWeek
                                        ?.let(::dayKey)
                                uiState.selectedShopHours?.weekly?.get(dayKey)?.let { regularDay ->
                                    AdminFieldSection(label = stringResource(R.string.admin_notice_regular_segments)) {
                                        AdminRegularScheduleSelector(
                                            day = regularDay.copy(breakTimes = uiState.selectedShopHours.breakTimes[dayKey].orEmpty()),
                                            onSegmentSelected = onRegularSegmentSelected,
                                        )
                                    }
                                }
                                AdminNoticeTimeFields(
                                    type = uiState.selectedNoticeType ?: draft.noticeType?.let(::toOperatingNoticeType),
                                    endTime = draft.endTime,
                                    isError = !draft.canRegisterOperatingNotice(),
                                    onTimesChanged = onDraftNoticeTimesChanged,
                                )
                            }
                            AdminDraftPreview(
                                draft = draft,
                                showVenueFields = !uiState.isOperatingNotice && uiState.editingEventId == null,
                                businessHours = uiState.selectedShopHours?.toDomain(),
                                onTitleChanged = onDraftTitleChanged,
                                onDescriptionChanged = onDraftDescriptionChanged,
                                externalVenues = uiState.externalVenues,
                                onVenueChanged = onDraftVenueChanged,
                            )
                        }
                    }
                }
            }
        }
    }
}

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

private val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png")

@Preview(showBackground = true)
@Composable
private fun AdminRegistrationScreenPreview() {
    RamapTheme {
        AdminRegistrationScreen(
            uiState =
                AdminRegistrationUiState(
                    shopNames = listOf("오레노라멘", "멘야준", "라멘베라보"),
                    shopName = "오레노라멘",
                    sourceUrl = "https://instagram.com/...",
                    feedback = "맛있어요",
                    selectedStartDate = "2024-05-01",
                    selectedEndDate = "2024-05-07",
                ),
            onNoticeTypeSelected = {},
            onEventTypeSelected = {},
            onShopNameChanged = {},
            onSourceUrlChanged = {},
            onFeedbackChanged = {},
            onImageOnlyRegistrationClick = {},
            onImageOnlyTitleChanged = {},
            onDraftTitleChanged = {},
            onDraftDescriptionChanged = {},
            onDraftVenueChanged = { _, _, _, _, _, _ -> },
            onDraftNoticeTimesChanged = { _, _ -> },
            onRegularSegmentSelected = { _, _ -> },
            onEvidenceSelected = {},
            onDateRangeSelected = { _, _ -> },
            onTodaySelected = {},
            onPreviewOrRegisterClick = {},
            onManagedEventsRefresh = {},
            onManagedEventSelected = {},
            onEventStatusSelected = {},
            onEventStatusScopeSelected = {},
            onEventStatusReasonChanged = {},
            onEventStatusDateRangeSelected = { _, _ -> },
            onEventStatusTodaySelected = {},
            onEventStatusSave = {},
            onManagedEventEdit = {},
            onCorrectionRequestChanged = {},
            onCorrectionPreviewRequested = {},
            onCorrectionConfirmed = {},
            onCorrectionPreviewDismissed = {},
            onTabSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminRegistrationScreenWithDraftPreview() {
    RamapTheme {
        AdminRegistrationScreen(
            uiState =
                AdminRegistrationUiState(
                    shopName = "멘야준",
                    selectedEventType = ShopEventType.POPUP,
                    draft =
                        AdminDraft(
                            shopName = "멘야준",
                            title = "라멘 페스티벌 팝업",
                            eventType = "POPUP",
                            venueName = "라멘 페스티벌",
                            venueInstagramUrl = "https://www.instagram.com/ramen_festival/",
                            venueNaverMapUrl = "https://map.naver.com/p/entry/place/1",
                            venueKakaoMapUrl = "https://place.map.kakao.com/1",
                            startDate = "2024-05-10",
                            endDate = "2024-05-16",
                            description = "특별한 팝업 이벤트입니다.",
                            sourceUrl = "https://instagram.com/p/...",
                            uncertainties = listOf("운영 시간은 변동될 수 있습니다."),
                            evidencePath = "evidence/path.jpg",
                        ),
                ),
            onNoticeTypeSelected = {},
            onEventTypeSelected = {},
            onShopNameChanged = {},
            onSourceUrlChanged = {},
            onFeedbackChanged = {},
            onImageOnlyRegistrationClick = {},
            onImageOnlyTitleChanged = {},
            onDraftTitleChanged = {},
            onDraftDescriptionChanged = {},
            onDraftVenueChanged = { _, _, _, _, _, _ -> },
            onDraftNoticeTimesChanged = { _, _ -> },
            onRegularSegmentSelected = { _, _ -> },
            onEvidenceSelected = {},
            onDateRangeSelected = { _, _ -> },
            onTodaySelected = {},
            onPreviewOrRegisterClick = {},
            onManagedEventsRefresh = {},
            onManagedEventSelected = {},
            onEventStatusSelected = {},
            onEventStatusScopeSelected = {},
            onEventStatusReasonChanged = {},
            onEventStatusDateRangeSelected = { _, _ -> },
            onEventStatusTodaySelected = {},
            onEventStatusSave = {},
            onManagedEventEdit = {},
            onCorrectionRequestChanged = {},
            onCorrectionPreviewRequested = {},
            onCorrectionConfirmed = {},
            onCorrectionPreviewDismissed = {},
            onTabSelected = {},
        )
    }
}
