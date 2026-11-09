package com.peto.ramap.data.datasource.announcement

import com.peto.ramap.data.model.AnnouncementResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

internal class RemoteAnnouncementDataSource(
    private val client: SupabaseClient,
) : AnnouncementDataSource {
    override suspend fun fetchAnnouncements(): List<AnnouncementResponse> {
        val announcements = mutableListOf<AnnouncementResponse>()
        do {
            val page =
                client
                    .from(TABLE_NAME)
                    .select {
                        order("published_at", Order.DESCENDING)
                        order("id", Order.DESCENDING)
                        range(announcements.size.toLong(), announcements.size.toLong() + PAGE_SIZE - 1)
                    }.decodeList<AnnouncementResponse>()
            announcements.addAll(page)
        } while (page.size == PAGE_SIZE)
        return announcements.distinctBy { it.id }
    }

    private companion object {
        const val TABLE_NAME = "service_announcements"
        const val PAGE_SIZE = 100
    }
}
