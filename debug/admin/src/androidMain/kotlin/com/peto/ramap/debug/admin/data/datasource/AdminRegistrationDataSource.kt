package com.peto.ramap.debug.admin.data.datasource

import com.peto.ramap.debug.admin.data.model.AdminCorrectionPreview
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminEvidence
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.debug.admin.data.model.AdminShopName
import com.peto.ramap.debug.admin.data.model.request.CorrectionRequest
import com.peto.ramap.debug.admin.data.model.request.EventStatusRequest
import com.peto.ramap.debug.admin.data.model.request.PreviewRequest
import com.peto.ramap.debug.admin.data.model.request.RegisterRequest
import com.peto.ramap.domain.model.event.ShopEventType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.client.call.body
import io.ktor.http.ContentType
import kotlin.uuid.Uuid

internal class AdminRegistrationDataSource(
    private val client: SupabaseClient,
) {
    suspend fun fetchShopNames(): List<String> =
        client
            .from(SHOPS_TABLE)
            .select(columns = Columns.list(SHOP_NAME_COLUMN))
            .decodeList<AdminShopName>()
            .map(AdminShopName::name)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    suspend fun preview(
        shopName: String,
        feedback: String,
        sourceUrl: String,
        evidence: AdminEvidence?,
        isOperatingNotice: Boolean,
    ): AdminDraft {
        val evidencePath = if (evidence == null) null else uploadEvidence(evidence)
        return try {
            client.functions
                .invoke(
                    PREVIEW_FUNCTION,
                    PreviewRequest(
                        registrationType = if (isOperatingNotice) "operating_notice" else "event",
                        shopName = shopName.ifBlank { null },
                        feedback = feedback.ifBlank { null },
                        sourceUrl = sourceUrl.ifBlank { null },
                        evidencePath = evidencePath,
                    ),
                ).body<AdminDraft>()
        } catch (exception: Throwable) {
            if (evidencePath != null) deleteEvidence(evidencePath)
            throw exception
        }
    }

    suspend fun register(
        draft: AdminDraft,
        isOperatingNotice: Boolean,
        eventType: ShopEventType,
    ) {
        client.functions.invoke(
            REGISTER_FUNCTION,
            RegisterRequest(
                registrationType = if (isOperatingNotice) "operating_notice" else "event",
                shopName = draft.shopName.orEmpty(),
                title = draft.title.orEmpty(),
                eventType = eventType.name.lowercase(),
                startDate = draft.startDate.orEmpty(),
                endDate = draft.endDate,
                description = draft.description.orEmpty(),
                sourceUrl = draft.sourceUrl.orEmpty(),
                evidencePath = draft.evidencePath,
                noticeType = draft.noticeType,
                startTime = draft.startTime,
                endTime = draft.endTime,
                participants = draft.participants,
            ),
        )
    }

    suspend fun registerImageOnly(
        shopName: String,
        title: String,
        eventType: ShopEventType,
        startDate: String,
        endDate: String?,
        evidence: AdminEvidence,
    ) {
        val evidencePath = uploadEvidence(evidence)
        try {
            client.functions.invoke(
                REGISTER_FUNCTION,
                RegisterRequest(
                    registrationType = "event",
                    shopName = shopName,
                    title = title,
                    eventType = eventType.name.lowercase(),
                    startDate = startDate,
                    endDate = endDate,
                    description = "",
                    sourceUrl = "",
                    evidencePath = evidencePath,
                    noticeType = null,
                    startTime = null,
                    endTime = null,
                    imageOnly = true,
                ),
            )
        } catch (exception: Throwable) {
            deleteEvidence(evidencePath)
            throw exception
        }
    }

    suspend fun fetchManagedEvents(): List<AdminManagedEvent> = client.functions.invoke(EVENT_STATUS_FUNCTION, EventStatusRequest(action = "list")).body()

    suspend fun saveEventStatus(
        eventId: String,
        status: String,
        scope: String,
        reason: String?,
        startDate: String?,
        endDate: String?,
    ) {
        client.functions.invoke(
            EVENT_STATUS_FUNCTION,
            EventStatusRequest("update", eventId, status, scope, reason, startDate, endDate),
        )
    }

    suspend fun updateEvent(
        eventId: String,
        draft: AdminDraft,
        eventType: ShopEventType,
    ) {
        client.functions.invoke(
            EVENT_STATUS_FUNCTION,
            EventStatusRequest(
                action = "edit",
                eventId = eventId,
                title = draft.title,
                description = draft.description,
                eventType = eventType.name.lowercase(),
                startDate = draft.startDate,
                endDate = draft.endDate,
            ),
        )
    }

    suspend fun previewCorrection(request: String): AdminCorrectionPreview =
        client.functions
            .invoke(
                CORRECTION_FUNCTION,
                CorrectionRequest(
                    action = "preview",
                    request = request,
                ),
            ).body()

    suspend fun applyCorrection(
        preview: AdminCorrectionPreview,
        changes: com.peto.ramap.debug.admin.data.model.AdminCorrectionChanges,
    ) {
        client.functions.invoke(
            CORRECTION_FUNCTION,
            CorrectionRequest(
                action = "apply",
                registrationType = preview.registrationType,
                targetId = preview.targetId,
                changes = changes,
            ),
        )
    }

    private suspend fun uploadEvidence(evidence: AdminEvidence): String {
        val path = "${Uuid.random()}.${if (evidence.mimeType == "image/png") "png" else "jpg"}"
        client.storage.from(EVIDENCE_BUCKET).upload(path, evidence.bytes) {
            contentType = ContentType.parse(evidence.mimeType)
        }
        return path
    }

    private suspend fun deleteEvidence(path: String) {
        client.storage.from(EVIDENCE_BUCKET).delete(path)
    }

    private companion object {
        const val SHOPS_TABLE = "ramen_shops"
        const val SHOP_NAME_COLUMN = "name"
        const val EVIDENCE_BUCKET = "news-report-evidence"
        const val PREVIEW_FUNCTION = "preview-event"
        const val REGISTER_FUNCTION = "register-event"
        const val EVENT_STATUS_FUNCTION = "admin-event-status"
        const val CORRECTION_FUNCTION = "admin-correct-registration"
    }
}
