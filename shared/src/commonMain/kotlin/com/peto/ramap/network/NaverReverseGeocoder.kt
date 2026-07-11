package com.peto.ramap.network

import com.peto.ramap.domain.model.Location
import com.peto.ramap.shared.RamapConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NaverReverseGeocoder(private val client: HttpClient) {
    suspend fun address(location: Location): String? {
        val root = Json.parseToJsonElement(client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = "maps.apigw.ntruss.com"
                path("map-reversegeocode", "v2", "gc")
                parameters.append("coords", "${location.lng},${location.lat}")
                parameters.append("sourcecrs", "EPSG:4326")
                parameters.append("orders", "roadaddr,addr")
                parameters.append("output", "json")
            }
            header("x-ncp-apigw-api-key-id", RamapConfig.NAVER_MAP_NCP_KEY_ID)
            header("x-ncp-apigw-api-key", RamapConfig.NAVER_CLIENT_SECRET)
        }.bodyAsText()).jsonObject

        val result = root["results"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val region = result["region"]?.jsonObject ?: return null
        val land = result["land"]?.jsonObject
        val regions = (1..4).mapNotNull { region["area$it"]?.jsonObject?.get("name")?.jsonPrimitive?.content?.takeIf(String::isNotBlank) }
        val landName = land?.get("name")?.jsonPrimitive?.content?.takeIf(String::isNotBlank)
        val number = listOfNotNull(land?.get("number1")?.jsonPrimitive?.content, land?.get("number2")?.jsonPrimitive?.content?.takeIf(String::isNotBlank)).joinToString("-")
        return (regions + landName + number.takeIf(String::isNotBlank)).joinToString(" ").takeIf(String::isNotBlank)
    }
}
