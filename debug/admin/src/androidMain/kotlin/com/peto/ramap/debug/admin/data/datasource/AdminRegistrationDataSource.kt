package com.peto.ramap.debug.admin.data.datasource

import com.peto.ramap.debug.admin.data.model.AdminCorrectionPreview
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminEvidence
import com.peto.ramap.debug.admin.data.model.AdminExternalVenue
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.debug.admin.data.model.AdminShopHours
import com.peto.ramap.domain.model.event.ShopEventType

internal interface AdminRegistrationDataSource {
    suspend fun fetchShopNames(): List<String>

    suspend fun fetchExternalVenues(): List<AdminExternalVenue>

    suspend fun fetchShopHours(shopName: String): AdminShopHours?

    suspend fun preview(
        shopName: String,
        feedback: String,
        sourceUrl: String,
        evidence: AdminEvidence?,
        isOperatingNotice: Boolean,
    ): AdminDraft

    suspend fun register(
        draft: AdminDraft,
        isOperatingNotice: Boolean,
        eventType: ShopEventType,
    )

    suspend fun registerImageOnly(
        shopName: String,
        title: String,
        eventType: ShopEventType,
        startDate: String,
        endDate: String?,
        evidence: AdminEvidence,
    )

    suspend fun fetchManagedEvents(): List<AdminManagedEvent>

    suspend fun saveEventStatus(
        eventId: String,
        status: String,
        scope: String,
        reason: String?,
        startDate: String?,
        endDate: String?,
    )

    suspend fun updateEvent(
        eventId: String,
        draft: AdminDraft,
        eventType: ShopEventType,
    )

    suspend fun previewCorrection(request: String): AdminCorrectionPreview

    suspend fun applyCorrection(
        preview: AdminCorrectionPreview,
        changes: com.peto.ramap.debug.admin.data.model.AdminCorrectionChanges,
    )
}
