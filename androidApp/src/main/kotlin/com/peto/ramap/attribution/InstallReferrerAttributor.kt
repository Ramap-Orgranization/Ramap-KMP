package com.peto.ramap.attribution

import android.content.Context
import androidx.core.content.edit
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.common.attribution.InstallAttributed

/**
 * Google Play Install Referrer를 한 번만 수집해 앱 설치 유입 정보를 분석 이벤트로 기록한다.
 *
 * 수집 시도 여부는 로컬 저장소에 보관해 앱을 다시 실행해도 Install Referrer 서비스에 중복 연결하지 않는다.
 */
class InstallReferrerAttributor(
    context: Context,
    private val analyticsTracker: AnalyticsTracker,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val applicationContext = context.applicationContext

    /**
     * 아직 시도하지 않은 경우에만 Google Play Install Referrer 서비스에 연결해 유입 정보를 수집한다.
     */
    fun collectOnce() {
        if (preferences.getBoolean(KEY_COLLECTION_ATTEMPTED, false)) return
        preferences.edit { putBoolean(KEY_COLLECTION_ATTEMPTED, true) }

        val client = InstallReferrerClient.newBuilder(applicationContext).build()
        client.startConnection(
            object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        logAttribution(client.installReferrer.installReferrer)
                    }
                    client.endConnection()
                }

                override fun onInstallReferrerServiceDisconnected() = Unit
            },
        )
    }

    private fun logAttribution(referrer: String) {
        val attribution = InstallReferrerParser.parse(referrer)
        if (attribution.isEmpty()) return
        analyticsTracker.logEvent(
            InstallAttributed(
                clickId = attribution.clickId,
                source = attribution.source,
                campaign = attribution.campaign,
                shopId = attribution.shopId,
            ),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "install_referrer_preferences"
        const val KEY_COLLECTION_ATTEMPTED = "collection_attempted"
    }
}
