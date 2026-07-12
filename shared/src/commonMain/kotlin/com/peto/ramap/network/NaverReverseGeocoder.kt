package com.peto.ramap.network

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.Location
import com.peto.ramap.network.execute.invokeRequest
import com.peto.ramap.shared.RamapConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NaverReverseGeocoder(
    private val client: HttpClient,
) {
    suspend fun address(location: Location): RamapResult<String?> = invokeRequest { parseAddress(request(location)) }

    private suspend fun request(location: Location): JsonObject =
        Json
            .parseToJsonElement(
                client
                    .get {
                        url {
                            protocol = URLProtocol.HTTPS
                            host = API_HOST
                            path(*API_PATH)
                            parameters.append(PARAM_COORDS, "${location.lng},${location.lat}")
                            parameters.append(PARAM_SOURCE_CRS, SOURCE_CRS)
                            parameters.append(PARAM_ORDERS, ORDERS)
                            parameters.append(PARAM_OUTPUT, OUTPUT_JSON)
                        }
                        header(HEADER_API_KEY_ID, RamapConfig.NAVER_MAP_NCP_KEY_ID)
                        header(HEADER_API_KEY, RamapConfig.NAVER_CLIENT_SECRET)
                    }.bodyAsText(),
            ).jsonObject

    private fun parseAddress(root: JsonObject): String? {
        val result = root[FIELD_RESULTS]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val region = result[FIELD_REGION]?.jsonObject ?: return null
        val land = result[FIELD_LAND]?.jsonObject
        return (region.names() + land?.text(FIELD_NAME) + land?.number())
            .joinToString(ADDRESS_SEPARATOR)
            .takeIf(String::isNotBlank)
    }

    private fun JsonObject.names(): List<String> =
        (1..REGION_AREA_COUNT).mapNotNull { this["$FIELD_AREA$it"]?.jsonObject?.text(FIELD_NAME) }

    private fun JsonObject.number(): String? =
        listOfNotNull(text(FIELD_NUMBER_1), text(FIELD_NUMBER_2))
            .joinToString(LAND_NUMBER_SEPARATOR)
            .takeIf(String::isNotBlank)

    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.content?.takeIf(String::isNotBlank)

    private companion object {
        const val API_HOST = "maps.apigw.ntruss.com"
        val API_PATH = arrayOf("map-reversegeocode", "v2", "gc")
        const val HEADER_API_KEY_ID = "x-ncp-apigw-api-key-id"
        const val HEADER_API_KEY = "x-ncp-apigw-api-key"
        const val PARAM_COORDS = "coords"
        const val PARAM_SOURCE_CRS = "sourcecrs"
        const val PARAM_ORDERS = "orders"
        const val PARAM_OUTPUT = "output"
        const val SOURCE_CRS = "EPSG:4326"
        const val ORDERS = "roadaddr,addr"
        const val OUTPUT_JSON = "json"
        const val FIELD_RESULTS = "results"
        const val FIELD_REGION = "region"
        const val FIELD_LAND = "land"
        const val FIELD_AREA = "area"
        const val FIELD_NAME = "name"
        const val FIELD_NUMBER_1 = "number1"
        const val FIELD_NUMBER_2 = "number2"
        const val REGION_AREA_COUNT = 4
        const val ADDRESS_SEPARATOR = " "
        const val LAND_NUMBER_SEPARATOR = "-"
    }
}
