package com.peto.ramap.analytics

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

class FirebaseAnalyticsTracker : AnalyticsTracker {
    private val firebase: FirebaseAnalytics by lazy { Firebase.analytics }

    override fun logEvent(event: AnalyticsEvent) {
        firebase.logEvent(event.name) {
            event.params().forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Double -> param(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    else ->
                        error(
                            "Unsupported analytics parameter: " +
                                "key=$key, type=${value::class.simpleName}",
                        )
                }
            }
        }
    }

    override fun logScreenView(
        screenName: String,
        params: Map<String, Any>,
    ) {
        firebase.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)

            params.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Double -> param(key, value)
                    is Boolean -> param(key, if (value) 1L else 0L)
                    else ->
                        error(
                            "Unsupported screen parameter: " +
                                "key=$key, type=${value::class.simpleName}",
                        )
                }
            }
        }
    }

    override fun userProperty(
        key: String,
        value: String,
    ) {
        firebase.setUserProperty(key, value)
    }
}
