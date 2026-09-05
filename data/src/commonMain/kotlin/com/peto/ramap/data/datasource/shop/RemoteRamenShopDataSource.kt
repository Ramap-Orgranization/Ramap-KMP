package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ShopDetailResponse
import com.peto.ramap.data.model.ShopEventParticipantResponse
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.data.model.ShopLikeCountResponse
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.SearchQuery
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteRamenShopDataSource(
    private val client: SupabaseClient,
) : RamenShopDataSource {
    override suspend fun fetchShopDetail(shopId: String): ShopDetailResponse? =
        client.postgrest
            .rpc(
                function = FETCH_SHOP_DETAIL_RPC,
                parameters = buildJsonObject { put(SHOP_ID_PARAMETER, shopId) },
            ).decodeList<ShopDetailResponse>()
            .singleOrNull()

    override suspend fun fetchShopMenuUpdatedAt(shopId: String): String? =
        client.postgrest
            .rpc(
                function = FETCH_SHOP_MENU_UPDATED_AT_RPC,
                parameters = buildJsonObject { put(SHOP_ID_PARAMETER, shopId) },
            ).decodeAs<String?>()

    override suspend fun fetchShopLikeCount(shopId: String): Long =
        client
            .from(SHOP_BOOKMARK_COUNTS_VIEW)
            .select(columns = Columns.list(COLUMN_SHOP_ID, COLUMN_LIKE_COUNT)) {
                filter { eq(COLUMN_SHOP_ID, shopId) }
                limit(1)
            }.decodeSingleOrNull<ShopLikeCountResponse>()
            ?.likeCount
            ?: 0L

    override suspend fun fetchActiveEvents(): List<ShopEventResponse> =
        client
            .from(ACTIVE_EVENTS_VIEW)
            .select()
            .decodeList<ShopEventResponse>()

    override suspend fun fetchActiveEvent(eventId: String): ShopEventResponse? =
        client
            .from(ACTIVE_EVENTS_VIEW)
            .select {
                filter { eq(COLUMN_ID, eventId) }
                limit(1)
            }.decodeSingleOrNull<ShopEventResponse>()

    override suspend fun fetchEvent(eventId: String): ShopEventResponse? =
        client
            .from(ACTIVE_EVENTS_VIEW)
            .select {
                filter { eq(COLUMN_ID, eventId) }
                limit(1)
            }.decodeSingleOrNull<ShopEventResponse>()

    override suspend fun fetchActiveShopEvents(shopId: String): List<ShopEventResponse> =
        client
            .from(EVENT_VIEW)
            .select {
                filter { eq(COLUMN_SHOP_CONTEXT_ID, shopId) }
            }.decodeList<ShopEventResponse>()

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
                    gte(COLUMN_LAT, bounds.minLat)
                    lte(COLUMN_LAT, bounds.maxLat)
                    gte(COLUMN_LNG, bounds.minLng)
                    lte(COLUMN_LNG, bounds.maxLng)
                }
            }.decodeList()

    override suspend fun fetchRamenShopsByIds(shopIds: Set<String>): List<RamenShopResponse> {
        if (shopIds.isEmpty()) return emptyList()

        return client
            .from(TABLE_NAME)
            .select {
                filter {
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
        val searchFilter = ShopTextSearchFilter(pattern)

        return client
            .from(TABLE_NAME)
            .select {
                filter {
                    or {
                        searchFilter.columns.forEach { column ->
                            ilike(column, searchFilter.pattern)
                        }
                    }
                }
                limit(limit.toLong())
            }.decodeList()
    }

    companion object {
        private const val TABLE_NAME = "ramen_shops"
        private const val SHOP_BOOKMARK_COUNTS_VIEW = "shop_bookmark_counts"
        private const val EVENT_VIEW = "active_shop_events"
        private const val ACTIVE_EVENTS_VIEW = "active_events"
        private const val FETCH_SHOP_DETAIL_RPC = "fetch_shop_detail"
        private const val FETCH_SHOP_MENU_UPDATED_AT_RPC = "fetch_shop_menu_updated_at"
        private const val SHOP_ID_PARAMETER = "p_shop_id"
        private const val EVENT_PARTICIPANT_TABLE = "shop_event_participants"
        private const val COLUMN_SHOP_CONTEXT_ID = "shop_context_id"
        private const val COLUMN_EVENT_ID = "event_id"

        private const val COLUMN_ID = "id"
        private const val COLUMN_SHOP_ID = "shop_id"
        private const val COLUMN_LIKE_COUNT = "like_count"
        private const val COLUMN_LAT = "lat"
        private const val COLUMN_LNG = "lng"
    }
}
