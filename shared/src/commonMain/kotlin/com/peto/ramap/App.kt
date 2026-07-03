package com.peto.ramap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.login.LoginRoute
import com.peto.ramap.ui.map.MapRoute
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

@Composable
@Preview
fun App(loginRepository: LoginRepository = koinInject()) {
    var sessionStatus: SessionStatus by remember {
        mutableStateOf(SessionStatus.Initializing)
    }

    LaunchedEffect(loginRepository) {
        loginRepository.awaitInitialization()
        loginRepository.sessionStatus.collectLatest { status ->
            sessionStatus = status
        }
    }

    RamapTheme {
        when (sessionStatus) {
            is SessionStatus.Authenticated -> MapRoute()
            SessionStatus.Initializing ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            is SessionStatus.NotAuthenticated,
            is SessionStatus.RefreshFailure,
            ->
                LoginRoute(
                    onLoginSuccess = {
                        sessionStatus = loginRepository.sessionStatus.value
                    },
                )
        }
    }
}
