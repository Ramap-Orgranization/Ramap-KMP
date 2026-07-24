package com.peto.ramap

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Kermit ERROR 이상 로그를 Firebase Crashlytics에 전달하는 [LogWriter].
 *
 * [BaseViewModel.handleError]의 `logger.e()` 호출이 자동으로 Crashlytics에 기록된다.
 */
internal class CrashlyticsLogWriter : LogWriter() {
    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun isLoggable(
        tag: String,
        severity: Severity,
    ): Boolean = severity >= Severity.Error

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        crashlytics.log("$tag: $message")
        throwable?.let { crashlytics.recordException(it) }
    }
}
