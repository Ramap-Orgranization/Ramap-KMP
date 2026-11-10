package com.peto.ramap.data.auth

import com.peto.ramap.domain.model.auth.SignupConsentDismissedException
import com.peto.ramap.domain.usecase.SignupConsentRequester
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class SignupConsentSignIn(
    private val supabaseClient: SupabaseClient,
    private val consentRequester: SignupConsentRequester,
) {
    suspend fun signIn(credentials: IDToken.Config.() -> Unit) {
        val challenge = exchangeCredentials(credentials) ?: return
        if (!consentRequester.requestConsent(challenge.terms)) throw SignupConsentDismissedException()
        val accepted =
            supabaseClient.postgrest
                .rpc(
                    function = ACCEPT_SIGNUP_TERMS_RPC,
                    parameters =
                        buildJsonObject {
                            put("p_challenge_id", challenge.id)
                            put("p_version", challenge.terms.version)
                        },
                ).decodeAs<Boolean>()
        check(accepted) { SIGNUP_CONSENT_EXPIRED }
        check(exchangeCredentials(credentials) == null) { SIGNUP_CONSENT_EXPIRED }
    }

    private suspend fun exchangeCredentials(credentials: IDToken.Config.() -> Unit): SignupTermsChallenge? {
        try {
            supabaseClient.auth.signInWith(IDToken, config = credentials)
            return null
        } catch (exception: RestException) {
            val message = exception.description
            if (message == null || !SignupTermsChallenge.isChallengeMessage(message)) throw exception
            // The challenge authorizes a consent write and must not reach error logging.
            check(exception.statusCode == SIGNUP_CHALLENGE_STATUS) { SIGNUP_CONSENT_INVALID }
            return checkNotNull(SignupTermsChallenge.decode(message)) { SIGNUP_CONSENT_INVALID }
        }
    }

    private companion object {
        const val ACCEPT_SIGNUP_TERMS_RPC = "accept_signup_terms"
        const val SIGNUP_CHALLENGE_STATUS = 422
        const val SIGNUP_CONSENT_INVALID = "Invalid signup consent request"
        const val SIGNUP_CONSENT_EXPIRED = "Signup consent request expired"
    }
}
