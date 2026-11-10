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

@Composable
internal fun TermsOfUseContent(
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
    onOpenFullDocument: (TermsOfUseDocument?) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
        ) {
            TermsOfUseOverview(
                checked = checked,
                onCheck = onCheck,
                onOpenFullDocument = onOpenFullDocument,
            )
            Spacer(modifier = Modifier.height(24.dp))
            AppButton(
                text = stringResource(Res.string.signup_consent_next),
                onClick = onContinue,
                enabled = checked,
                height = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsOfUseContentPreview() {
    RamapTheme {
        TermsOfUseContent(
            checked = false,
            onCheck = {},
            onOpenFullDocument = {},
            onContinue = {},
        )
    }
}
