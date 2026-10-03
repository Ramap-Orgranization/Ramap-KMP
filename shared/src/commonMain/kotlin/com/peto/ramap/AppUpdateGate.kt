package com.peto.ramap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.update.AppUpdatePolicy
import com.peto.ramap.domain.repository.AppUpdateRepository
import com.peto.ramap.platform.AppVersionProvider
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.app_update_action
import ramap.shared.generated.resources.app_update_description
import ramap.shared.generated.resources.app_update_title

@Composable
internal fun AppUpdateGate(
    appUpdateRepository: AppUpdateRepository,
    appVersionProvider: AppVersionProvider,
    viewModel: AppUpdateGateViewModel =
        koinViewModel(parameters = { parametersOf(appUpdateRepository, appVersionProvider) }),
    content: @Composable () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val policy = uiState.policy

    if (uiState.loadState.isLoading(AppUpdateLoadKey.Policy)) {
        RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
        return
    }

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
        return
    }

    content()
}

internal fun shouldRequireAppUpdate(
    policy: AppUpdatePolicy?,
    buildNumber: Long,
    isStoreUrlSupported: Boolean,
): Boolean = policy != null && buildNumber < policy.minimumBuildNumber && isStoreUrlSupported
