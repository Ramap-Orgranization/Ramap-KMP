package com.peto.ramap.designsystem.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.resource.review.reviewCreationTime
import com.peto.ramap.designsystem.resource.review.reviewDateTimeResource
import com.peto.ramap.designsystem.resource.review.reviewTimeResource
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_time_date
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Composable
fun reviewWrittenTimeLabel(createdAt: String): String? {
    val timeZone = TimeZone.currentSystemDefault()
    val writtenAt = remember(createdAt, timeZone) { reviewCreationTime(createdAt, timeZone) } ?: return null
    var label by remember(writtenAt, timeZone) {
        mutableStateOf(reviewTimeResource(writtenAt = writtenAt, now = Clock.System.now(), timeZone = timeZone))
    }

    LaunchedEffect(writtenAt, timeZone) {
        while (label.resource != Res.string.review_time_date) {
            delay(1.minutes)
            label = reviewTimeResource(writtenAt = writtenAt, now = Clock.System.now(), timeZone = timeZone)
        }
    }

    return label.format()
}

@Composable
fun reviewWrittenDateTimeLabel(createdAt: String): String? {
    val timeZone = TimeZone.currentSystemDefault()
    val label = remember(createdAt, timeZone) { reviewDateTimeResource(createdAt, timeZone) }
    return label?.format()
}
