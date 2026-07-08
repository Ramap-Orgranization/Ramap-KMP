package com.peto.ramap.data.datasource.report

import com.peto.ramap.data.model.ShopInformationReportRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class RemoteShopReportDataSource(
    private val client: SupabaseClient,
) : ShopReportDataSource {
    override suspend fun insert(report: ShopInformationReportRequest) {
        client
            .from(TABLE_NAME)
            .insert(report)
    }

    companion object {
        private const val TABLE_NAME = "shop_information_reports"
    }
}
