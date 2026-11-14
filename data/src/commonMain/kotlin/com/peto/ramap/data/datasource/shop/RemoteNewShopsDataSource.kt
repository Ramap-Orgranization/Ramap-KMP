package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.NewShopResponse
import com.peto.ramap.domain.model.shop.ShopOpeningMonth
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

internal class RemoteNewShopsDataSource(
    private val client: SupabaseClient,
) : NewShopsDataSource {
    override suspend fun fetchNewShops(month: ShopOpeningMonth): List<NewShopResponse> {
        val shops = mutableListOf<NewShopResponse>()

        do {
            val page = fetchPage(month, cursor = shops.lastOrNull())
            shops.addAll(page)
        } while (page.size == PAGE_SIZE)

        return shops
    }

    private suspend fun fetchPage(
        month: ShopOpeningMonth,
        cursor: NewShopResponse?,
    ): List<NewShopResponse> =
        client
            .from(VIEW_NAME)
            .select(columns = SELECTED_COLUMNS) {
                filter {
                    and {
                        gte(OPENING_DATE, month.firstDay.toString())
                        lt(OPENING_DATE, month.endExclusive.toString())
                    }
                    if (cursor != null) {
                        val lastOpeningDate = cursor.openingDate.toString()
                        or {
                            lt(OPENING_DATE, lastOpeningDate)
                            and {
                                eq(OPENING_DATE, lastOpeningDate)
                                lt(ID, cursor.id)
                            }
                        }
                    }
                }
                order(OPENING_DATE, Order.DESCENDING)
                order(ID, Order.DESCENDING)
                limit(PAGE_SIZE.toLong())
            }.decodeList()

    private companion object {
        const val VIEW_NAME = "current_month_opened_shops"
        const val OPENING_DATE = "opening_date"
        const val ID = "id"
        const val PAGE_SIZE = 100

        val SELECTED_COLUMNS = Columns.list(ID, "name", OPENING_DATE, "opening_kind", "cover_image_url")
    }
}
