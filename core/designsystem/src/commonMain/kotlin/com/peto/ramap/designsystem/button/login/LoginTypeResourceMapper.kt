package com.peto.ramap.designsystem.button.login

import com.peto.ramap.designsystem.component.LoginTypeUiModel
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LoginColor
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_login_button_title
import ramap.shared.generated.resources.apple_login_failure_message
import ramap.shared.generated.resources.ic_apple
import ramap.shared.generated.resources.ic_kakao
import ramap.shared.generated.resources.kakao_login_button_title
import ramap.shared.generated.resources.kakao_login_failure_message

object LoginTypeResourceMapper {
    fun button(type: LoginType): LoginTypeUiModel =
        when (type) {
            LoginType.KAKAO ->
                LoginTypeUiModel(
                    buttonTitle = Res.string.kakao_login_button_title,
                    buttonLogo = Res.drawable.ic_kakao,
                    buttonBackground = LoginColor.Kakao,
                    buttonBorder = LoginColor.Kakao,
                    buttonTextColor = GrayColor.C500,
                )

            LoginType.APPLE ->
                LoginTypeUiModel(
                    buttonTitle = Res.string.apple_login_button_title,
                    buttonLogo = Res.drawable.ic_apple,
                    buttonBackground = CommonColor.Black,
                    buttonBorder = CommonColor.Black,
                    buttonTextColor = CommonColor.White,
                )
        }

    fun failureMessage(type: LoginType): StringResource =
        when (type) {
            LoginType.KAKAO -> Res.string.kakao_login_failure_message
            LoginType.APPLE -> Res.string.apple_login_failure_message
        }
}
