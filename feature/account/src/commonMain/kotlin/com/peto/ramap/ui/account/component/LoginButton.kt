package com.peto.ramap.ui.account.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.ui.account.model.LoginTypeUiModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginButton(
    type: LoginTypeUiModel,
    onClickLogin: (LoginType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(type.loginButtonBackground())
                .border(1.dp, type.loginButtonBorder(), RoundedCornerShape(12.dp))
                .noRippleClickable { onClickLogin(type.type) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(type.loginButtonLogo()),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.width(12.dp))

        AppText(
            text = stringResource(type.loginButtonTitle()),
            style = AppTextStyle.T3,
            color = type.loginButtonTextColor(),
        )
    }
}
