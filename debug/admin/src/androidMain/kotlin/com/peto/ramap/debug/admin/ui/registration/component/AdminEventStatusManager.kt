package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatus
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatusScope
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.CategoryFilterChip
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminEventStatusManager(
    events: List<AdminManagedEvent>,
    today: java.time.LocalDate,
    isLoading: Boolean,
    hasFailed: Boolean,
    selectedEventId: String?,
    status: AdminEventStatus,
    scope: AdminEventStatusScope,
    reason: String,
    startDate: String?,
    endDate: String?,
    isSaving: Boolean,
    onRefresh: () -> Unit,
    onEventSelected: (String) -> Unit,
    onStatusSelected: (AdminEventStatus) -> Unit,
    onScopeSelected: (AdminEventStatusScope) -> Unit,
    onReasonChanged: (String) -> Unit,
    onDateRangeSelected: (String, String) -> Unit,
    onTodaySelected: () -> Unit,
    onSave: () -> Unit,
) {
    val filteredEvents =
        events.filter { event ->
            event.canChangeStatusOn(today) && (status == AdminEventStatus.CANCELLED || (event.occursOn(today) && !event.isCancelledOn(today)))
        }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminEventListHeader(
            title = stringResource(R.string.admin_event_status_select_event),
            isLoading = isLoading,
            hasFailed = hasFailed,
            onRefresh = onRefresh,
        )
        StatusChoices(status, onStatusSelected)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (filteredEvents.isEmpty() && !isLoading && !hasFailed) {
                item {
                    AppText(
                        text = stringResource(R.string.admin_event_status_empty),
                        style = AppTextStyle.B2,
                        color = GrayColor.C400,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(filteredEvents, key = { it.id }) { event ->
                AdminEventCard(
                    event = event,
                    today = today,
                    onClick = { onEventSelected(event.id) },
                )
            }
        }
    }
    val selectedEvent = events.firstOrNull { it.id == selectedEventId } ?: return
    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onEventSelected(selectedEvent.id) },
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { !isSaving },
            ),
        containerColor = CommonColor.White,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppText(
                    text = selectedEvent.shopName,
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                )
                AppText(
                    text = selectedEvent.title,
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                )
                StatusChoices(status, onStatusSelected)
                if (status == AdminEventStatus.CANCELLED) {
                    AppText(
                        text = stringResource(R.string.admin_event_status_cancel_scope),
                        style = AppTextStyle.B1,
                        color = GrayColor.C500,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedEvent.occursOn(today)) {
                            CategoryFilterChip(
                                label = stringResource(R.string.admin_event_status_today),
                                selected = scope == AdminEventStatusScope.TODAY,
                                onClick = { onScopeSelected(AdminEventStatusScope.TODAY) },
                            )
                        }
                        if (selectedEvent.endDate != null) {
                            CategoryFilterChip(
                                label = stringResource(R.string.admin_event_status_entire_period),
                                selected = scope == AdminEventStatusScope.ENTIRE_PERIOD,
                                onClick = { onScopeSelected(AdminEventStatusScope.ENTIRE_PERIOD) },
                            )
                        }
                        CategoryFilterChip(
                            label = stringResource(R.string.admin_event_status_custom_period),
                            selected = scope == AdminEventStatusScope.CUSTOM_PERIOD,
                            onClick = { onScopeSelected(AdminEventStatusScope.CUSTOM_PERIOD) },
                        )
                    }
                    if (scope == AdminEventStatusScope.CUSTOM_PERIOD) {
                        AdminRegistrationDateRangeField(
                            label = stringResource(R.string.admin_event_status_custom_period),
                            startDate = startDate,
                            endDate = endDate,
                            onDateRangeSelected = onDateRangeSelected,
                            onTodayClick = onTodaySelected,
                        )
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = onReasonChanged,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            AppText(
                                text = stringResource(R.string.admin_event_status_reason),
                                style = AppTextStyle.B4,
                                color = GrayColor.C400,
                            )
                        },
                    )
                }
            }
            val validPeriod = scope != AdminEventStatusScope.CUSTOM_PERIOD || (startDate != null && endDate != null && startDate <= endDate)
            AppButton(
                text = stringResource(R.string.admin_event_status_save),
                onClick = onSave,
                enabled = !isSaving && if (status == AdminEventStatus.SOLD_OUT) selectedEvent.occursOn(today) && !selectedEvent.isCancelledOn(today) && !selectedEvent.isSoldOutOn(today) else reason.isNotBlank() && validPeriod,
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatusChoices(
    status: AdminEventStatus,
    onStatusSelected: (AdminEventStatus) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryFilterChip(
            label = stringResource(R.string.admin_event_status_sold_out),
            selected = status == AdminEventStatus.SOLD_OUT,
            onClick = { onStatusSelected(AdminEventStatus.SOLD_OUT) },
        )
        CategoryFilterChip(
            label = stringResource(R.string.admin_event_status_cancelled),
            selected = status == AdminEventStatus.CANCELLED,
            onClick = { onStatusSelected(AdminEventStatus.CANCELLED) },
        )
    }
}
