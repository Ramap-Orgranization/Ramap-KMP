package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.importation.ImportationPreview

interface ImportationRepository {
    suspend fun analyze(url: String): RamapResult<ImportationPreview>
}
