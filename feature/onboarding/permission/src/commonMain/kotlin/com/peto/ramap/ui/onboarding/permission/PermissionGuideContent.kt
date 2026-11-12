package com.peto.ramap.ui.onboarding.permission

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SurfaceColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_current_location
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.onboarding_permission_confirm
import ramap.shared.generated.resources.onboarding_permission_location_description
import ramap.shared.generated.resources.onboarding_permission_location_title
import ramap.shared.generated.resources.onboarding_permission_notice
import ramap.shared.generated.resources.onboarding_permission_notification_description
import ramap.shared.generated.resources.onboarding_permission_notification_title
import ramap.shared.generated.resources.onboarding_permission_optional
import ramap.shared.generated.resources.onboarding_permission_title

@Composable
fun PermissionGuideContent(
    isConfirming: Boolean,
    onConfirm: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(SurfaceColor.Background)
                .safeDrawingPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            AppText(
                text = stringResource(Res.string.onboarding_permission_title),
                style = AppTextStyle.H3,
                color = GrayColor.C500,
                modifier = Modifier.semantics { heading() },
            )
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PermissionGuideItem(
                    title = Res.string.onboarding_permission_location_title,
                    description = Res.string.onboarding_permission_location_description,
                    icon = Res.drawable.ic_current_location,
                )
                PermissionGuideItem(
                    title = Res.string.onboarding_permission_notification_title,
                    description = Res.string.onboarding_permission_notification_description,
                    icon = Res.drawable.ic_notification,
                )
            }
            AppText(
                text = stringResource(Res.string.onboarding_permission_notice),
                style = AppTextStyle.B2,
                color = GrayColor.C300,
            )
        }
        AppButton(
            text = stringResource(Res.string.onboarding_permission_confirm),
            onClick = onConfirm,
            isLoading = isConfirming,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun PermissionGuideItem(
    title: StringResource,
    description: StringResource,
    icon: DrawableResource,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(GrayColor.C050, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GrayColor.C500),
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = stringResource(title),
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                )
                AppText(
                    text = stringResource(Res.string.onboarding_permission_optional),
                    style = AppTextStyle.C1,
                    color = GrayColor.C300,
                    modifier =
                        Modifier
                            .background(GrayColor.C050, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            AppText(
                text = stringResource(description),
                style = AppTextStyle.B2,
                color = GrayColor.C300,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionGuideContentPreview() {
    RamapTheme {
        PermissionGuideContent(
            isConfirming = false,
            onConfirm = {},
        )
    }
}
