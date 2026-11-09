package com.peto.ramap.ui.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingLoadKey
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.contract.OnboardingUiState

@Composable
internal fun OnboardingContent(
    uiState: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
    guideContent: @Composable () -> Unit,
) {
    when (uiState.step) {
        OnboardingStep.Initializing,
        OnboardingStep.Completed,
        -> RamenLoadingIndicator(modifier = Modifier.fillMaxSize())

        OnboardingStep.PermissionGuide ->
            PermissionGuideContent(
                isConfirming = uiState.loadState.isLoading(OnboardingLoadKey.ConfirmPermissionGuide),
                onConfirm = { onIntent(OnboardingIntent.ConfirmPermissionGuide) },
            )

        OnboardingStep.Guide -> guideContent()
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingContentPreview() {
    RamapTheme {
        OnboardingContent(
            uiState = OnboardingUiState(step = OnboardingStep.PermissionGuide),
            onIntent = {},
            guideContent = {},
        )
    }
}
