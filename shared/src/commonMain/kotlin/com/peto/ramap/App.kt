package com.peto.ramap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.designsystem.toast.ToastHost
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.navigation.AppRoute
import com.peto.ramap.theme.RamapTheme
import org.koin.compose.koinInject

@Composable
@Preview
fun App(
    loginRepository: LoginRepository = koinInject(),
    toastManager: ToastManager = koinInject(),
) {
    LaunchedEffect(loginRepository) {
        loginRepository.awaitInitialization()
    }

    RamapTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AppRoute()

            ToastHost(toastManager = toastManager)
        }
    }
}
