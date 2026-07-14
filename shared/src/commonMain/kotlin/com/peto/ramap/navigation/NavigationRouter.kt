package com.peto.ramap.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.ui.main.component.NavigationBar
import com.peto.ramap.ui.main.map.model.TabStatus

@Composable
fun NavigationRouter(
    currentRoute: ScreenRoutes,
    selectedTab: TabStatus,
    onTabSelected: (TabStatus) -> Unit,
    mapContent: @Composable () -> Unit,
    myContent: @Composable () -> Unit,
    hiddenContent: @Composable () -> Unit,
    eventContent: @Composable () -> Unit,
) {
    if (currentRoute == ScreenRoutes.HiddenShopListRoutes) {
        Box(modifier = Modifier.fillMaxSize().background(CommonColor.White)) {
            hiddenContent()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(CommonColor.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .alpha(
                                if (currentRoute == ScreenRoutes.TabRoutes ||
                                    currentRoute is ScreenRoutes.EventDetailRoutes
                                ) {
                                    1f
                                } else {
                                    0f
                                },
                            ),
                ) {
                    mapContent()
                }

                if (currentRoute == ScreenRoutes.MyTabRoutes) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        myContent()
                    }
                }
            }

            NavigationBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        if (currentRoute is ScreenRoutes.EventDetailRoutes) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(CommonColor.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
            ) {
                eventContent()
            }
        }
    }
}
