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

@Composable
fun NavigationRouter(
    currentRoute: ScreenRoutes,
    selectedTab: TabStatus,
    onTabSelected: (TabStatus) -> Unit,
    mapScreen: @Composable () -> Unit,
    eventListScreen: @Composable () -> Unit,
    myScreen: @Composable () -> Unit,
    hiddenScreen: @Composable () -> Unit,
    notificationSettingsScreen: @Composable () -> Unit,
    eventScreen: @Composable () -> Unit,
) {
    if (currentRoute == ScreenRoutes.HiddenShopListRoutes || currentRoute == ScreenRoutes.NotificationSettingsRoutes) {
        Box(modifier = Modifier.fillMaxSize().background(CommonColor.White)) {
            if (currentRoute == ScreenRoutes.HiddenShopListRoutes) {
                hiddenScreen()
            } else {
                notificationSettingsScreen()
            }
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
                    mapScreen()
                }

                if (currentRoute == ScreenRoutes.MyTabRoutes) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        myScreen()
                    }
                }

                if (
                    currentRoute == ScreenRoutes.EventTabRoutes ||
                    (
                        currentRoute is ScreenRoutes.EventDetailRoutes &&
                            selectedTab == TabStatus.EVENT
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        eventListScreen()
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
                eventScreen()
            }
        }
    }
}
