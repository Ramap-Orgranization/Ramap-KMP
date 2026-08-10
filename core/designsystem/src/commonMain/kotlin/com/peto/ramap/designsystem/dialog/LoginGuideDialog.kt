package com.peto.ramap.designsystem.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_description
import ramap.shared.generated.resources.login_required_message

@Composable
fun LoginGuideDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onLoginTypeSelected: (LoginType) -> Unit,
    loginButton: @Composable (LoginType, () -> Unit) -> Unit,
) {
    CommonBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
    ) { _ ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppText(
                text = stringResource(Res.string.login_required_message),
                modifier = Modifier.fillMaxWidth(),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = stringResource(Res.string.login_required_description),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                textAlign = TextAlign.Center,
            )

            supportedLoginTypes().forEach { loginType ->
                loginButton(loginType) { onLoginTypeSelected(loginType) }
            }
        }
    }
}
