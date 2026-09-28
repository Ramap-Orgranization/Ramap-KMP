package com.peto.ramap.data.datasource.review

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReviewImageSignerTest {
    @Test
    fun preservesOrderSkipsMissingImagesAndBoundsParallelism() =
        runTest {
            var running = 0
            var peak = 0
            val signer =
                ReviewImageSigner { path ->
                    running++
                    peak = maxOf(peak, running)
                    delay(10)
                    running--
                    if (path == "missing") error("not found")
                    "signed:$path"
                }
            val paths = (1..12).map(Int::toString) + "missing"
            assertEquals(paths.dropLast(1).map { "signed:$it" }, signer.signImages(paths))
            assertEquals(4, peak)
            assertEquals(0, running)
        }

    @Test
    fun propagatesCancellationInsteadOfTreatingItAsMissingImage() =
        runTest {
            val signer = ReviewImageSigner { throw CancellationException("cancelled") }
            assertFailsWith<CancellationException> { signer.signImages(listOf("one")) }
        }
}
