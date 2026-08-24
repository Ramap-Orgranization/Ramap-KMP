package com.peto.ramap.debug.admin.ui.registration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatus
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatusScope
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationIntent
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationTab
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AdminRegistrationRoute(viewModel: AdminRegistrationViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AdminRegistrationScreen(
        uiState = uiState,
        onNoticeTypeSelected = { noticeType: OperatingNoticeType ->
            viewModel.dispatch(AdminRegistrationIntent.OnNoticeTypeSelected(noticeType))
        },
        onShopNameChanged = { viewModel.dispatch(AdminRegistrationIntent.OnShopNameChanged(it)) },
        onSourceUrlChanged = { viewModel.dispatch(AdminRegistrationIntent.OnSourceUrlChanged(it)) },
        onFeedbackChanged = { viewModel.dispatch(AdminRegistrationIntent.OnFeedbackChanged(it)) },
        onDraftDescriptionChanged = { viewModel.dispatch(AdminRegistrationIntent.OnDraftDescriptionChanged(it)) },
        onEvidenceSelected = { viewModel.dispatch(AdminRegistrationIntent.OnEvidenceSelected(it)) },
        onDateRangeSelected = { startDate, endDate ->
            viewModel.dispatch(AdminRegistrationIntent.OnDateRangeSelected(startDate, endDate))
        },
        onTodaySelected = { viewModel.dispatch(AdminRegistrationIntent.OnTodaySelected) },
        onPreviewOrRegisterClick = { viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked) },
        onManagedEventsRefresh = { viewModel.dispatch(AdminRegistrationIntent.OnManagedEventsRefreshed) },
        onManagedEventSelected = { viewModel.dispatch(AdminRegistrationIntent.OnManagedEventSelected(it)) },
        onEventStatusSelected = { status: AdminEventStatus -> viewModel.dispatch(AdminRegistrationIntent.OnEventStatusSelected(status)) },
        onEventStatusScopeSelected = { scope: AdminEventStatusScope ->
            viewModel.dispatch(AdminRegistrationIntent.OnEventStatusScopeSelected(scope))
        },
        onEventStatusReasonChanged = { viewModel.dispatch(AdminRegistrationIntent.OnEventStatusReasonChanged(it)) },
        onEventStatusDateRangeSelected = {
            startDate,
            endDate,
            ->
            viewModel.dispatch(AdminRegistrationIntent.OnEventStatusDateRangeSelected(startDate, endDate))
        },
        onEventStatusTodaySelected = { viewModel.dispatch(AdminRegistrationIntent.OnEventStatusTodaySelected) },
        onEventStatusSave = { viewModel.dispatch(AdminRegistrationIntent.OnEventStatusSaved) },
        onTabSelected = { tab: AdminRegistrationTab -> viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(tab)) },
    )
}
