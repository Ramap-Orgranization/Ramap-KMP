package com.peto.ramap.attribution

data class InstallReferrerAttribution(
    val clickId: String? = null,
    val source: String? = null,
    val campaign: String? = null,
    val shopId: String? = null,
) {
    fun isEmpty(): Boolean = clickId == null && source == null && campaign == null && shopId == null
}
