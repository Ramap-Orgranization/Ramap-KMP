package com.peto.ramap.ui.account.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right

@Composable
internal fun TermsOfUseDocumentRow(
    document: TermsOfUseDocument,
    onOpen: (TermsOfUseDocument) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .noRippleClickable(role = Role.Button) { onOpen(document) }
                .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = stringResource(document.title),
            style = AppTextStyle.B2,
            color = GrayColor.C400,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            tint = GrayColor.C300,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsOfUseDocumentRowPreview() {
    RamapTheme {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            TermsOfUseDocumentRow(
                document = TermsOfUseDocument.Terms,
            ) {}
        }
    }
}
