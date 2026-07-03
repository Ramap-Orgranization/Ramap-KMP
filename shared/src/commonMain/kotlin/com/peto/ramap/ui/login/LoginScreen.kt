package com.peto.ramap.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.LoginType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.login.component.LoginButton
import com.peto.ramap.ui.login.contract.LoginIntent
import com.peto.ramap.ui.login.contract.LoginSideEffect
import com.peto.ramap.ui.login.contract.LoginUiState
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collectLatest { sideEffect ->
            when (sideEffect) {
                LoginSideEffect.LoginSuccess -> onLoginSuccess()
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onClickLogin = { type ->
            viewModel.dispatch(LoginIntent.ClickLogin(type))
        },
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onClickLogin: (LoginType) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppText(
            text = "Ramap",
            style = AppTextStyle.H1,
            color = GrayColor.C500,
        )

        Spacer(Modifier.height(12.dp))

        AppText(
            text = "라멘 지도를 더 편하게 시작해보세요.",
            style = AppTextStyle.T3,
            color = GrayColor.C300,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        uiState.loginTypes.forEach { type ->
            LoginButton(
                type = type,
                onClickLogin = onClickLogin,
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}
