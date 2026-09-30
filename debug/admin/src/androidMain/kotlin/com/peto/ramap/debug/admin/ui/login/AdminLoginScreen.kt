package com.peto.ramap.debug.admin.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginError
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginUiState
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.navigation_back
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
internal fun AdminLoginScreen(
    uiState: AdminLoginUiState,
    onBack: () -> Unit,
    onAdminLogin: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(CommonColor.White).statusBarsPadding(),
    ) {
        CommonTopBar(
            title = androidStringResource(R.string.admin_login_title),
            left = {
                Image(
                    painter = painterResource(Res.drawable.ic_arrow3_left),
                    contentDescription = stringResource(Res.string.navigation_back),
                    modifier = Modifier.padding(18.dp).size(24.dp).noRippleClickable(onClick = onBack),
                )
            },
        )
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                uiState.error?.let { error ->
                    AppText(
                        text = adminLoginErrorText(error),
                        style = AppTextStyle.B2,
                        color = SystemColor.Warning,
                    )
                }
                AppButton(
                    text = androidStringResource(R.string.admin_login_action),
                    onClick = onAdminLogin,
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState.loadState.isAnyLoading,
                )
            }
        }
    }
}

@Composable
private fun adminLoginErrorText(error: AdminLoginError): String =
    when (error) {
        AdminLoginError.AccessDenied -> androidStringResource(R.string.admin_login_access_denied)
        AdminLoginError.AccessUnavailable -> androidStringResource(R.string.admin_login_access_unavailable)
        AdminLoginError.LoginFailed -> androidStringResource(R.string.admin_login_failed)
    }
