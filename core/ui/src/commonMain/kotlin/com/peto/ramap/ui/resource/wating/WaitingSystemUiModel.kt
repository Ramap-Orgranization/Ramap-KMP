package com.peto.ramap.ui.resource.wating

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.shop.WaitingProvider
import com.peto.ramap.domain.model.shop.WaitingSystem
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.catchtable
import ramap.shared.generated.resources.shop_detail_waiting_catchtable
import ramap.shared.generated.resources.shop_detail_waiting_syrup_friends
import ramap.shared.generated.resources.shop_detail_waiting_tabling
import ramap.shared.generated.resources.syrup_friends
import ramap.shared.generated.resources.tabling

@Immutable
data class WaitingSystemUiModel(
    val label: StringResource,
    val icon: DrawableResource,
    val providerUrl: String,
)

fun WaitingSystem?.toUiModel(): WaitingSystemUiModel? {
    val waitingSystem = this ?: return null
    val providerUrl = waitingSystem.providerUrl ?: return null

    return when (waitingSystem.provider) {
        WaitingProvider.CATCHTABLE ->
            WaitingSystemUiModel(
                label = Res.string.shop_detail_waiting_catchtable,
                icon = Res.drawable.catchtable,
                providerUrl = providerUrl,
            )

        WaitingProvider.TABLING ->
            WaitingSystemUiModel(
                label = Res.string.shop_detail_waiting_tabling,
                icon = Res.drawable.tabling,
                providerUrl = providerUrl,
            )

        WaitingProvider.SYRUP_FRIENDS ->
            WaitingSystemUiModel(
                label = Res.string.shop_detail_waiting_syrup_friends,
                icon = Res.drawable.syrup_friends,
                providerUrl = providerUrl,
            )

        WaitingProvider.UNKNOWN -> null
    }
}
