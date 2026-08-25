package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.event.detail.contract.EventDetailUiState
import com.peto.ramap.ui.main.event.detail.preview.EventDetailPreviewParameterProvider

@Composable
fun EventDetailDescriptionSection(
    description: String,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        EventDetailValue(
            value = description,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventDetailDescriptionSectionPreview(
    @PreviewParameter(EventDetailPreviewParameterProvider::class)
    uiState: EventDetailUiState,
) {
    RamapTheme {
        uiState.event?.let { event ->
            EventDetailDescriptionSection(
                description = event.description,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
