package com.peto.ramap.ui.main.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.login.rememberLoginGuideRequest
import com.peto.ramap.ui.main.feed.component.FeedDialogs
import com.peto.ramap.ui.main.feed.contract.FeedIntent
import com.peto.ramap.ui.main.feed.contract.FeedSideEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeedRoute(
    onOpenProfile: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
    onLoginTypeSelected: (LoginType) -> Unit,
    onFindVisitedShop: () -> Unit,
    shopDetailContent: @Composable (String, () -> Unit) -> Unit,
    viewModel: FeedViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(FeedIntent.Resume)
    }
    var detailShopId by rememberSaveable { mutableStateOf<String?>(null) }
    val requestLogin = rememberLoginGuideRequest(onLoginTypeSelected = onLoginTypeSelected, ownerKey = viewModel)
    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            is FeedSideEffect.LoginRequired -> requestLogin(effect.description)
            is FeedSideEffect.ShowToast -> toastManager.show(effect.data)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        FeedContent(
            state = state,
            listState = listState,
            onIntent = viewModel::dispatch,
            onOpenProfile = onOpenProfile,
            onOpenShop = { detailShopId = it },
            onEditReview = onEditReview,
            onFindVisitedShop = onFindVisitedShop,
        )
        FeedDialogs(state = state, onIntent = viewModel::dispatch)
        detailShopId?.let { shopId ->
            shopDetailContent(shopId) {
                detailShopId = null
                viewModel.dispatch(FeedIntent.Resume)
            }
        }
    }
}
