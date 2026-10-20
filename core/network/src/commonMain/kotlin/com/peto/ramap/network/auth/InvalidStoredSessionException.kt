package com.peto.ramap.network.auth

/** 복구할 수 없는 저장 데이터 손상만 나타내며 일시적인 저장소 접근 실패와 구분합니다. */
internal class InvalidStoredSessionException : Exception("Stored session is invalid")
