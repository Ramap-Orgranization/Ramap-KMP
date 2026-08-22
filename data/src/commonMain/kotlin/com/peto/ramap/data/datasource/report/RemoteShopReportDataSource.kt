package com.peto.ramap.data.datasource.report

import com.peto.ramap.data.model.NewsReportRequest
import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest
import com.peto.ramap.domain.model.report.NewsReport
import com.peto.ramap.domain.model.report.NewsReportSubmission
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlin.uuid.Uuid

internal class RemoteShopReportDataSource(
    private val client: SupabaseClient,
) : ShopReportDataSource {
    override suspend fun submitShopInformationReport(report: ShopInformationReportRequest) {
        client
            .from(TABLE_NAME)
            .insert(report)
    }

    override suspend fun submitUnregisteredPlaceReport(report: UnregisteredPlaceReportRequest) {
        client
            .from(PLACE_REPORT_TABLE_NAME)
            .insert(report)
    }

    override suspend fun submitNewsReport(report: NewsReport): NewsReportSubmission {
        val evidence = report.evidence
        val attachmentPath =
            if (evidence == null) {
                null
            } else {
                uploadEvidence(evidence)
            }
        return try {
            val submission =
                client
                    .postgrest
                    .rpc(NEWS_REPORT_SUBMISSION_FUNCTION, NewsReportRequest.from(report, attachmentPath))
                    .decodeAs<String>()
            NewsReportSubmission.valueOf(submission.trim().removeSurrounding("\"").uppercase())
        } catch (exception: Throwable) {
            if (attachmentPath != null) deleteEvidence(attachmentPath)
            throw exception
        }.also { submission ->
            if (submission == NewsReportSubmission.DUPLICATE && attachmentPath != null) deleteEvidence(attachmentPath)
        }
    }

    private suspend fun uploadEvidence(report: com.peto.ramap.domain.model.report.NewsReportEvidence): String {
        require(report.isValid()) { "Unsupported news report evidence" }
        val path = "${Uuid.random()}.${report.fileExtension}"
        client.storage.from(NEWS_REPORT_EVIDENCE_BUCKET).upload(path, report.bytes) {
            contentType = ContentType.parse(report.mimeType)
        }
        return path
    }

    private suspend fun deleteEvidence(path: String) {
        try {
            client.storage.from(NEWS_REPORT_EVIDENCE_BUCKET).delete(path)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Evidence cleanup must not hide the report submission result.
        }
    }

    companion object {
        private const val TABLE_NAME = "shop_information_reports"
        private const val PLACE_REPORT_TABLE_NAME = "unregistered_place_reports"
        private const val NEWS_REPORT_EVIDENCE_BUCKET = "news-report-evidence"
        private const val NEWS_REPORT_SUBMISSION_FUNCTION = "submit_news_report"
    }
}
