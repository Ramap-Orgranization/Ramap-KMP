package com.peto.ramap.debug.admin.ui.registration

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminEvidence
import com.peto.ramap.debug.admin.ui.registration.component.AdminBottomNavigation
import com.peto.ramap.debug.admin.ui.registration.component.AdminDraftPreview
import com.peto.ramap.debug.admin.ui.registration.component.AdminEventStatusManager
import com.peto.ramap.debug.admin.ui.registration.component.AdminEventTypeSelector
import com.peto.ramap.debug.admin.ui.registration.component.AdminEvidenceField
import com.peto.ramap.debug.admin.ui.registration.component.AdminFeedbackField
import com.peto.ramap.debug.admin.ui.registration.component.AdminFieldSection
import com.peto.ramap.debug.admin.ui.registration.component.AdminNoticeTypeSelector
import com.peto.ramap.debug.admin.ui.registration.component.AdminRegistrationDateRangeField
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
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor

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
    onTabSelected: (AdminRegistrationTab) -> Unit,
) {
    val context = LocalContext.current
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val mimeType = uri?.let(context.contentResolver::getType)
            val bytes =
                uri?.let {
                    context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                }
            onEvidenceSelected(
                if (mimeType != null && mimeType in SUPPORTED_MIME_TYPES && bytes != null) {
                    AdminEvidence(bytes, mimeType)
                } else {
                    null
                },
            )
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AdminBottomNavigation(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(innerPadding)
                    .imePadding()
                    .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (uiState.selectedTab == AdminRegistrationTab.EVENT_MANAGEMENT) {
                AdminFieldSection(label = "") {
                    AdminEventStatusManager(
                        events = uiState.managedEvents,
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

                AdminRegistrationDateRangeField(
                    label = stringResource(R.string.admin_registration_date_range),
                    startDate = uiState.selectedStartDate ?: uiState.draft?.startDate,
                    endDate = uiState.selectedEndDate ?: uiState.draft?.endDate,
                    onDateRangeSelected = onDateRangeSelected,
                    onTodayClick = onTodaySelected,
                )

                AdminFieldSection(label = stringResource(R.string.admin_registration_image)) {
                    AdminEvidenceField(
                        evidence = uiState.evidence,
                        onAddClick = { imagePicker.launch("image/*") },
                        onRemoveClick = { onEvidenceSelected(null) },
                    )
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
                        backgroundColor = CommonColor.Black,
                    )
                }

                AppButton(
                    text =
                        stringResource(
                            if (uiState.isImageOnly || uiState.draft != null) {
                                R.string.admin_registration_register
                            } else {
                                R.string.admin_registration_preview
                            },
                        ),
                    onClick = onPreviewOrRegisterClick,
                    enabled = !uiState.isSubmitting,
                    isLoading = uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CommonColor.Black,
                )

                uiState.message?.let { message ->
                    AppText(
                        text = stringResource(R.string.admin_registration_success),
                        style = AppTextStyle.B2,
                        color = SystemColor.Success,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                uiState.draft?.let { draft ->
                    AdminDraftPreview(
                        draft = draft,
                        onTitleChanged = onDraftTitleChanged,
                        onDescriptionChanged = onDraftDescriptionChanged,
                    )
                }
            }
        }
    }
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
                    isOperatingNotice = true,
                    draft =
                        AdminDraft(
                            shopName = "멘야준",
                            title = "임시 휴무",
                            startDate = "2024-05-10",
                            endDate = "2024-05-10",
                            description = "내부 공사로 인한 임시 휴무입니다.",
                            sourceUrl = "https://instagram.com/p/...",
                            uncertainties = listOf("정확한 영업 재개일은 미정입니다."),
                            evidencePath = "evidence/path.jpg",
                            noticeType = "TEMPORARY_CLOSURE",
                            startTime = "11:00",
                            endTime = "21:00",
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
            onTabSelected = {},
        )
    }
}
