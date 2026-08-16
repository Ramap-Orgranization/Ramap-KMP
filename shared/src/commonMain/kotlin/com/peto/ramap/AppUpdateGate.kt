package com.peto.ramap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.update.AppUpdatePolicy
import com.peto.ramap.domain.repository.AppUpdateRepository
import com.peto.ramap.platform.AppVersionProvider
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.app_update_action
import ramap.shared.generated.resources.app_update_description
import ramap.shared.generated.resources.app_update_title

@Composable
internal fun AppUpdateGate(
    appUpdateRepository: AppUpdateRepository,
    appVersionProvider: AppVersionProvider,
    content: @Composable () -> Unit,
) {
    var policy by remember { mutableStateOf<AppUpdatePolicy?>(null) }
    var hasCheckedPolicy by remember { mutableStateOf(false) }

    LaunchedEffect(appUpdateRepository, appVersionProvider) {
        policy = fetchAppUpdatePolicy(appUpdateRepository, appVersionProvider.platform)
        hasCheckedPolicy = true
    }

    if (!hasCheckedPolicy) {
        RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
        return
    }

    content()
    val isUpdateRequired =
        shouldRequireAppUpdate(
            policy = policy,
            buildNumber = appVersionProvider.buildNumber,
            isStoreUrlSupported = ExternalUriOpener.isSupportedWebUri(policy?.storeUrl.orEmpty()),
        )
    if (isUpdateRequired) {
        CommonDialog(
            visible = true,
            confirmText = stringResource(Res.string.app_update_action),
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            onDismissRequest = {},
            content = {
                AppText(
                    text = stringResource(Res.string.app_update_title),
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                    textAlign = TextAlign.Center,
                )
                AppText(
                    text = stringResource(Res.string.app_update_description),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                    textAlign = TextAlign.Center,
                )
            },
            onConfirm = { ExternalUriOpener.startAppUpdate(policy?.storeUrl.orEmpty()) },
        )
    }
}

internal fun shouldRequireAppUpdate(
    policy: AppUpdatePolicy?,
    buildNumber: Long,
    isStoreUrlSupported: Boolean,
): Boolean = policy != null && buildNumber < policy.minimumBuildNumber && isStoreUrlSupported

private suspend fun fetchAppUpdatePolicy(
    repository: AppUpdateRepository,
    platform: String,
): AppUpdatePolicy? =
    withTimeoutOrNull(UPDATE_POLICY_TIMEOUT_MS) {
        when (val result = repository.fetchAppUpdatePolicy(platform)) {
            is RamapResult.Success -> result.data
            is RamapResult.Error -> null
        }
    }

private const val UPDATE_POLICY_TIMEOUT_MS = 5_000L
