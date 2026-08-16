package com.peto.ramap.domain.model.update

data class AppUpdatePolicy(
    val minimumBuildNumber: Long,
    val storeUrl: String,
)
