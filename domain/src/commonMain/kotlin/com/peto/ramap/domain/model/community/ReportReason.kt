package com.peto.ramap.domain.model.community

enum class ReportReason {
    SPAM,
    HARASSMENT,
    HATE,
    SEXUAL_CONTENT,
    VIOLENCE,
    PRIVACY,
    OTHER,
    ;

    val maxDetailsLength: Int
        get() = if (this == OTHER) OTHER_MAX_DETAILS_LENGTH else MAX_DETAILS_LENGTH

    val requiresDetails: Boolean
        get() = this == OTHER

    fun isValidDetails(details: String): Boolean = details.length <= maxDetailsLength && (!requiresDetails || details.isNotBlank())

    companion object {
        const val MAX_DETAILS_LENGTH = 1000
        const val OTHER_MAX_DETAILS_LENGTH = 100
    }
}
