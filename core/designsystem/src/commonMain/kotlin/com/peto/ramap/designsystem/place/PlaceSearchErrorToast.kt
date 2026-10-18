package com.peto.ramap.designsystem.place

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.place.PlaceSearchErrorCode
import com.peto.ramap.domain.model.place.PlaceSearchException
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.place_search_daily_limited
import ramap.shared.generated.resources.place_search_daily_limited_hours
import ramap.shared.generated.resources.place_search_daily_limited_minutes
import ramap.shared.generated.resources.place_search_daily_limited_seconds
import ramap.shared.generated.resources.place_search_rate_limited
import ramap.shared.generated.resources.place_search_rate_limited_hours
import ramap.shared.generated.resources.place_search_rate_limited_minutes
import ramap.shared.generated.resources.place_search_rate_limited_seconds
import ramap.shared.generated.resources.place_search_unavailable
import ramap.shared.generated.resources.place_search_unavailable_hours
import ramap.shared.generated.resources.place_search_unavailable_minutes
import ramap.shared.generated.resources.place_search_unavailable_seconds

object PlaceSearchErrorToast {
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
    private const val DURATION_MILLIS = 6_000L
    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val HTTP_UNAVAILABLE = 503

    fun resolve(error: RamapError): ToastData? {
        val failure = error.cause as? PlaceSearchException ?: httpFailure(error) ?: return null
        return when (failure.code) {
            PlaceSearchErrorCode.REQUEST_LIMIT ->
                toast(
                    failure.retryAfterSeconds,
                    Res.string.place_search_rate_limited,
                    Res.string.place_search_rate_limited_seconds,
                    Res.string.place_search_rate_limited_minutes,
                    Res.string.place_search_rate_limited_hours,
                )
            PlaceSearchErrorCode.DAILY_LIMIT ->
                toast(
                    failure.retryAfterSeconds,
                    Res.string.place_search_daily_limited,
                    Res.string.place_search_daily_limited_seconds,
                    Res.string.place_search_daily_limited_minutes,
                    Res.string.place_search_daily_limited_hours,
                )
            PlaceSearchErrorCode.UNAVAILABLE ->
                toast(
                    failure.retryAfterSeconds,
                    Res.string.place_search_unavailable,
                    Res.string.place_search_unavailable_seconds,
                    Res.string.place_search_unavailable_minutes,
                    Res.string.place_search_unavailable_hours,
                )
        }
    }

    private fun httpFailure(error: RamapError): PlaceSearchException? {
        if (error !is RamapError.Http) return null
        return when (error.status) {
            HTTP_TOO_MANY_REQUESTS -> PlaceSearchException(PlaceSearchErrorCode.REQUEST_LIMIT)
            HTTP_UNAVAILABLE -> PlaceSearchException(PlaceSearchErrorCode.UNAVAILABLE)
            else -> null
        }
    }

    private fun toast(
        retryAfterSeconds: Long?,
        fallbackResource: StringResource,
        secondsResource: StringResource,
        minutesResource: StringResource,
        hoursResource: StringResource,
    ): ToastData {
        val resource =
            when {
                retryAfterSeconds == null -> fallbackResource
                retryAfterSeconds < SECONDS_PER_MINUTE -> secondsResource
                retryAfterSeconds < SECONDS_PER_HOUR -> minutesResource
                else -> hoursResource
            }
        val amount =
            when {
                retryAfterSeconds == null -> null
                retryAfterSeconds < SECONDS_PER_MINUTE -> retryAfterSeconds
                retryAfterSeconds < SECONDS_PER_HOUR -> (retryAfterSeconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
                else -> (retryAfterSeconds + SECONDS_PER_HOUR - 1) / SECONDS_PER_HOUR
            }
        return ToastData(
            message = resource,
            type = ToastType.ERROR,
            durationMills = DURATION_MILLIS,
            formatArguments = amount?.let { listOf(it.toString()) }.orEmpty(),
        )
    }
}
