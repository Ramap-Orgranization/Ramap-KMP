package com.peto.ramap.ui.bookmark.importation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.SettingsPage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.importation_guide_kakao_step1
import ramap.shared.generated.resources.importation_guide_kakao_step1_image_description
import ramap.shared.generated.resources.importation_guide_kakao_step2
import ramap.shared.generated.resources.importation_guide_kakao_step2_image_description
import ramap.shared.generated.resources.importation_guide_kakao_title
import ramap.shared.generated.resources.importation_guide_naver_step1
import ramap.shared.generated.resources.importation_guide_naver_step1_image_description
import ramap.shared.generated.resources.importation_guide_naver_step2
import ramap.shared.generated.resources.importation_guide_naver_step2_image_description
import ramap.shared.generated.resources.importation_guide_naver_title
import ramap.shared.generated.resources.importation_guide_public_notice
import ramap.shared.generated.resources.importation_guide_screen_title
import ramap.shared.generated.resources.kakao_guide1
import ramap.shared.generated.resources.kakao_guide2
import ramap.shared.generated.resources.naver_guide1
import ramap.shared.generated.resources.naver_guide2

@Composable
fun ImportationGuideRoute(onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    ImportationGuideContent(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { selectedTabIndex = it },
        onBack = onBack,
    )
}

@Composable
private fun ImportationGuideContent(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onBack: () -> Unit,
) {
    SettingsPage(
        title = Res.string.importation_guide_screen_title,
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppText(
                text = stringResource(Res.string.importation_guide_public_notice),
                style = AppTextStyle.B2,
                color = GrayColor.C300,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { onTabSelected(0) },
                    text = {
                        AppText(
                            text = stringResource(Res.string.importation_guide_kakao_title),
                            style = AppTextStyle.B1,
                            color = GrayColor.C500,
                        )
                    },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { onTabSelected(1) },
                    text = {
                        AppText(
                            text = stringResource(Res.string.importation_guide_naver_title),
                            style = AppTextStyle.B1,
                            color = GrayColor.C500,
                        )
                    },
                )
            }
            if (selectedTabIndex == 0) {
                ImportationGuideSection(
                    firstStep = stringResource(Res.string.importation_guide_kakao_step1),
                    firstImageDescription =
                        stringResource(Res.string.importation_guide_kakao_step1_image_description),
                    firstImage = Res.drawable.kakao_guide1,
                    secondStep = stringResource(Res.string.importation_guide_kakao_step2),
                    secondImageDescription =
                        stringResource(Res.string.importation_guide_kakao_step2_image_description),
                    secondImage = Res.drawable.kakao_guide2,
                )
            } else {
                ImportationGuideSection(
                    firstStep = stringResource(Res.string.importation_guide_naver_step1),
                    firstImageDescription =
                        stringResource(Res.string.importation_guide_naver_step1_image_description),
                    firstImage = Res.drawable.naver_guide1,
                    secondStep = stringResource(Res.string.importation_guide_naver_step2),
                    secondImageDescription =
                        stringResource(Res.string.importation_guide_naver_step2_image_description),
                    secondImage = Res.drawable.naver_guide2,
                )
            }
        }
    }
}

@Composable
private fun ImportationGuideSection(
    firstStep: String,
    firstImageDescription: String,
    firstImage: DrawableResource,
    secondStep: String,
    secondImageDescription: String,
    secondImage: DrawableResource,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 10.dp),
    ) {
        AppText(text = firstStep, style = AppTextStyle.T2, color = GrayColor.C400)
        Image(
            painter = painterResource(firstImage),
            contentDescription = firstImageDescription,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
        )
        AppText(text = secondStep, style = AppTextStyle.T2, color = GrayColor.C400)
        Image(
            painter = painterResource(secondImage),
            contentDescription = secondImageDescription,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun ImportationGuideRoutePreview() {
    RamapTheme {
        ImportationGuideContent(
            selectedTabIndex = 0,
            onTabSelected = {},
            onBack = {},
        )
    }
}
