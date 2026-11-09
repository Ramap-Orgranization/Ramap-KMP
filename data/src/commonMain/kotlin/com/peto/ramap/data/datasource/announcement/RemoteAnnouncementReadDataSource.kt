package com.peto.ramap.data.datasource.announcement

import com.peto.ramap.data.model.AnnouncementReadResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

internal class RemoteAnnouncementReadDataSource(
    private val client: SupabaseClient,
) : AnnouncementReadDataSource {
    override suspend fun fetchReadIds(accountId: String): Set<String> {
        val reads = mutableListOf<AnnouncementReadResponse>()
        do {
            val page =
                client
                    .from(TABLE_NAME)
                    .select {
                        filter { eq("user_id", accountId) }
                        order("announcement_id", Order.ASCENDING)
                        range(reads.size.toLong(), reads.size.toLong() + PAGE_SIZE - 1)
                    }.decodeList<AnnouncementReadResponse>()
            reads.addAll(page)
        } while (page.size == PAGE_SIZE)
        return reads.map { it.announcementId }.toSet()
    }

    override suspend fun markRead(
        announcementId: String,
        accountId: String,
    ) {
        client.from(TABLE_NAME).upsert(AnnouncementReadResponse(accountId, announcementId)) {
            onConflict = "user_id,announcement_id"
            ignoreDuplicates = true
        }
    }

    private companion object {
        const val TABLE_NAME = "service_announcement_reads"
        const val PAGE_SIZE = 100
    }
}
