package com.peto.ramap.analytics.common.login

class LoginFailureException : RuntimeException(MESSAGE) {
    private companion object {
        const val MESSAGE = "Login failed"
    }
}
