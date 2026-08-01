package com.peto.ramap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.toast.ToastHost
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.theme.RamapTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onStart
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.personalization_load_failure_message
import ramap.shared.generated.resources.personalization_load_failure_title

@Composable
fun App(
    loginRepository: LoginRepository = koinInject(),
    personalizationStore: ShopPersonalizationStore = koinInject(),
    toastManager: ToastManager = koinInject(),
    onExitRequested: (() -> Unit)? = null,
) {
    val retryRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val personalizationState by personalizationStore.state.collectAsStateWithLifecycle()
    // 구성 변경이 새로고침의 Loading 상태에서 끝나도 네비게이션 저장소를 다시 연결한다.
    var hasShownAppRoute by rememberSaveable { mutableStateOf(false) }

    if (personalizationState is PersonalizationBootstrapState.Success) {
        hasShownAppRoute = true
    }

    LaunchedEffect(loginRepository, personalizationStore) {
        observeSessionPersonalization(
            loginRepository = loginRepository,
            personalizationStore = personalizationStore,
            retryRequests = retryRequests,
        )
    }

    RamapTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                shouldShowAppRoute(
                    personalizationState = personalizationState,
                    hasShownAppRoute = hasShownAppRoute,
                ) ->
                    AppRoute(
                        toastManager = toastManager,
                        onExitRequested = onExitRequested,
                    )

                personalizationState == PersonalizationBootstrapState.Loading ->
                    RamenLoadingIndicator(modifier = Modifier.fillMaxSize())

                personalizationState == PersonalizationBootstrapState.Error ->
                    LoadErrorContent(
                        image = Res.drawable.laduck_error_confused,
                        title = stringResource(Res.string.personalization_load_failure_title),
                        description = stringResource(Res.string.personalization_load_failure_message),
                        onRetry = { retryRequests.tryEmit(Unit) },
                        modifier = Modifier.fillMaxSize(),
                    )

                else -> Unit
            }

            ToastHost(toastManager = toastManager)
        }
    }
}

internal fun shouldShowAppRoute(
    personalizationState: PersonalizationBootstrapState,
    hasShownAppRoute: Boolean,
): Boolean = hasShownAppRoute || personalizationState is PersonalizationBootstrapState.Success

internal suspend fun observeSessionPersonalization(
    loginRepository: LoginRepository,
    personalizationStore: ShopPersonalizationStore,
    retryRequests: Flow<Unit> = emptyFlow(),
) {
    awaitSessionInitialization(loginRepository)
    loginRepository.sessionState
        .distinctUntilChanged()
        .collectLatest { sessionState ->
            if (sessionState != LoginSessionState.AUTHENTICATED) {
                personalizationStore.clear()
                return@collectLatest
            }

            retryRequests
                .onStart { emit(Unit) }
                .collectLatest { refreshPersonalization(personalizationStore) }
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

private suspend fun refreshPersonalization(personalizationStore: ShopPersonalizationStore) {
    try {
        personalizationStore.refresh()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        // Store 경계 밖 구현의 예외가 사용자 재시도 수집을 종료하지 않게 한다.
    }
}
