package com.peto.ramap.network.execute

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

suspend inline fun <T> invokeRequest(crossinline call: suspend () -> T): RamapResult<T> =
    try {
        RamapResult.Success(call())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: ResponseException) {
        RamapResult.Error(RamapError.Http(exception.response.status.value, exception))
    } catch (exception: HttpRequestTimeoutException) {
        RamapResult.Error(RamapError.Timeout(exception))
    } catch (exception: IOException) {
        RamapResult.Error(RamapError.Network(exception))
    } catch (exception: SerializationException) {
        RamapResult.Error(RamapError.Serialization(exception))
    } catch (exception: Throwable) {
        RamapResult.Error(RamapError.Unknown(exception))
    }
