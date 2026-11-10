package com.peto.ramap.ui.account.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppIconButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleToggleable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.ic_signup_check
import ramap.shared.generated.resources.signup_consent_agree
import ramap.shared.generated.resources.signup_consent_required
import ramap.shared.generated.resources.signup_consent_terms_open

@Composable
internal fun TermsOfUseRequiredAgreement(
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .noRippleToggleable(value = checked, role = Role.Checkbox, onValueChange = onCheck)
                    .padding(start = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TermsOfUseAgreementMark(checked)
            AppText(
                text = stringResource(Res.string.signup_consent_required),
                style = AppTextStyle.B4,
                color = if (checked) GrayColor.C500 else GrayColor.C200,
            )
            AppText(
                text = stringResource(Res.string.signup_consent_agree),
                style = AppTextStyle.T2,
                color = GrayColor.C500,
                modifier = Modifier.weight(1f),
            )
        }
        AppIconButton(
            icon = Res.drawable.ic_chevron_right,
            contentDescription = stringResource(Res.string.signup_consent_terms_open),
            onClick = onOpenTerms,
            tint = GrayColor.C300,
        )
    }
}

@Composable
internal fun TermsOfUseAgreementMark(checked: Boolean) {
    val tint = if (checked) GrayColor.C500 else GrayColor.C200
    Icon(
        painter = painterResource(Res.drawable.ic_signup_check),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(20.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun TermsOfUseRequiredAgreementPreview() {
    RamapTheme {
        Column {
            TermsOfUseRequiredAgreement(
                checked = true,
                onCheck = {},
            ) {}
            TermsOfUseRequiredAgreement(
                checked = false,
                onCheck = {},
            ) {}
        }
    }
}
