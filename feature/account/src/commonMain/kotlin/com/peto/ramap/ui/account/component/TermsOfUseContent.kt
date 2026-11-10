package com.peto.ramap.ui.account.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.signup_consent_next
import ramap.shared.generated.resources.signup_consent_return

@Composable
internal fun TermsOfUseContent(
    checked: Boolean,
    openedDocument: TermsOfUseDocument?,
    onCheck: (Boolean) -> Unit,
    onOpenDocument: (TermsOfUseDocument) -> Unit,
    onOpenFullDocument: (TermsOfUseDocument) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
    ) {
        val horizontalPadding = if (openedDocument == null) 24.dp else 20.dp
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        bottom = if (openedDocument == null) 20.dp else 8.dp,
                    ),
        ) {
            if (openedDocument == null) {
                TermsOfUseOverview(
                    checked = checked,
                    onCheck = onCheck,
                    onOpenDocument = onOpenDocument,
                )
                Spacer(modifier = Modifier.height(24.dp))
                AppButton(
                    text = stringResource(Res.string.signup_consent_next),
                    onClick = onContinue,
                    enabled = checked,
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TermsOfUseDocumentOverview(
                    document = openedDocument,
                    onOpen = { onOpenFullDocument(openedDocument) },
                )
                TermsOfUseTextAction(
                    text = stringResource(Res.string.signup_consent_return),
                    onClick = onBack,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsOfUseContentPreview() {
    RamapTheme {
        TermsOfUseContent(
            checked = false,
            openedDocument = null,
            onCheck = {},
            onOpenDocument = {},
            onOpenFullDocument = {},
            onBack = {},
            onContinue = {},
        )
    }
}
