package com.peto.ramap.ui.onboarding.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.onboarding.guide.component.OnboardingGuidePagePreview
import com.peto.ramap.ui.onboarding.guide.model.OnboardingGuidePage
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.onboarding_guide_next
import ramap.shared.generated.resources.onboarding_guide_skip

@Composable
public fun OnboardingGuidePageContent(
    page: OnboardingGuidePage,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingGuidePagePreview(
            page = page,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        )
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppButton(
                text = stringResource(Res.string.onboarding_guide_next),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                text = stringResource(Res.string.onboarding_guide_skip),
                onClick = onSkip,
                textColor = GrayColor.C300,
                textStyle = AppTextStyle.B1,
                backgroundColor = Color.Transparent,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopNotificationMenuGuidePreview() {
    GuidePagePreview(page = OnboardingGuidePage.NotificationMenu)
}

@Preview(showBackground = true)
@Composable
private fun ShopNotificationTypesGuidePreview() {
    GuidePagePreview(page = OnboardingGuidePage.NotificationTypes)
}

@Preview(showBackground = true)
@Composable
private fun ReviewMapGuidePreview() {
    GuidePagePreview(page = OnboardingGuidePage.ReviewMap)
}

@Composable
private fun GuidePagePreview(page: OnboardingGuidePage) {
    RamapTheme {
        OnboardingGuideLayout(page = page) {
            OnboardingGuidePageContent(page = page, onNext = {}, onSkip = {})
        }
    }
}
