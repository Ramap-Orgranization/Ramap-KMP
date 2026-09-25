package com.peto.ramap.domain.model.auth

actual fun supportedLoginTypes(): List<LoginType> = listOf(LoginType.APPLE, LoginType.KAKAO)
