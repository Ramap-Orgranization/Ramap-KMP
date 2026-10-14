package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ShopDetailResponse
import com.peto.ramap.data.model.ShopEventParticipantResponse
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.SearchQuery
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteRamenShopDataSource(
    private val client: SupabaseClient,
) : RamenShopDataSource {
    override suspend fun fetchShopDetail(shopId: String): ShopDetailResponse =
        client.postgrest
            .rpc(
                function = FETCH_SHOP_DETAIL_RPC,
                parameters = buildJsonObject { put(SHOP_ID_PARAMETER, shopId) },
            ).decodeList<ShopDetailResponse>()
            .single()

    override suspend fun fetchActiveShopEvents(shopId: String): List<ShopEventResponse> =
        client.postgrest
            .rpc(
                function = FETCH_ACTIVE_SHOP_EVENTS_RPC,
                parameters = buildJsonObject { put(SHOP_ID_PARAMETER, shopId) },
            ).decodeList()

    override suspend fun fetchEvents(eventId: String?): List<ShopEventResponse> =
        client.postgrest
            .rpc(
                function = FETCH_ACTIVE_EVENTS_RPC,
                parameters =
                    buildJsonObject {
                        put(EVENT_ID_PARAMETER, eventId?.let { JsonPrimitive(it) } ?: JsonNull)
                    },
            ).decodeList()

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
        private const val FETCH_ACTIVE_EVENTS_RPC = "fetch_active_events"
        private const val FETCH_ACTIVE_SHOP_EVENTS_RPC = "fetch_active_shop_events"
        private const val FETCH_SHOP_DETAIL_RPC = "fetch_shop_detail_v2"
        private const val SHOP_ID_PARAMETER = "p_shop_id"
        private const val EVENT_ID_PARAMETER = "p_event_id"
        private const val EVENT_PARTICIPANT_TABLE = "shop_event_participants"
        private const val COLUMN_EVENT_ID = "event_id"
        private const val COLUMN_ID = "id"

        private const val COLUMN_LAT = "lat"
        private const val COLUMN_LNG = "lng"
    }
}
