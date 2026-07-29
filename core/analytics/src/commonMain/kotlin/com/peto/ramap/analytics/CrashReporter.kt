package com.peto.ramap.analytics

/**
 * 비정상 종료 및 오류를 Crashlytics에 보고하는 리포터.
 *
 * Android에서는 Firebase Crashlytics, iOS에서는 Firebase iOS SDK 구현체가 주입된다.
 * Kermit ERROR 로그가 [log]와 [recordException]을 통해 자동 전달된다.
 */
interface CrashReporter {
    /**
     * 크래시 컨텍스트에 breadcrumb 메시지를 남긴다.
     *
     * @param message 기록할 메시지
     */
    fun log(message: String)

    /**
     * 치명적이지 않은 예외를 Crashlytics에 기록한다.
     *
     * @param throwable 기록할 예외
     */
    fun recordException(throwable: Throwable)

    /**
     * 크래시 리포트에 포함될 커스텀 키-값을 설정한다.
     *
     * @param key 커스텀 키
     * @param value 커스텀 값
     */
    fun setCustomKey(
        key: String,
        value: String,
    )
}
