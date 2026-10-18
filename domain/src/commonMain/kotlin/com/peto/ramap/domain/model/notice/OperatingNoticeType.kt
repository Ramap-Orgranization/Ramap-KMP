package com.peto.ramap.domain.model.notice

enum class OperatingNoticeType {
    TEMPORARY_CLOSURE,
    EARLY_CLOSING,
    PLANNED_CLOSING,
    ;

    val hasClosingTime: Boolean
        get() = this == EARLY_CLOSING || this == PLANNED_CLOSING

    companion object {
        fun from(id: String): OperatingNoticeType = fromOrNull(id) ?: error("Invalid notice type: $id")

        fun fromOrNull(id: String): OperatingNoticeType? =
            when (id.lowercase()) {
                "full_close", "temporary_closure" -> TEMPORARY_CLOSURE
                "early_close", "early_closing" -> EARLY_CLOSING
                "planned_close", "planned_closing" -> PLANNED_CLOSING
                else -> null
            }
    }
}
