package com.peto.ramap.data.model

import com.peto.ramap.domain.model.report.NewsReport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NewsReportRequest(
    @SerialName("p_source_url")
    val sourceUrl: String?,
    @SerialName("p_attachment_path")
    val attachmentPath: String?,
) {
    companion object {
        fun from(
            report: NewsReport,
            attachmentPath: String?,
        ): NewsReportRequest =
            NewsReportRequest(
                sourceUrl = report.sourceUrl,
                attachmentPath = attachmentPath,
            )
    }
}
