package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatus
import com.peto.ramap.debug.admin.ui.registration.contract.AdminEventStatusScope
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.CategoryFilterChip
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_refresh

@Composable
internal fun AdminEventStatusManager(
    events: List<AdminManagedEvent>,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusToggle(
                    selected = status == AdminEventStatus.SOLD_OUT,
                    onClick = { onStatusSelected(AdminEventStatus.SOLD_OUT) },
                    label = stringResource(R.string.admin_event_status_sold_out),
                )
                StatusToggle(
                    selected = status == AdminEventStatus.CANCELLED,
                    onClick = { onStatusSelected(AdminEventStatus.CANCELLED) },
                    label = stringResource(R.string.admin_event_status_cancelled),
                )
            }
        }

        // Event Selection Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = stringResource(R.string.admin_event_status_select_event),
                    style = AppTextStyle.B1,
                    color = GrayColor.C500,
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.admin_event_status_refresh),
                        tint = GrayColor.C400,
                    )
                }
            }

            val filteredEvents =
                events.filter { event ->
                    when (status) {
                        AdminEventStatus.SOLD_OUT -> event.cancelledDates.isEmpty() || event.soldOutDates.isNotEmpty()
                        AdminEventStatus.CANCELLED -> event.soldOutDates.isEmpty() || event.cancelledDates.isNotEmpty()
                    }
                }

            if (filteredEvents.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = stringResource(R.string.admin_event_status_empty),
                        style = AppTextStyle.B3,
                        color = GrayColor.C300,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredEvents.forEach { event ->
                        AdminEventCard(
                            event = event,
                            selected = selectedEventId == event.id,
                            onClick = { onEventSelected(event.id) },
                            currentStatus = status,
                        )
                    }
                }
            }
        }

        // Configuration Section
        if (selectedEventId != null) {
            val selectedEvent = events.firstOrNull { it.id == selectedEventId }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(color = GrayColor.C100)

                if (status == AdminEventStatus.CANCELLED) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppText(
                            text = stringResource(R.string.admin_event_status_cancel_scope),
                            style = AppTextStyle.B1,
                            color = GrayColor.C500,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryFilterChip(
                                label = stringResource(R.string.admin_event_status_today),
                                selected = scope == AdminEventStatusScope.TODAY,
                                onClick = { onScopeSelected(AdminEventStatusScope.TODAY) },
                            )
                            if (selectedEvent?.endDate != null) {
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
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            AppText(
                                text = stringResource(R.string.admin_event_status_reason),
                                style = AppTextStyle.B4,
                                color = GrayColor.C400,
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                AppButton(
                    text = stringResource(R.string.admin_event_status_save),
                    onClick = onSave,
                    enabled = (status == AdminEventStatus.SOLD_OUT || reason.isNotBlank()) && !isSaving,
                    isLoading = isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CommonColor.Black,
                )
            }
        }
    }
}

@Composable
private fun StatusToggle(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) GrayColor.C500 else CommonColor.White,
        border = BorderStroke(1.dp, if (selected) GrayColor.C500 else GrayColor.C200),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = label,
                style = AppTextStyle.B2,
                color = if (selected) CommonColor.White else GrayColor.C400,
            )
        }
    }
}

@Composable
private fun AdminEventCard(
    event: AdminManagedEvent,
    selected: Boolean,
    onClick: () -> Unit,
    currentStatus: AdminEventStatus,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) GrayColor.C050 else CommonColor.White,
        border = BorderStroke(1.dp, if (selected) GrayColor.C500 else GrayColor.C100),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = event.shopName,
                    style = AppTextStyle.B2,
                    color = if (selected) GrayColor.C500 else GrayColor.C400,
                )
                val hasRelevantStatus =
                    when (currentStatus) {
                        AdminEventStatus.SOLD_OUT -> event.soldOutDates.isNotEmpty()
                        AdminEventStatus.CANCELLED -> event.cancelledDates.isNotEmpty()
                    }
                if (hasRelevantStatus) {
                    NewsBadge(
                        text =
                            stringResource(
                                if (currentStatus == AdminEventStatus.CANCELLED) {
                                    R.string.admin_event_status_badge_cancelled
                                } else {
                                    R.string.admin_event_status_badge_sold_out
                                },
                            ),
                        textStyle = AppTextStyle.C2,
                        containerColor = SystemColor.Warning,
                        contentColor = CommonColor.White,
                    )
                }
            }
            AppText(
                text = event.title,
                style = AppTextStyle.T2,
                color = if (selected) GrayColor.C500 else GrayColor.C400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AppText(
                text = "${event.startDate} ~ ${event.endDate ?: event.startDate}",
                style = AppTextStyle.B4,
                color = GrayColor.C300,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminEventStatusManagerPreview() {
    RamapTheme {
        AdminEventStatusManager(
            events =
                listOf(
                    AdminManagedEvent(
                        id = "1",
                        title = "이벤트 1",
                        startDate = "2024-05-01",
                        shopName = "오레노라멘",
                    ),
                ),
            selectedEventId = "1",
            status = AdminEventStatus.SOLD_OUT,
            scope = AdminEventStatusScope.TODAY,
            reason = "",
            startDate = null,
            endDate = null,
            isSaving = false,
            onRefresh = {},
            onEventSelected = {},
            onStatusSelected = {},
            onScopeSelected = {},
            onReasonChanged = {},
            onDateRangeSelected = { _, _ -> },
            onTodaySelected = {},
            onSave = {},
        )
    }
}
