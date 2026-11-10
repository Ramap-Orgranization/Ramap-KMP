package com.peto.ramap.ui.account.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun TermsOfUseOverview(
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
    onOpenFullDocument: (TermsOfUseDocument?) -> Unit,
) {
    TermsOfUseRequiredAgreement(
        checked = checked,
        onCheck = onCheck,
        onOpenTerms = { onOpenFullDocument(null) },
    )
    TermsOfUseDocumentRow(TermsOfUseDocument.Privacy, onOpen = { onOpenFullDocument(it) })
    TermsOfUseDocumentRow(TermsOfUseDocument.Community, onOpen = { onOpenFullDocument(it) })
}

@Preview(showBackground = true)
@Composable
private fun TermsOfUseOverviewPreview() {
    RamapTheme {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            TermsOfUseOverview(
                checked = false,
                onCheck = {},
                onOpenFullDocument = {},
            )
        }
    }
}
