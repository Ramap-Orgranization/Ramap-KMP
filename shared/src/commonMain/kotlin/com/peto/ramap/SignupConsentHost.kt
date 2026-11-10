package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.ui.account.component.TermsOfUseBottomSheet
import com.peto.ramap.ui.login.SignupConsentController
import org.koin.compose.koinInject

@Composable
internal fun SignupConsentHost(controller: SignupConsentController = koinInject()) {
    val pending by controller.request.collectAsStateWithLifecycle()
    val request = pending ?: return

    key(request) {
        TermsOfUseBottomSheet(
            terms = request.terms,
            onAccept = { controller.accept(request) },
            onDismiss = { controller.dismiss(request) },
        )
    }
}
