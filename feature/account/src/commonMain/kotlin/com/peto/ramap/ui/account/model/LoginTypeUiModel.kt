package com.peto.ramap.ui.account.model

import androidx.compose.ui.graphics.Color
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LoginColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_kakao
import ramap.shared.generated.resources.kakao_login_button_title

data class LoginTypeUiModel(
    val type: LoginType,
) {
    fun loginButtonTitle(): StringResource =
        when (type) {
            LoginType.KAKAO -> Res.string.kakao_login_button_title
        }

    fun loginButtonLogo(): DrawableResource =
        when (type) {
            LoginType.KAKAO -> Res.drawable.ic_kakao
        }

    fun loginButtonBackground(): Color =
        when (type) {
            LoginType.KAKAO -> LoginColor.Kakao
        }

    fun loginButtonBorder(): Color =
        when (type) {
            LoginType.KAKAO -> LoginColor.Kakao
        }

    fun loginButtonTextColor(): Color =
        when (type) {
            LoginType.KAKAO -> GrayColor.C500
        }
}
