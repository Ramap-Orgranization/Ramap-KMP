package com.peto.ramap.ui.account.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.resource.login.toUiModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginButton(
    type: LoginType,
    onClickLogin: (LoginType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiModel = type.toUiModel()

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(uiModel.buttonBackground)
                .border(1.dp, uiModel.buttonBorder, RoundedCornerShape(12.dp))
                .noRippleClickable { onClickLogin(uiModel.type) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(uiModel.buttonLogo),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.width(12.dp))

        AppText(
            text = stringResource(uiModel.buttonTitle),
            style = AppTextStyle.T3,
            color = uiModel.buttonTextColor,
        )
    }
}

@Preview
@Composable
private fun LoginButtonPreview() {
    RamapTheme {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LoginType.entries.forEach {
                LoginButton(it, {})
            }
        }
    }
}
