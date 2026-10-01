package com.peto.ramap.domain.model.profile

data class ProfileImage(
    val bytes: ByteArray,
    val mimeType: String,
) {
    val fileExtension: String
        get() = if (mimeType == PNG_MIME_TYPE) "png" else "jpg"

    fun isValid(): Boolean = bytes.isNotEmpty() && bytes.size <= MAX_BYTES && mimeType in SUPPORTED_MIME_TYPES && hasExpectedSignature()

    private fun hasExpectedSignature(): Boolean =
        when (mimeType) {
            JPEG_MIME_TYPE -> bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            PNG_MIME_TYPE -> bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE)
            else -> false
        }

    companion object {
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val PNG_MIME_TYPE = "image/png"
        const val MAX_BYTES = 5 * 1024 * 1024

        private val SUPPORTED_MIME_TYPES = setOf(JPEG_MIME_TYPE, PNG_MIME_TYPE)
        private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
