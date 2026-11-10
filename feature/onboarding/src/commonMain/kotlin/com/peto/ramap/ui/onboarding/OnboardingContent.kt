package com.peto.ramap.ui.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.contract.OnboardingUiState
import com.peto.ramap.ui.onboarding.permission.PermissionGuideContent

@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
    permissionContent: @Composable () -> Unit = {
        PermissionGuideContent(
            isConfirming = false,
            onConfirm = {},
        )
    },
    guideContent: @Composable () -> Unit,
) {
    when (uiState.step) {
        OnboardingStep.Initializing,
        OnboardingStep.Completed,
        -> RamenLoadingIndicator(modifier = Modifier.fillMaxSize())

        OnboardingStep.PermissionGuide -> permissionContent()

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
