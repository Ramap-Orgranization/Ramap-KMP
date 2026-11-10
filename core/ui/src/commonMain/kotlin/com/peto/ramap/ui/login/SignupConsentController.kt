package com.peto.ramap.ui.login

import com.peto.ramap.domain.model.auth.SignupTerms
import com.peto.ramap.domain.usecase.SignupConsentRequester
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SignupConsentController : SignupConsentRequester {
    private val pending = MutableStateFlow<Request?>(null)
    val request = pending.asStateFlow()

    override suspend fun requestConsent(terms: SignupTerms): Boolean {
        val request = Request(terms)
        if (!pending.compareAndSet(null, request)) return false

        return try {
            request.answer.await()
        } finally {
            pending.compareAndSet(request, null)
        }
    }

    fun accept(request: Request) = complete(request, accepted = true)

    fun dismiss(request: Request) = complete(request, accepted = false)

    private fun complete(
        request: Request,
        accepted: Boolean,
    ) {
        if (pending.value === request) request.answer.complete(accepted)
    }

    class Request internal constructor(
        val terms: SignupTerms,
    ) {
        internal val answer = CompletableDeferred<Boolean>()
    }
}
