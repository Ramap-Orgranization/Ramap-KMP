package com.peto.ramap.ui.refresh

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.peto.ramap.domain.model.notice.OperatingNotice
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock

@Composable
fun BusinessHoursStatusRefreshEffect(
    currentDateTime: LocalDateTime,
    notices: List<OperatingNotice>,
    onRefresh: () -> Unit,
) {
    val nextClosing = notices.mapNotNull { it.nextClosingAt(currentDateTime) }.minOrNull()
    val intervalMillis =
        nextClosing?.let {
            (it.toInstant(TimeZone.of(SEOUL_TIME_ZONE)) - Clock.System.now()).inWholeMilliseconds.coerceIn(1L, STATUS_REFRESH_MILLIS)
        } ?: STATUS_REFRESH_MILLIS

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME, onEvent = onRefresh)
    PeriodicRefreshEffect(intervalMillis = intervalMillis, onRefresh = onRefresh)
}

private const val STATUS_REFRESH_MILLIS = 60_000L
private const val SEOUL_TIME_ZONE = "Asia/Seoul"
