package com.peto.ramap.debug.admin

import com.peto.ramap.debug.admin.data.datasource.AdminRegistrationDataSource
import com.peto.ramap.debug.admin.data.model.AdminCorrectionChanges
import com.peto.ramap.debug.admin.data.model.AdminCorrectionPreview
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminEvidence
import com.peto.ramap.debug.admin.data.model.AdminExternalVenue
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.debug.admin.data.model.AdminShopHours
import com.peto.ramap.domain.model.event.ShopEventType
import kotlinx.coroutines.CompletableDeferred

internal class FakeAdminRegistrationDataSource : AdminRegistrationDataSource {
    var events = listOf(event("active"), event("ended", "2026-10-01", "2026-10-10"), event("old-renewal", "2026-08-01", null, "store_renewal"), event("upcoming", "2026-10-12", "2026-10-13"))
    var eventsFailure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null
    var saves = 0
    var edits = 0
    var registrations = 0
    var correctionApplications = 0
    var savedDraft: AdminDraft? = null
    var previewResult: AdminDraft? = null
    var registeredAsOperatingNotice: Boolean? = null

    override suspend fun fetchShopNames() = listOf("테스트 매장")

    override suspend fun fetchExternalVenues() = emptyList<AdminExternalVenue>()

    override suspend fun fetchShopHours(shopName: String): AdminShopHours? = null

    override suspend fun fetchManagedEvents(): List<AdminManagedEvent> {
        eventsFailure?.let { throw it }
        return events
    }

    override suspend fun saveEventStatus(
        eventId: String,
        status: String,
        scope: String,
        reason: String?,
        startDate: String?,
        endDate: String?,
    ) {
        gate?.await()
        saves++
        events = events.map { if (it.id == eventId) it.copy(soldOutDates = if (status == "sold_out") listOf(TODAY) else it.soldOutDates, cancelledDates = if (status == "cancelled") listOf(TODAY) else it.cancelledDates) else it }
    }

    override suspend fun preview(
        shopName: String,
        feedback: String,
        sourceUrl: String,
        evidence: AdminEvidence?,
        isOperatingNotice: Boolean,
    ): AdminDraft {
        gate?.await()
        previewResult?.let { return it }
        return AdminDraft(
            shopName = shopName.ifBlank { "테스트 매장" },
            title = "등록 테스트",
            description = "테스트 내용",
            eventType = if (isOperatingNotice) null else "limited_menu",
            startDate = TODAY,
            endDate = TODAY,
            sourceUrl = sourceUrl,
            noticeType = if (isOperatingNotice) "early_close" else null,
            endTime = if (isOperatingNotice) "15:00" else null,
        )
    }

    override suspend fun register(
        draft: AdminDraft,
        isOperatingNotice: Boolean,
        eventType: ShopEventType,
    ) {
        gate?.await()
        registrations++
        savedDraft = draft
        registeredAsOperatingNotice = isOperatingNotice
        events = events + event("registered").copy(title = draft.title.orEmpty())
    }

    override suspend fun registerImageOnly(
        shopName: String,
        title: String,
        eventType: ShopEventType,
        startDate: String,
        endDate: String?,
        evidence: AdminEvidence,
    ) {
        registrations++
        events = events + event("registered").copy(title = title)
    }

    override suspend fun updateEvent(
        eventId: String,
        draft: AdminDraft,
        eventType: ShopEventType,
    ) {
        gate?.await()
        edits++
        savedDraft = draft
        events = events.map { if (it.id == eventId) it.copy(title = draft.title.orEmpty(), description = draft.description.orEmpty(), startDate = draft.startDate!!, endDate = draft.endDate, eventType = eventType.name.lowercase()) else it }
    }

    override suspend fun previewCorrection(request: String) = AdminCorrectionPreview("event", "active", "수정 확인 테스트", AdminCorrectionChanges(title = "수정된 제목"))

    override suspend fun applyCorrection(
        preview: AdminCorrectionPreview,
        changes: AdminCorrectionChanges,
    ) {
        gate?.await()
        correctionApplications++
        events = events.map { if (it.id == preview.targetId) it.copy(title = changes.title!!) else it }
    }

    companion object {
        const val TODAY = "2026-10-11"

        fun event(
            id: String,
            start: String = TODAY,
            end: String? = TODAY,
            type: String = "limited_menu",
        ) = AdminManagedEvent(id = id, title = "이벤트 $id", startDate = start, endDate = end, shopName = "테스트 매장", description = "테스트 내용", eventType = type)
    }
}
