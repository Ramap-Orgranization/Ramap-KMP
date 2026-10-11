package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import java.time.LocalDate

@Composable
internal fun AdminEventEditManager(
    events: List<AdminManagedEvent>,
    today: LocalDate,
    isLoading: Boolean,
    hasFailed: Boolean,
    onRefresh: () -> Unit,
    onEdit: (String) -> Unit,
    correction: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminEventListHeader(
            title = stringResource(R.string.admin_event_edit_select),
            isLoading = isLoading,
            hasFailed = hasFailed,
            onRefresh = onRefresh,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (events.isEmpty() && !isLoading && !hasFailed) {
                item {
                    AppText(
                        text = stringResource(R.string.admin_event_status_empty),
                        style = AppTextStyle.B2,
                        color = GrayColor.C400,
                    )
                }
            }
            items(events, key = { it.id }) { event ->
                AdminEventCard(
                    event = event,
                    today = today,
                    onClick = { onEdit(event.id) },
                )
            }
            item(key = "correction") { correction() }
        }
    }
}
