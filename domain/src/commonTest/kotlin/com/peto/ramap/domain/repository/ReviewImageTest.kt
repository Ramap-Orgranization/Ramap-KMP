package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.review.ReviewImage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewImageTest {
    @Test
    fun isValid_acceptsSupportedSignaturesWithinFiveMiB() {
        assertTrue(ReviewImage(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), ReviewImage.JPEG_MIME_TYPE).isValid())
        assertTrue(ReviewImage(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), ReviewImage.PNG_MIME_TYPE).isValid())
        assertFalse(ReviewImage(byteArrayOf(1, 2, 3), ReviewImage.JPEG_MIME_TYPE).isValid())
        assertFalse(ReviewImage(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), "image/gif").isValid())
        val oversizedJpeg = ByteArray(ReviewImage.MAX_SIZE_BYTES + 1)
        oversizedJpeg[0] = 0xFF.toByte()
        oversizedJpeg[1] = 0xD8.toByte()
        oversizedJpeg[2] = 0xFF.toByte()
        assertFalse(ReviewImage(oversizedJpeg, ReviewImage.JPEG_MIME_TYPE).isValid())
    }
}
