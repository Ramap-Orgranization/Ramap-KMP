package com.peto.ramap.designsystem.button

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.peto.ramap.core.extension.noRippleClickable
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.LoginType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LoginColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_kakao
import ramap.shared.generated.resources.kakao_login_button_title

@Composable
internal fun LoginButton(
    type: LoginType,
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
                .noRippleClickable { onClickLogin(type) },
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

private fun LoginType.loginButtonTitle(): StringResource =
    when (this) {
        LoginType.KAKAO -> Res.string.kakao_login_button_title
    }

private fun LoginType.loginButtonLogo(): DrawableResource =
    when (this) {
        LoginType.KAKAO -> Res.drawable.ic_kakao
    }

private fun LoginType.loginButtonBackground(): Color =
    when (this) {
        LoginType.KAKAO -> LoginColor.Kakao
    }

private fun LoginType.loginButtonBorder(): Color =
    when (this) {
        LoginType.KAKAO -> LoginColor.Kakao
    }

private fun LoginType.loginButtonTextColor(): Color =
    when (this) {
        LoginType.KAKAO -> GrayColor.C500
    }
