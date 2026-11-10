package com.peto.ramap.ui.account.component

import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.privacy_policy_menu
import ramap.shared.generated.resources.signup_consent_community
import ramap.shared.generated.resources.signup_consent_community_summary
import ramap.shared.generated.resources.signup_consent_privacy_summary
import ramap.shared.generated.resources.signup_consent_terms
import ramap.shared.generated.resources.signup_consent_terms_summary

internal enum class TermsOfUseDocument(
    val title: StringResource,
    val summary: StringResource,
    val publicUrl: String? = null,
) {
    Terms(
        Res.string.signup_consent_terms,
        Res.string.signup_consent_terms_summary,
    ),
    Privacy(
        Res.string.privacy_policy_menu,
        Res.string.signup_consent_privacy_summary,
        "https://ramap-orgranization.github.io/Ramap-KMP/",
    ),
    Community(
        Res.string.signup_consent_community,
        Res.string.signup_consent_community_summary,
        "https://ramap-orgranization.github.io/Ramap-KMP/community-guidelines.html",
    ),
}
