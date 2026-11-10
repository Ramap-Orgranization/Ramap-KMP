package com.peto.ramap.domain.usecase

import com.peto.ramap.domain.model.auth.SignupTerms

fun interface SignupConsentRequester {
    suspend fun requestConsent(terms: SignupTerms): Boolean
}
