package com.peto.ramap.ui.main.newshops

import kotlin.time.Clock
import kotlin.time.Instant

internal class NewShopsTestClock(
    var instant: Instant,
) : Clock {
    override fun now(): Instant = instant
}
