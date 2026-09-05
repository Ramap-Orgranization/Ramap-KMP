package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
internal fun AdminEventEditManager(
    events: List<AdminManagedEvent>,
    onEdit: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(
            text = stringResource(R.string.admin_event_edit_select),
            style = AppTextStyle.B1,
            color = GrayColor.C500,
        )
        events.forEach { event ->
            Surface(onClick = { onEdit(event.id) }, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { AppText(event.title, style = AppTextStyle.T2, color = GrayColor.C400) },
                    supportingContent = { AppText(event.shopName, style = AppTextStyle.B4, color = GrayColor.C300) },
                )
            }
        }
    }
}
