package com.peto.ramap.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

val LocalAppTypography =
    staticCompositionLocalOf<AppTypography> {
        error("AppTypography is not provided")
    }

private val LightColorScheme =
    lightColorScheme(
        primary = GrayColor.C500,
        onPrimary = CommonColor.White,
        secondary = ChromaticColor.Green400,
        onSecondary = GrayColor.C500,
        background = CommonColor.White,
        onBackground = GrayColor.C500,
        surface = CommonColor.White,
        onSurface = GrayColor.C500,
        error = SystemColor.Warning,
    )

/**
 * Ramap 애플리케이션의 기본 테마 테마 컴포저블.
 * Material Design 3 [MaterialTheme]와 앱 전용 커스텀 디자인 시스템([LocalAppTypography])을 설정한다.
 *
 * 특히 [androidx.compose.ui.tooling.preview.Preview] 환경에서 [androidx.navigationevent.compose.NavigationBackHandler]가
 * [NavigationEventDispatcher] 부재로 인해 `IllegalStateException`을 발생시키는 것을 방지하기 위해,
 * Preview 모드인 경우 자동으로 모의(Mock) 디스패처를 주입한다.
 *
 * @param content 테마가 적용될 하위 컴포저블 콘텐츠.
 */
@Composable
fun RamapTheme(content: @Composable () -> Unit) {
    val typography = provideAppTypography()
    val isPreview = LocalInspectionMode.current
    val currentNavigationOwner = LocalNavigationEventDispatcherOwner.current

    val themeContent =
        @Composable {
            MaterialTheme(
                colorScheme = LightColorScheme,
                typography = typography.toMaterialTypography(),
                content = content,
            )
        }

    CompositionLocalProvider(
        LocalAppTypography provides typography,
    ) {
        if (isPreview && currentNavigationOwner == null) {
            // Preview 환경에서 NavigationBackHandler 사용 시 발생할 수 있는 IllegalStateException 방지
            val dispatcher = remember { NavigationEventDispatcher() }
            DisposableEffect(dispatcher) {
                onDispose { dispatcher.dispose() }
            }
            val owner =
                remember(dispatcher) {
                    object : NavigationEventDispatcherOwner {
                        override val navigationEventDispatcher = dispatcher
                    }
                }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                themeContent()
            }
        } else {
            themeContent()
        }
    }
}

private fun AppTypography.toMaterialTypography(): Typography =
    Typography(
        displayLarge = h1,
        displayMedium = h2,
        displaySmall = h3,
        headlineLarge = h1,
        headlineMedium = h2,
        headlineSmall = h3,
        titleLarge = t1,
        titleMedium = t2,
        titleSmall = t3,
        bodyLarge = b1,
        bodyMedium = b2,
        bodySmall = c1,
        labelLarge = b3,
        labelMedium = b4,
        labelSmall = c2,
    )
