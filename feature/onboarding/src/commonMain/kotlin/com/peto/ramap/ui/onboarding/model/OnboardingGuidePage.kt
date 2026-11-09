package com.peto.ramap.ui.onboarding.model

import androidx.compose.ui.geometry.Rect
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.onboarding_notification_menu_guide
import ramap.shared.generated.resources.onboarding_notification_menu_image_description
import ramap.shared.generated.resources.onboarding_notification_menu_screen
import ramap.shared.generated.resources.onboarding_notification_types_guide
import ramap.shared.generated.resources.onboarding_notification_types_image_description
import ramap.shared.generated.resources.onboarding_notification_types_screen
import ramap.shared.generated.resources.onboarding_review_map_guide
import ramap.shared.generated.resources.onboarding_review_map_image_description
import ramap.shared.generated.resources.onboarding_review_map_screen

internal enum class OnboardingGuidePage(
    val image: DrawableResource,
    val message: StringResource,
    val imageDescription: StringResource,
    val sourceBounds: Rect,
    val highlights: List<Rect>,
) {
    NotificationMenu(
        image = Res.drawable.onboarding_notification_menu_screen,
        message = Res.string.onboarding_notification_menu_guide,
        imageDescription = Res.string.onboarding_notification_menu_image_description,
        sourceBounds = Rect(0f, 86f, 1080f, 2116f),
        highlights = listOf(Rect(826f, 1202f, 916f, 1318f), Rect(493f, 1642f, 917f, 1770f)),
    ),
    NotificationTypes(
        image = Res.drawable.onboarding_notification_types_screen,
        message = Res.string.onboarding_notification_types_guide,
        imageDescription = Res.string.onboarding_notification_types_image_description,
        sourceBounds = Rect(60f, 180f, 1020f, 2070f),
        highlights = listOf(Rect(112f, 545f, 968f, 2060f)),
    ),
    ReviewMap(
        image = Res.drawable.onboarding_review_map_screen,
        message = Res.string.onboarding_review_map_guide,
        imageDescription = Res.string.onboarding_review_map_image_description,
        sourceBounds = Rect(0f, 86f, 1080f, 2116f),
        highlights = listOf(Rect(19f, 477f, 220f, 677f), Rect(750f, 466f, 964f, 674f)),
    ),
}
