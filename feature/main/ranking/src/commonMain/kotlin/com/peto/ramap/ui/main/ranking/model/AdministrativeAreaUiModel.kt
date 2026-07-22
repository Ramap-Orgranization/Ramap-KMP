package com.peto.ramap.ui.main.ranking.model

import com.peto.ramap.domain.model.shop.AdministrativeArea
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ranking_area_busan
import ramap.shared.generated.resources.ranking_area_busan_short
import ramap.shared.generated.resources.ranking_area_chungbuk
import ramap.shared.generated.resources.ranking_area_chungbuk_short
import ramap.shared.generated.resources.ranking_area_chungnam
import ramap.shared.generated.resources.ranking_area_chungnam_short
import ramap.shared.generated.resources.ranking_area_daegu
import ramap.shared.generated.resources.ranking_area_daegu_short
import ramap.shared.generated.resources.ranking_area_daejeon
import ramap.shared.generated.resources.ranking_area_daejeon_short
import ramap.shared.generated.resources.ranking_area_gangwon
import ramap.shared.generated.resources.ranking_area_gangwon_short
import ramap.shared.generated.resources.ranking_area_gwangju
import ramap.shared.generated.resources.ranking_area_gwangju_short
import ramap.shared.generated.resources.ranking_area_gyeongbuk
import ramap.shared.generated.resources.ranking_area_gyeongbuk_short
import ramap.shared.generated.resources.ranking_area_gyeonggi
import ramap.shared.generated.resources.ranking_area_gyeonggi_short
import ramap.shared.generated.resources.ranking_area_gyeongnam
import ramap.shared.generated.resources.ranking_area_gyeongnam_short
import ramap.shared.generated.resources.ranking_area_incheon
import ramap.shared.generated.resources.ranking_area_incheon_short
import ramap.shared.generated.resources.ranking_area_jeju
import ramap.shared.generated.resources.ranking_area_jeju_short
import ramap.shared.generated.resources.ranking_area_jeonbuk
import ramap.shared.generated.resources.ranking_area_jeonbuk_short
import ramap.shared.generated.resources.ranking_area_jeonnam
import ramap.shared.generated.resources.ranking_area_jeonnam_short
import ramap.shared.generated.resources.ranking_area_sejong
import ramap.shared.generated.resources.ranking_area_sejong_short
import ramap.shared.generated.resources.ranking_area_seoul
import ramap.shared.generated.resources.ranking_area_seoul_short
import ramap.shared.generated.resources.ranking_area_ulsan
import ramap.shared.generated.resources.ranking_area_ulsan_short

data class AdministrativeAreaUiModel(
    val area: AdministrativeArea,
    val shortNameResource: StringResource,
    val officialNameResource: StringResource,
) {
    companion object {
        val entries =
            listOf(
                AdministrativeAreaUiModel(
                    AdministrativeArea.SEOUL,
                    Res.string.ranking_area_seoul_short,
                    Res.string.ranking_area_seoul,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.BUSAN,
                    Res.string.ranking_area_busan_short,
                    Res.string.ranking_area_busan,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.DAEGU,
                    Res.string.ranking_area_daegu_short,
                    Res.string.ranking_area_daegu,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.INCHEON,
                    Res.string.ranking_area_incheon_short,
                    Res.string.ranking_area_incheon,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.GWANGJU,
                    Res.string.ranking_area_gwangju_short,
                    Res.string.ranking_area_gwangju,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.DAEJEON,
                    Res.string.ranking_area_daejeon_short,
                    Res.string.ranking_area_daejeon,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.ULSAN,
                    Res.string.ranking_area_ulsan_short,
                    Res.string.ranking_area_ulsan,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.SEJONG,
                    Res.string.ranking_area_sejong_short,
                    Res.string.ranking_area_sejong,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.GYEONGGI,
                    Res.string.ranking_area_gyeonggi_short,
                    Res.string.ranking_area_gyeonggi,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.CHUNGBUK,
                    Res.string.ranking_area_chungbuk_short,
                    Res.string.ranking_area_chungbuk,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.CHUNGNAM,
                    Res.string.ranking_area_chungnam_short,
                    Res.string.ranking_area_chungnam,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.JEONNAM,
                    Res.string.ranking_area_jeonnam_short,
                    Res.string.ranking_area_jeonnam,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.GYEONGBUK,
                    Res.string.ranking_area_gyeongbuk_short,
                    Res.string.ranking_area_gyeongbuk,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.GYEONGNAM,
                    Res.string.ranking_area_gyeongnam_short,
                    Res.string.ranking_area_gyeongnam,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.GANGWON,
                    Res.string.ranking_area_gangwon_short,
                    Res.string.ranking_area_gangwon,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.JEONBUK,
                    Res.string.ranking_area_jeonbuk_short,
                    Res.string.ranking_area_jeonbuk,
                ),
                AdministrativeAreaUiModel(
                    AdministrativeArea.JEJU,
                    Res.string.ranking_area_jeju_short,
                    Res.string.ranking_area_jeju,
                ),
            )

        fun from(area: AdministrativeArea): AdministrativeAreaUiModel = entries.single { it.area == area }
    }
}
