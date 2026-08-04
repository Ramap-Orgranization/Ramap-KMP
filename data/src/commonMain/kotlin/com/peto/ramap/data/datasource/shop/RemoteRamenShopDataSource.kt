package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ShopEventParticipantResponse
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.network.config.RamapSecrets
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

internal class RemoteRamenShopDataSource(
    private val client: SupabaseClient,
) : RamenShopDataSource {
    override suspend fun fetchActiveEvents(): List<ShopEventResponse> =
        client
            .from(ACTIVE_EVENTS_VIEW)
            .select()
            .decodeList<ShopEventResponse>()
            .map(::resolveProfileImageUrl)

    override suspend fun fetchActiveEvent(eventId: String): ShopEventResponse? =
        client
            .from(ACTIVE_EVENTS_VIEW)
            .select {
                filter { eq(COLUMN_ID, eventId) }
                limit(1)
            }.decodeSingleOrNull<ShopEventResponse>()
            ?.let(::resolveProfileImageUrl)

    override suspend fun fetchActiveShopEvents(shopId: String): List<ShopEventResponse> =
        client
            .from(EVENT_VIEW)
            .select {
                filter { eq(COLUMN_SHOP_CONTEXT_ID, shopId) }
            }.decodeList<ShopEventResponse>()
            .map(::resolveProfileImageUrl)

    override suspend fun fetchShopEventParticipants(eventId: String): List<ShopEventParticipantResponse> =
        client
            .from(EVENT_PARTICIPANT_TABLE)
            .select {
                filter { eq(COLUMN_EVENT_ID, eventId) }
            }.decodeList()

    override suspend fun fetchRamenShops(bounds: MapBounds): List<RamenShopResponse> =
        client
            .from(TABLE_NAME)
            .select {
                filter {
                    eq(COLUMN_IS_VISIBLE, true)
                    and {
                        gte(COLUMN_LAT, bounds.minLat)
                        lte(COLUMN_LAT, bounds.maxLat)
                        gte(COLUMN_LNG, bounds.minLng)
                        lte(COLUMN_LNG, bounds.maxLng)
                    }
                }
            }.decodeList()

    override suspend fun fetchRamenShopsByIds(shopIds: Set<String>): List<RamenShopResponse> {
        if (shopIds.isEmpty()) return emptyList()

        return client
            .from(TABLE_NAME)
            .select {
                filter {
                    eq(COLUMN_IS_VISIBLE, true)
                    isIn(COLUMN_ID, shopIds.toList())
                }
            }.decodeList()
    }

    override suspend fun searchRamenShops(
        query: SearchQuery,
        limit: Int,
    ): List<RamenShopResponse> {
        val normalizedQuery = query.normalizeShopSearchQuery()

        if (normalizedQuery.value.isBlank()) {
            return emptyList()
        }

        return searchByTextFields(
            pattern = normalizedQuery.ilikePattern(),
            limit = limit,
        )
    }

    private suspend fun searchByTextFields(
        pattern: String,
        limit: Int,
    ): List<RamenShopResponse> {
        val searchFilter = ShopTextSearchFilter.forVisibleShops(pattern)

        return client
            .from(TABLE_NAME)
            .select {
                filter {
                    eq(COLUMN_IS_VISIBLE, searchFilter.isVisible)
                    or {
                        searchFilter.columns.forEach { column ->
                            ilike(column, searchFilter.pattern)
                        }
                    }
                }
                limit(limit.toLong())
            }.decodeList()
    }

    private fun resolveProfileImageUrl(event: ShopEventResponse): ShopEventResponse =
        event.copy(
            venueProfileImageUrl =
                event.venueProfileImageUrl?.let { path ->
                    "${RamapSecrets.supabaseUrl}/storage/v1/object/public/$PROFILE_BUCKET/$path"
                },
        )

    companion object {
        private const val TABLE_NAME = "shops"
        private const val EVENT_VIEW = "active_shop_events"
        private const val ACTIVE_EVENTS_VIEW = "active_events"
        private const val PROFILE_BUCKET = "shop-profile-images"
        private const val EVENT_PARTICIPANT_TABLE = "shop_event_participants"
        private const val COLUMN_SHOP_CONTEXT_ID = "shop_context_id"
        private const val COLUMN_EVENT_ID = "event_id"

        private const val COLUMN_ID = "id"
        private const val COLUMN_IS_VISIBLE = "is_visible"
        private const val COLUMN_LAT = "lat"
        private const val COLUMN_LNG = "lng"
    }
}
