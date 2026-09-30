package com.peto.ramap.debug.admin.data.datasource

internal interface AdminAccessDataSource {
    suspend fun hasAccess(): Boolean
}
