package com.peto.ramap.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.tab.AppChipTabs
import com.peto.ramap.navigation.NewsTab
import com.peto.ramap.theme.CommonColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.news_tab_event
import ramap.shared.generated.resources.news_tab_operating_notice

@Composable
internal fun NewsScreen(
    selectedTab: NewsTab,
    onTabSelected: (NewsTab) -> Unit,
    eventsContent: @Composable () -> Unit,
    operatingNoticesContent: @Composable () -> Unit,
) {
    val tabStateHolder = rememberSaveableStateHolder()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White)
                .statusBarsPadding(),
    ) {
        AppChipTabs(
            labels =
                NewsTab.entries.map { tab ->
                    stringResource(
                        when (tab) {
                            NewsTab.EVENTS -> Res.string.news_tab_event
                            NewsTab.OPERATING_NOTICES -> Res.string.news_tab_operating_notice
                        },
                    )
                },
            selectedIndex = selectedTab.ordinal,
            onSelect = { index -> onTabSelected(NewsTab.entries[index]) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        tabStateHolder.SaveableStateProvider(selectedTab.name) {
            when (selectedTab) {
                NewsTab.EVENTS -> eventsContent()
                NewsTab.OPERATING_NOTICES -> operatingNoticesContent()
            }
        }
    }
}
