package com.peto.ramap.ui.resource.area

import androidx.compose.runtime.Immutable
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

@Immutable
data class AdministrativeAreaUiModel(
    val area: AdministrativeArea,
    val shortName: StringResource,
    val officialName: StringResource,
) {
    companion object {
        val entries: List<AdministrativeAreaUiModel> =
            AdministrativeArea.entries.map(::map)

        fun map(area: AdministrativeArea): AdministrativeAreaUiModel =
            when (area) {
                AdministrativeArea.SEOUL ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_seoul_short,
                        Res.string.ranking_area_seoul,
                    )

                AdministrativeArea.BUSAN ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_busan_short,
                        Res.string.ranking_area_busan,
                    )

                AdministrativeArea.DAEGU ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_daegu_short,
                        Res.string.ranking_area_daegu,
                    )

                AdministrativeArea.INCHEON ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_incheon_short,
                        Res.string.ranking_area_incheon,
                    )

                AdministrativeArea.GWANGJU ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_gwangju_short,
                        Res.string.ranking_area_gwangju,
                    )

                AdministrativeArea.DAEJEON ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_daejeon_short,
                        Res.string.ranking_area_daejeon,
                    )

                AdministrativeArea.ULSAN ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_ulsan_short,
                        Res.string.ranking_area_ulsan,
                    )

                AdministrativeArea.SEJONG ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_sejong_short,
                        Res.string.ranking_area_sejong,
                    )

                AdministrativeArea.GYEONGGI ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_gyeonggi_short,
                        Res.string.ranking_area_gyeonggi,
                    )

                AdministrativeArea.CHUNGBUK ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_chungbuk_short,
                        Res.string.ranking_area_chungbuk,
                    )

                AdministrativeArea.CHUNGNAM ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_chungnam_short,
                        Res.string.ranking_area_chungnam,
                    )

                AdministrativeArea.JEONNAM ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_jeonnam_short,
                        Res.string.ranking_area_jeonnam,
                    )

                AdministrativeArea.GYEONGBUK ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_gyeongbuk_short,
                        Res.string.ranking_area_gyeongbuk,
                    )

                AdministrativeArea.GYEONGNAM ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_gyeongnam_short,
                        Res.string.ranking_area_gyeongnam,
                    )

                AdministrativeArea.GANGWON ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_gangwon_short,
                        Res.string.ranking_area_gangwon,
                    )

                AdministrativeArea.JEONBUK ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_jeonbuk_short,
                        Res.string.ranking_area_jeonbuk,
                    )

                AdministrativeArea.JEJU ->
                    AdministrativeAreaUiModel(
                        area,
                        Res.string.ranking_area_jeju_short,
                        Res.string.ranking_area_jeju,
                    )
            }
    }
}
