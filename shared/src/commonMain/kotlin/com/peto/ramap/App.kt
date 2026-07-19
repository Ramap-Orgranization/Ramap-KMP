package com.peto.ramap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.designsystem.toast.ToastHost
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.theme.RamapTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.koinInject

@Composable
@Preview
fun App(
    loginRepository: LoginRepository = koinInject(),
    personalizationStore: ShopPersonalizationStore = koinInject(),
    toastManager: ToastManager = koinInject(),
    onExitRequested: (() -> Unit)? = null,
) {
    LaunchedEffect(loginRepository, personalizationStore) {
        observeSessionPersonalization(
            loginRepository = loginRepository,
            personalizationStore = personalizationStore,
        )
    }

    RamapTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AppRoute(
                toastManager = toastManager,
                onExitRequested = onExitRequested,
            )

            ToastHost(toastManager = toastManager)
        }
    }
}

internal suspend fun observeSessionPersonalization(
    loginRepository: LoginRepository,
    personalizationStore: ShopPersonalizationStore,
) {
    awaitSessionInitialization(loginRepository)
    loginRepository.sessionState
        .distinctUntilChanged()
        .collectLatest { sessionState ->
            synchronizePersonalization(
                isAuthenticated = sessionState == LoginSessionState.AUTHENTICATED,
                personalizationStore = personalizationStore,
            )
        }
}

private suspend fun awaitSessionInitialization(loginRepository: LoginRepository) {
    try {
        loginRepository.awaitInitialization()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Throwable) {
        // 초기화 실패 시 게스트 상태로 앱을 연다.
    }
}

private suspend fun synchronizePersonalization(
    isAuthenticated: Boolean,
    personalizationStore: ShopPersonalizationStore,
) {
    if (!isAuthenticated) {
        personalizationStore.clear()
        return
    }
    personalizationStore.refresh()
}
