package com.peto.ramap.ui.resource.login

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LoginColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_kakao
import ramap.shared.generated.resources.kakao_login_button_title

@Immutable
data class LoginTypeUiModel(
    val type: LoginType,
    val buttonTitle: StringResource,
    val buttonLogo: DrawableResource,
    val buttonBackground: Color,
    val buttonBorder: Color,
    val buttonTextColor: Color,
)

fun LoginType.toUiModel(): LoginTypeUiModel =
    when (this) {
        LoginType.KAKAO ->
            LoginTypeUiModel(
                type = this,
                buttonTitle = Res.string.kakao_login_button_title,
                buttonLogo = Res.drawable.ic_kakao,
                buttonBackground = LoginColor.Kakao,
                buttonBorder = LoginColor.Kakao,
                buttonTextColor = GrayColor.C500,
            )
    }
