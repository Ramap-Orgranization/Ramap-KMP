package com.peto.ramap.domain.repository

class ImportationException(
    val code: ImportationErrorCode,
) : Exception(code.name)
