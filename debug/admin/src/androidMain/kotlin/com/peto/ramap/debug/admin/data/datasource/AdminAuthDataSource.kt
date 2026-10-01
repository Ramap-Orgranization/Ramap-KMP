package com.peto.ramap.debug.admin.data.datasource

internal interface AdminAuthDataSource {
    suspend fun signIn()
}
