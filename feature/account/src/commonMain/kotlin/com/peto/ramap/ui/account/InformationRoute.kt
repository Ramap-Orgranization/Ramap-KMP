package com.peto.ramap.ui.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.SettingsPage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.platform.AppVersionProvider
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.app_version_label
import ramap.shared.generated.resources.privacy_policy_menu
import ramap.shared.generated.resources.settings_information_menu

private const val PRIVACY_POLICY_URL = "https://ramap-orgranization.github.io/Ramap-KMP/"

@Composable
fun InformationRoute(
    onBack: () -> Unit,
    versionProvider: AppVersionProvider = koinInject(),
) {
    SettingsPage(Res.string.settings_information_menu, onBack) {
        SectionCard {
            AppText(
                text = stringResource(Res.string.privacy_policy_menu),
                style = AppTextStyle.B1,
                color = if (PRIVACY_POLICY_URL.isBlank()) GrayColor.C300 else GrayColor.C500,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .noRippleClickable(
                            enabled = PRIVACY_POLICY_URL.isNotBlank(),
                            onClick = { ExternalUriOpener.open(PRIVACY_POLICY_URL) },
                        ).padding(horizontal = 20.dp, vertical = 16.dp),
            )

            HorizontalDivider(thickness = 1.dp, color = GrayColor.C200)
            AppText(
                text = stringResource(Res.string.app_version_label, versionProvider.versionName),
                style = AppTextStyle.B1,
                color = GrayColor.C500,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}
