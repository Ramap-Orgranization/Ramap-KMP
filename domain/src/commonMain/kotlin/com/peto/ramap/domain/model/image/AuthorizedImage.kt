package com.peto.ramap.domain.model.image

data class AuthorizedImage(
    val url: String,
    val reference: String,
    val scope: ImageAccessScope,
) {
    val memoryKey: String get() = scope.memoryKey(reference)
}
