package com.peto.ramap.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.peto.ramap.navigation.NewsTab
import com.peto.ramap.theme.SurfaceColor

@Composable
internal fun NewsScreen(
    selectedTab: NewsTab,
    eventsContent: @Composable () -> Unit,
    operatingNoticesContent: @Composable () -> Unit,
) {
    val tabStateHolder = rememberSaveableStateHolder()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(SurfaceColor.Background)
                .statusBarsPadding(),
    ) {
        tabStateHolder.SaveableStateProvider(selectedTab.name) {
            when (selectedTab) {
                NewsTab.EVENTS -> eventsContent()
                NewsTab.OPERATING_NOTICES -> operatingNoticesContent()
            }
        }
    }
}
