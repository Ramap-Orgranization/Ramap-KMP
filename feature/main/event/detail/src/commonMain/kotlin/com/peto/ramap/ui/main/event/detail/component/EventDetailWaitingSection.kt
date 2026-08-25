package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.event.detail.contract.EventDetailUiState
import com.peto.ramap.ui.main.event.detail.preview.EventDetailPreviewParameterProvider
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.catchtable
import ramap.shared.generated.resources.event_waiting
import ramap.shared.generated.resources.event_waiting_action

@Composable
fun EventDetailWaitingSection(
    waitingMethod: String,
    waitingUrl: String?,
    onWaitingLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(Res.string.event_waiting),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(vertical = 10.dp)) {
            EventDetailValue(waitingMethod)
            waitingUrl?.takeIf(ExternalUriOpener::isSupportedWebUri)?.let { url ->
                AppButton(
                    text = stringResource(Res.string.event_waiting_action),
                    icon = Res.drawable.catchtable,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    textColor = CommonColor.Black,
                    backgroundColor = GrayColor.C100,
                    onClick = { onWaitingLinkClick(url) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventDetailWaitingSectionPreview(
    @PreviewParameter(EventDetailPreviewParameterProvider::class)
    uiState: EventDetailUiState,
) {
    RamapTheme {
        uiState.event?.let { event ->
            event.waitingMethod?.let { waitingMethod ->
                EventDetailWaitingSection(
                    waitingMethod = waitingMethod,
                    waitingUrl = event.waitingUrl,
                    onWaitingLinkClick = {},
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
