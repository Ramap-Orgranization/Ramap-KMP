package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.NewShopResponse
import com.peto.ramap.domain.model.shop.ShopRegistrationMonth
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

internal class RemoteNewShopsDataSource(
    private val client: SupabaseClient,
) : NewShopsDataSource {
    override suspend fun fetchNewShops(month: ShopRegistrationMonth): List<NewShopResponse> {
        val shops = mutableListOf<NewShopResponse>()
        val startsAt = month.start.toString()
        val endsAt = month.endExclusive.toString()

        do {
            val page = fetchPage(startsAt = startsAt, endsAt = endsAt, cursor = shops.lastOrNull())
            shops.addAll(page)
        } while (page.size == PAGE_SIZE)

        return shops
    }

    private suspend fun fetchPage(
        startsAt: String,
        endsAt: String,
        cursor: NewShopResponse?,
    ): List<NewShopResponse> =
        client
            .from(VIEW_NAME)
            .select(columns = SELECTED_COLUMNS) {
                filter {
                    and {
                        gte(CREATED_AT, startsAt)
                        lt(CREATED_AT, endsAt)
                    }
                    if (cursor != null) {
                        or {
                            lt(CREATED_AT, cursor.createdAt)
                            and {
                                eq(CREATED_AT, cursor.createdAt)
                                lt(ID, cursor.id)
                            }
                        }
                    }
                }
                order(CREATED_AT, Order.DESCENDING)
                order(ID, Order.DESCENDING)
                limit(PAGE_SIZE.toLong())
            }.decodeList()

    private companion object {
        const val VIEW_NAME = "confirmed_new_shops"
        const val CREATED_AT = "created_at"
        const val ID = "id"
        const val PAGE_SIZE = 100

        val SELECTED_COLUMNS = Columns.list(ID, "name", CREATED_AT, "opening_date", "opening_kind", "cover_image_url")
    }
}
