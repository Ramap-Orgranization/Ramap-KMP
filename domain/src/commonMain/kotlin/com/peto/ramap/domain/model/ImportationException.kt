package com.peto.ramap.domain.model

class ImportationException(
    val code: ImportationErrorCode,
) : Exception(code.name)
