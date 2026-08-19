package com.peto.ramap.network.execute

import co.touchlab.kermit.Logger
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

@PublishedApi
internal val networkLogger = Logger.withTag("RamapNetwork")

suspend inline fun <T> invokeRequest(crossinline call: suspend () -> T): RamapResult<T> =
    try {
        RamapResult.Success(call())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: RestException) {
        networkLogger.d(exception) {
            "supabase response exception status=${exception.response.status.value}, message=${exception.message}"
        }
        RamapResult.Error(RamapError.Http(exception.response.status.value, exception))
    } catch (exception: ResponseException) {
        networkLogger.d(exception) {
            "response exception status=${exception.response.status.value}, message=${exception.message}"
        }
        RamapResult.Error(RamapError.Http(exception.response.status.value, exception))
    } catch (exception: HttpRequestTimeoutException) {
        networkLogger.d(exception) { "timeout message=${exception.message}" }
        RamapResult.Error(RamapError.Timeout(exception))
    } catch (exception: IOException) {
        networkLogger.d(exception) {
            "io exception=${exception::class.simpleName}, message=${exception.message}"
        }
        RamapResult.Error(RamapError.Network(exception))
    } catch (exception: SerializationException) {
        networkLogger.d(exception) { "serialization exception message=${exception.message}" }
        RamapResult.Error(RamapError.Serialization(exception))
    } catch (exception: Throwable) {
        networkLogger.d(exception) {
            "unknown exception=${exception::class.simpleName}, message=${exception.message}"
        }
        RamapResult.Error(RamapError.Unknown(exception))
    }
