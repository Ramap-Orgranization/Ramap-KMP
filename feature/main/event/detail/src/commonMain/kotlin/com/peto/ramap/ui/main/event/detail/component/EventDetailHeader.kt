package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.main.event.detail.contract.EventDetailUiState
import com.peto.ramap.ui.main.event.detail.preview.EventDetailPreviewParameterProvider
import org.jetbrains.compose.resources.stringResource

@Composable
fun EventDetailHeader(
    event: ShopEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 5.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isStatus = ShopEventResourceMapper.statusLabel(event) != null
            ShopEventResourceMapper.dateLabel(event)?.let { dateLabel ->
                NewsBadge(
                    text = stringResource(dateLabel),
                    containerColor = if (isStatus) SystemColor.Warning else ChromaticColor.Yellow400,
                    contentColor = if (isStatus) CommonColor.White else GrayColor.C500,
                    textStyle = AppTextStyle.T3,
                )
            }
            NewsBadge(
                text = stringResource(ShopEventResourceMapper.typeLabel(event.type)),
                textStyle = AppTextStyle.T3,
            )
        }
        AppText("🍜 ${event.title}", style = AppTextStyle.H1, color = GrayColor.C500)
    }
}

@Preview(showBackground = true)
@Composable
private fun EventDetailHeaderPreview(
    @PreviewParameter(EventDetailPreviewParameterProvider::class)
    uiState: EventDetailUiState,
) {
    RamapTheme {
        uiState.event?.let { event ->
            EventDetailHeader(
                event = event,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
