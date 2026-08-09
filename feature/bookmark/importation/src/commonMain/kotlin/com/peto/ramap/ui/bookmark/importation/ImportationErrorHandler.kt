package com.peto.ramap.ui.bookmark.importation

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.domain.repository.ImportationErrorCode
import com.peto.ramap.domain.repository.ImportationException
import com.peto.ramap.ui.bookmark.importation.contract.ImportationError
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.importation_error_analyze
import ramap.shared.generated.resources.importation_error_confirm
import ramap.shared.generated.resources.importation_error_invalid_url
import ramap.shared.generated.resources.importation_error_personalization
import ramap.shared.generated.resources.importation_error_provider_failure
import ramap.shared.generated.resources.importation_error_unavailable_list
import ramap.shared.generated.resources.importation_error_unsupported_url
import ramap.shared.generated.resources.login_required_message

object ImportationErrorHandler {
    private const val HTTP_UNAUTHORIZED = 401

    fun resolveAnalyzeError(error: RamapError): ImportationError =
        when ((error.cause as? ImportationException)?.code) {
            ImportationErrorCode.UNSUPPORTED_URL -> ImportationError.UNSUPPORTED_URL
            ImportationErrorCode.UNAVAILABLE_LIST -> ImportationError.UNAVAILABLE_LIST
            ImportationErrorCode.PROVIDER_FAILURE -> ImportationError.PROVIDER_FAILURE
            null -> ImportationError.ANALYZE_FAILED
        }

    fun resolveConfirmError(error: RamapError): ImportationError =
        if (error is RamapError.Http && error.status == HTTP_UNAUTHORIZED) {
            ImportationError.LOGIN_REQUIRED
        } else {
            ImportationError.CONFIRM_FAILED
        }

    fun resourceFor(error: ImportationError): StringResource =
        when (error) {
            ImportationError.INVALID_URL -> Res.string.importation_error_invalid_url
            ImportationError.UNSUPPORTED_URL -> Res.string.importation_error_unsupported_url
            ImportationError.UNAVAILABLE_LIST -> Res.string.importation_error_unavailable_list
            ImportationError.PROVIDER_FAILURE -> Res.string.importation_error_provider_failure
            ImportationError.PERSONALIZATION_UNAVAILABLE -> Res.string.importation_error_personalization
            ImportationError.LOGIN_REQUIRED -> Res.string.login_required_message
            ImportationError.ANALYZE_FAILED -> Res.string.importation_error_analyze
            ImportationError.CONFIRM_FAILED -> Res.string.importation_error_confirm
        }
}
