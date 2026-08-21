package com.peto.ramap.data.datasource.notice

import com.peto.ramap.data.model.OperatingNoticeResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.datetime.LocalDate

internal class RemoteOperatingNoticeDataSource(
    private val client: SupabaseClient,
) : OperatingNoticeDataSource {
    override suspend fun fetchApprovedOperatingNotices(today: LocalDate): List<OperatingNoticeResponse> =
        client
            .from(TABLE_NAME)
            .select {
                filter {
                    or {
                        filter(COLUMN_END_DATE, FilterOperator.IS, null)
                        gte(COLUMN_END_DATE, today.toString())
                    }
                }
            }.decodeList()

    override suspend fun fetchApprovedShopOperatingNotices(
        shopId: String,
        today: LocalDate,
    ): List<OperatingNoticeResponse> =
        client
            .from(TABLE_NAME)
            .select {
                filter {
                    eq(COLUMN_SHOP_ID, shopId)
                    or {
                        filter(COLUMN_END_DATE, FilterOperator.IS, null)
                        gte(COLUMN_END_DATE, today.toString())
                    }
                }
            }.decodeList()

    private companion object {
        const val TABLE_NAME = "shop_operating_notices"
        const val COLUMN_END_DATE = "end_date"
        const val COLUMN_SHOP_ID = "shop_id"
    }
}
