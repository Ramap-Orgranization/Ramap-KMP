package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_profile_private
import ramap.shared.generated.resources.ic_profile_public
import ramap.shared.generated.resources.ic_setting
import ramap.shared.generated.resources.profile_visibility
import ramap.shared.generated.resources.settings_title

@Composable
internal fun MyTabTopBar(
    isLoggedIn: Boolean,
    isPublic: Boolean,
    isProfileLoaded: Boolean,
    onVisibilityClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(end = 14.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        if (isLoggedIn) {
            IconButton(
                onClick = onVisibilityClick,
                enabled = isProfileLoaded,
            ) {
                Image(
                    painter =
                        painterResource(
                            if (isPublic) Res.drawable.ic_profile_public else Res.drawable.ic_profile_private,
                        ),
                    contentDescription = stringResource(Res.string.profile_visibility),
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                painter = painterResource(Res.drawable.ic_setting),
                contentDescription = stringResource(Res.string.settings_title),
                tint = GrayColor.C500,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyTabTopBarPreview() {
    RamapTheme {
        MyTabTopBar(
            isLoggedIn = true,
            isPublic = true,
            isProfileLoaded = true,
            onVisibilityClick = {},
            onSettingsClick = {},
        )
    }
}
