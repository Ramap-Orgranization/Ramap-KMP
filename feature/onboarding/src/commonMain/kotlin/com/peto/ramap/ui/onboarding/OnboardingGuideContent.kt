package com.peto.ramap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.onboarding.contract.ShopSelectionIntent
import com.peto.ramap.ui.onboarding.contract.ShopSelectionUiState
import com.peto.ramap.ui.onboarding.model.OnboardingGuidePage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.onboarding_guide_progress

@Composable
internal fun OnboardingGuideContent(
    uiState: ShopSelectionUiState,
    onIntent: (ShopSelectionIntent) -> Unit,
) {
    val guidePages = OnboardingGuidePage.entries
    val pagerState = rememberPagerState(pageCount = { guidePages.size })
    var showShopSelection by rememberSaveable { mutableStateOf(false) }
    val screenStateHolder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val canNavigate = !uiState.isCompleting && !uiState.isCompleted

    NavigationBackHandler(
        state = rememberNavigationEventState<NavigationEventInfo>(NavigationEventInfo.None),
        isBackEnabled = showShopSelection || pagerState.currentPage > 0,
        onBackCompleted = {
            if (!canNavigate) return@NavigationBackHandler
            if (showShopSelection) {
                pagerState.requestScrollToPage(guidePages.lastIndex)
                showShopSelection = false
            } else if (!pagerState.isScrollInProgress) {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            }
        },
    )
    OnboardingGuideLayout(page = if (showShopSelection) null else guidePages.getOrNull(pagerState.settledPage)) {
        screenStateHolder.SaveableStateProvider(showShopSelection) {
            if (showShopSelection) {
                ShopSelectionContent(uiState = uiState, onIntent = onIntent)
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    OnboardingGuidePageContent(
                        page = guidePages[page],
                        onNext = {
                            if (page == guidePages.lastIndex) {
                                showShopSelection = true
                            } else {
                                scope.launch { pagerState.animateScrollToPage(page + 1) }
                            }
                        },
                        onSkip = { showShopSelection = true },
                    )
                }
            }
        }
    }
}

@Composable
internal fun OnboardingGuideLayout(
    page: OnboardingGuidePage?,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(CommonColor.White).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (page != null) OnboardingGuideIndicator(page = page)
        content()
    }
}

@Composable
private fun OnboardingGuideIndicator(page: OnboardingGuidePage) {
    val pages = OnboardingGuidePage.entries
    val progressDescription = stringResource(Res.string.onboarding_guide_progress, page.ordinal + 1, pages.size)
    Row(
        modifier =
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 20.dp)
                .semantics {
                    contentDescription = progressDescription
                    progressBarRangeInfo = ProgressBarRangeInfo((page.ordinal + 1).toFloat(), 0f..pages.size.toFloat())
                },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        pages.forEach { item ->
            Spacer(
                modifier =
                    Modifier.weight(1f).height(4.dp).background(
                        color = if (item == page) ChromaticColor.Orange400 else GrayColor.C100,
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}
