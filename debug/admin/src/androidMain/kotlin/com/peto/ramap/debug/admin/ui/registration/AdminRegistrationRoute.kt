package com.peto.ramap.debug.admin.ui.registration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatus
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatusScope
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationIntent
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationSideEffect
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationTab
import com.peto.ramap.designsystem.toast.ToastHost
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.ui.base.ObserveAsEvents
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AdminRegistrationRoute(
    toastManager: ToastManager = koinInject(),
    viewModel: AdminRegistrationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is AdminRegistrationSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AdminRegistrationScreen(
            uiState = uiState,
            onNoticeTypeSelected = { noticeType: OperatingNoticeType ->
                viewModel.dispatch(AdminRegistrationIntent.OnNoticeTypeSelected(noticeType))
            },
            onEventTypeSelected = { eventType: ShopEventType ->
                viewModel.dispatch(AdminRegistrationIntent.OnEventTypeSelected(eventType))
            },
            onShopNameChanged = { viewModel.dispatch(AdminRegistrationIntent.OnShopNameChanged(it)) },
            onSourceUrlChanged = { viewModel.dispatch(AdminRegistrationIntent.OnSourceUrlChanged(it)) },
            onFeedbackChanged = { viewModel.dispatch(AdminRegistrationIntent.OnFeedbackChanged(it)) },
            onImageOnlyRegistrationClick = { viewModel.dispatch(AdminRegistrationIntent.OnImageOnlyRegistrationClicked) },
            onImageOnlyTitleChanged = { viewModel.dispatch(AdminRegistrationIntent.OnImageOnlyTitleChanged(it)) },
            onDraftTitleChanged = { viewModel.dispatch(AdminRegistrationIntent.OnDraftTitleChanged(it)) },
            onDraftDescriptionChanged = { viewModel.dispatch(AdminRegistrationIntent.OnDraftDescriptionChanged(it)) },
            onEvidenceSelected = { viewModel.dispatch(AdminRegistrationIntent.OnEvidenceSelected(it)) },
            onDateRangeSelected = { startDate, endDate ->
                viewModel.dispatch(AdminRegistrationIntent.OnDateRangeSelected(startDate, endDate))
            },
            onTodaySelected = { viewModel.dispatch(AdminRegistrationIntent.OnTodaySelected) },
            onPreviewOrRegisterClick = { viewModel.dispatch(AdminRegistrationIntent.OnPreviewOrRegisterClicked) },
            onManagedEventsRefresh = { viewModel.dispatch(AdminRegistrationIntent.OnManagedEventsRefreshed) },
            onManagedEventSelected = { viewModel.dispatch(AdminRegistrationIntent.OnManagedEventSelected(it)) },
            onEventStatusSelected = { status: AdminEventStatus ->
                viewModel.dispatch(AdminRegistrationIntent.OnEventStatusSelected(status))
            },
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
            onTabSelected = { tab: AdminRegistrationTab ->
                viewModel.dispatch(AdminRegistrationIntent.OnTabSelected(tab))
            },
        )
        ToastHost(toastManager = toastManager)
    }
}
