package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.SearchQuery
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class RemoteRamenShopDataSource(
    private val client: SupabaseClient,
) : RamenShopDataSource {
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
    ): List<RamenShopResponse> =
        client
            .from(TABLE_NAME)
            .select {
                filter {
                    eq(COLUMN_IS_VISIBLE, true)
                    or {
                        SEARCH_COLUMNS.forEach { column ->
                            ilike(column, pattern)
                        }
                    }
                }
                limit(limit.toLong())
            }.decodeList()

    companion object {
        private const val TABLE_NAME = "shops"

        private const val COLUMN_IS_VISIBLE = "is_visible"
        private const val COLUMN_LAT = "lat"
        private const val COLUMN_LNG = "lng"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_ADDRESS = "address"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_BUSINESS_HOURS = "business_hours"

        private val SEARCH_COLUMNS =
            listOf(
                COLUMN_NAME,
                COLUMN_ADDRESS,
                COLUMN_PHONE,
                COLUMN_BUSINESS_HOURS,
            )
    }
}
