package com.peto.ramap.data.datasource.report

import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class RemoteShopReportDataSource(
    private val client: SupabaseClient,
) : ShopReportDataSource {
    override suspend fun submitShopInformationReport(report: ShopInformationReportRequest) {
        client
            .from(TABLE_NAME)
            .insert(report)
    }

    override suspend fun submitUnregisteredPlaceReport(report: UnregisteredPlaceReportRequest) {
        client
            .from(PLACE_REPORT_TABLE_NAME)
            .insert(report)
    }

    companion object {
        private const val TABLE_NAME = "shop_information_reports"
        private const val PLACE_REPORT_TABLE_NAME = "unregistered_place_reports"
    }
}
