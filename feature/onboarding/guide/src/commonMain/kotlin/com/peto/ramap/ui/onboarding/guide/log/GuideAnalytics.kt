package com.peto.ramap.ui.onboarding.guide.log

public interface GuideAnalytics {
    fun viewStage(stage: OnboardingAnalyticsStage)

    fun guideAction(
        stage: OnboardingAnalyticsStage,
        action: String,
    )

    fun shopGoalReached(savedCount: Int)
}
