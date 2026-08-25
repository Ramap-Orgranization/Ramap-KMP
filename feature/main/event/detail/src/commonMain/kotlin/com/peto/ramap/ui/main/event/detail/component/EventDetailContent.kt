package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.event.detail.contract.EventDetailUiState
import com.peto.ramap.ui.main.event.detail.preview.EventDetailPreviewParameterProvider

@Composable
fun EventDetailContent(
    event: ShopEvent,
    hasCollaborators: Boolean,
    onVenueShopClick: (String) -> Unit,
    onCollaboratorShopClick: (String) -> Unit,
    onCollaboratorInstagramClick: (String) -> Unit,
    onWaitingLinkClick: (String) -> Unit,
    onSourceLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        EventDetailHeader(event = event)

        event.cancellationReason?.let { reason ->
            EventCancellationNotice(reason)
        }

        EventImages(event.displayImageUrls)

        EventDetailInfoSection(
            event = event,
            hasCollaborators = hasCollaborators,
            onVenueShopClick = onVenueShopClick,
            onCollaboratorShopClick = onCollaboratorShopClick,
            onCollaboratorInstagramClick = onCollaboratorInstagramClick,
        )

        if (event.description.isNotBlank()) {
            EventDetailDescriptionSection(description = event.description)
        }

        event.waitingMethod?.let { waiting ->
            EventDetailWaitingSection(
                waitingMethod = waiting,
                waitingUrl = event.waitingUrl,
                onWaitingLinkClick = onWaitingLinkClick,
            )
        }

        if (ExternalUriOpener.isSupportedWebUri(event.sourceUrl)) {
            EventDetailSourceButton(
                sourceUrl = event.sourceUrl,
                onSourceLinkClick = onSourceLinkClick,
            )
        }
    }
}

@Preview(name = "이벤트 상세 내용", showBackground = true)
@Composable
private fun EventDetailContentPreview(
    @PreviewParameter(EventDetailPreviewParameterProvider::class)
    uiState: EventDetailUiState,
) {
    RamapTheme {
        uiState.event?.let { event ->
            EventDetailContent(
                event = event,
                hasCollaborators = uiState.hasCollaborators,
                onVenueShopClick = {},
                onCollaboratorShopClick = {},
                onCollaboratorInstagramClick = {},
                onWaitingLinkClick = {},
                onSourceLinkClick = {},
                modifier = Modifier.background(CommonColor.White),
            )
        }
    }
}
