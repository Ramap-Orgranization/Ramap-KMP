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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
internal fun AdminRegistrationDateRangeField(
    label: String,
    startDate: String?,
    endDate: String?,
    onDateRangeSelected: (String, String) -> Unit,
    onTodayClick: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppText(
            text = label,
            style = AppTextStyle.B1,
            color = GrayColor.C500,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = { showPicker = true },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GrayColor.C200),
                color = Color.White,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val dateRangeText =
                        formatDateRange(
                            startDate,
                            endDate,
                            stringResource(R.string.admin_registration_date_empty),
                        )
                    AppText(
                        text = dateRangeText,
                        style = AppTextStyle.B2,
                        color = if (startDate == null) GrayColor.C200 else GrayColor.C500,
                    )
                }
            }
            TextButton(
                onClick = onTodayClick,
                modifier = Modifier.height(56.dp),
            ) {
                AppText(
                    text = stringResource(R.string.admin_registration_today),
                    style = AppTextStyle.B2,
                    color = GrayColor.C500,
                )
            }
        }
    }

    if (showPicker) {
        val pickerState =
            rememberDateRangePickerState(
                initialSelectedStartDateMillis = startDate.toDateMillis(),
                initialSelectedEndDateMillis = endDate.toDateMillis(),
            )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedStartDateMillis != null,
                    onClick = {
                        pickerState.selectedStartDateMillis?.let { startMillis ->
                            val endMillis = pickerState.selectedEndDateMillis ?: startMillis
                            onDateRangeSelected(startMillis.toDateString(), endMillis.toDateString())
                        }
                        showPicker = false
                    },
                ) {
                    AppText(
                        text = stringResource(R.string.admin_registration_confirm),
                        style = AppTextStyle.B2,
                        color = GrayColor.C500,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    AppText(
                        text = stringResource(R.string.admin_registration_cancel),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                    )
                }
            },
        ) {
            DateRangePicker(
                state = pickerState,
                title = {
                    AppText(
                        text = stringResource(R.string.admin_registration_date_select),
                        style = AppTextStyle.T2,
                        color = GrayColor.C500,
                        modifier = Modifier.padding(16.dp),
                    )
                },
                showModeToggle = false,
            )
        }
    }
}

private fun formatDateRange(
    startDate: String?,
    endDate: String?,
    emptyDate: String,
): String =
    when {
        startDate == null -> emptyDate
        endDate == null || startDate == endDate -> startDate
        else -> "$startDate ~ $endDate"
    }

private fun String?.toDateMillis(): Long? =
    this?.let { value ->
        runCatching {
            LocalDate
                .parse(value)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

private fun Long.toDateString(): String =
    Instant
        .ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()

@Preview(showBackground = true)
@Composable
private fun AdminRegistrationDateRangeFieldPreview() {
    RamapTheme {
        AdminRegistrationDateRangeField(
            label = "날짜 범위",
            startDate = "2024-05-01",
            endDate = "2024-05-07",
            onDateRangeSelected = { _, _ -> },
            onTodayClick = {},
        )
    }
}
