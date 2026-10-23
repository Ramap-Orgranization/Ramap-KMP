package com.peto.ramap.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.peto.ramap.domain.model.auth.LoginType
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject

@Composable
fun rememberLoginGuideRequest(
    onLoginTypeSelected: (LoginType) -> Unit,
    onDismiss: () -> Unit = {},
    ownerKey: Any? = Unit,
    controller: LoginGuideController = koinInject(),
): (StringResource) -> Unit {
    val currentRequest by rememberUpdatedState(
        key(controller, ownerKey) {
            val owner = remember { Any() }
            val currentOnLoginTypeSelected by rememberUpdatedState(onLoginTypeSelected)
            val currentOnDismiss by rememberUpdatedState(onDismiss)

            DisposableEffect(controller, owner) {
                onDispose { controller.cancel(owner) }
            }

            remember(controller, owner) {
                { description: StringResource ->
                    controller.show(
                        owner = owner,
                        description = description,
                        onLoginTypeSelected = { currentOnLoginTypeSelected(it) },
                        onDismiss = { currentOnDismiss() },
                    )
                }
            }
        },
    )

    return remember { { description -> currentRequest(description) } }
}
