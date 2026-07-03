package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.map.MapRoute
import org.koin.compose.koinInject

@Composable
@Preview
fun App(loginRepository: LoginRepository = koinInject()) {
    LaunchedEffect(loginRepository) {
        loginRepository.awaitInitialization()
    }

    RamapTheme {
        MapRoute()
    }
}
