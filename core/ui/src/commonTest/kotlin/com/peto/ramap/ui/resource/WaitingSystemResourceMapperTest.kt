package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.shop.WaitingProvider
import com.peto.ramap.domain.model.shop.WaitingSystem
import com.peto.ramap.ui.resource.category.label
import com.peto.ramap.ui.resource.wating.toUiModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.catchtable
import ramap.shared.generated.resources.shop_detail_waiting_catchtable
import ramap.shared.generated.resources.shop_detail_waiting_syrup_friends
import ramap.shared.generated.resources.shop_detail_waiting_tabling
import ramap.shared.generated.resources.syrup_friends
import ramap.shared.generated.resources.tabling
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WaitingSystemResourceMapperTest {
    @Test
    fun `지원하는 웨이팅 제공자의 표시 정보와 링크를 만든다`() {
        val providers =
            listOf(
                Triple(
                    WaitingProvider.CATCHTABLE,
                    Res.string.shop_detail_waiting_catchtable,
                    Res.drawable.catchtable,
                ),
                Triple(
                    WaitingProvider.TABLING,
                    Res.string.shop_detail_waiting_tabling,
                    Res.drawable.tabling,
                ),
                Triple(
                    WaitingProvider.SYRUP_FRIENDS,
                    Res.string.shop_detail_waiting_syrup_friends,
                    Res.drawable.syrup_friends,
                ),
            )

        providers.forEach { (provider, expectedLabel, expectedIcon) ->
            val resources = waitingSystem(provider = provider).toUiModel()
            assertEquals(expectedLabel, resources?.label)
            assertEquals(expectedIcon, resources?.icon)
            assertEquals(PROVIDER_URL, resources?.providerUrl)
        }
    }

    @Test
    fun `링크가 없거나 제공자를 지원하지 않으면 표시 정보를 만들지 않는다`() {
        assertNull(
            waitingSystem(
                provider = WaitingProvider.CATCHTABLE,
                providerUrl = null,
            ).toUiModel(),
        )
        assertNull(waitingSystem(WaitingProvider.UNKNOWN).toUiModel())
        assertNull(null.toUiModel())
    }

    private fun waitingSystem(
        provider: WaitingProvider,
        providerUrl: String? = PROVIDER_URL,
    ): WaitingSystem =
        WaitingSystem(
            id = "waiting-system-id",
            shopId = "shop-id",
            provider = provider,
            providerUrl = providerUrl,
        )

    private companion object {
        const val PROVIDER_URL = "https://example.com/waiting"
    }
}
