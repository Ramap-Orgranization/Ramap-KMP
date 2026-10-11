package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.admin_registration_managed_events_load_failure
import ramap.shared.generated.resources.ic_refresh
import org.jetbrains.compose.resources.stringResource as composeStringResource

@Composable
internal fun AdminEventListHeader(
    title: String,
    isLoading: Boolean,
    hasFailed: Boolean,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppText(
                text = title,
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.admin_event_status_refresh),
                    tint = GrayColor.C500,
                )
            }
        }
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = GrayColor.C500,
            )
        }
        if (hasFailed) {
            AppText(
                text = composeStringResource(Res.string.admin_registration_managed_events_load_failure),
                style = AppTextStyle.B2,
                color = SystemColor.Warning,
            )
        }
    }
}
