package com.peto.ramap.ui.account.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_link
import ramap.shared.generated.resources.signup_consent_open_document

@Composable
internal fun TermsOfUseDocumentOverview(
    document: TermsOfUseDocument,
    onOpen: () -> Unit,
) {
    AppText(
        text = stringResource(document.title),
        style = AppTextStyle.H4,
        color = GrayColor.C500,
    )
    Spacer(modifier = Modifier.height(8.dp))
    AppText(
        text = stringResource(document.summary),
        style = AppTextStyle.B2,
        color = GrayColor.C400,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GrayColor.C050, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
    )
    AppButton(
        text = stringResource(Res.string.signup_consent_open_document),
        onClick = onOpen,
        icon = Res.drawable.ic_link,
        iconSize = 16.dp,
        iconTint = CommonColor.White,
        modifier =
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
    )
}
