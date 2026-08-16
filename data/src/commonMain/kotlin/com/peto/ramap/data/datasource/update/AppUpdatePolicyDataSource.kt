package com.peto.ramap.data.datasource.update

import com.peto.ramap.data.model.AppUpdatePolicyResponse

internal interface AppUpdatePolicyDataSource {
    suspend fun fetchAppUpdatePolicy(platform: String): AppUpdatePolicyResponse?
}
