package com.peto.ramap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.designsystem.toast.ToastHost
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.navigation.AppRoute
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.component.LaduckLoadingContent
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject

@Composable
@Preview
fun App(
    loginRepository: LoginRepository = koinInject(),
    toastManager: ToastManager = koinInject(),
    onExitRequested: (() -> Unit)? = null,
) {
    var isInitialized by remember(loginRepository) { mutableStateOf(false) }
    LaunchedEffect(loginRepository) {
        try {
            loginRepository.awaitInitialization()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            // 초기화 실패 시 게스트 상태로 앱을 연다.
        }
        isInitialized = true
    }

    RamapTheme {
        if (!isInitialized) {
            Box(
                modifier = Modifier.fillMaxSize().background(CommonColor.White),
                contentAlignment = Alignment.Center,
            ) {
                LaduckLoadingContent()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                AppRoute(
                    toastManager = toastManager,
                    onExitRequested = onExitRequested,
                )

                ToastHost(toastManager = toastManager)
            }
        }
    }
}
