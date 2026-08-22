package com.peto.ramap.domain.model.report

data class NewsReportEvidence(
    val bytes: ByteArray,
    val mimeType: String,
) {
    val fileExtension: String
        get() = if (mimeType == PNG_MIME_TYPE) "png" else "jpg"

    fun isValid(): Boolean =
        bytes.isNotEmpty() &&
            bytes.size <= MAX_SIZE_BYTES &&
            mimeType in SUPPORTED_MIME_TYPES

    companion object {
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val PNG_MIME_TYPE = "image/png"
        const val MAX_SIZE_BYTES = 5 * 1024 * 1024

        private val SUPPORTED_MIME_TYPES = setOf(JPEG_MIME_TYPE, PNG_MIME_TYPE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NewsReportEvidence

        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false
        if (fileExtension != other.fileExtension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileExtension.hashCode()
        return result
    }
}
