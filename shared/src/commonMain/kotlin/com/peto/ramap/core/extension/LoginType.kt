package com.peto.ramap.core.extension

import com.peto.ramap.domain.model.LoginType
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LoginColor
import com.peto.ramap.ui.login.model.LoginTypeUiModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_kakao
import ramap.shared.generated.resources.kakao_login_button_title

fun LoginType.toUiModel(): LoginTypeUiModel =
    when (this) {
        LoginType.KAKAO ->
            LoginTypeUiModel(
                type = this,
                logo = Res.drawable.ic_kakao,
                title = Res.string.kakao_login_button_title,
                background = LoginColor.Kakao,
                border = LoginColor.Kakao,
                textColor = GrayColor.C500,
            )
    }
