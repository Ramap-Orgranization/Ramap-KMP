package com.peto.ramap.debug.admin.data.datasource

import com.peto.ramap.debug.admin.data.model.request.EventStatusRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions

internal class RemoteAdminAccessDataSource(
    private val client: SupabaseClient,
) : AdminAccessDataSource {
    override suspend fun hasAccess(): Boolean =
        try {
            val response = client.functions.invoke(ADMIN_EVENT_STATUS_FUNCTION, EventStatusRequest(action = "list"))
            when (response.status.value) {
                in 200..299 -> true
                401, 403 -> false
                else -> error("Administrator access check failed")
            }
        } catch (exception: RestException) {
            if (exception.statusCode == 401 || exception.statusCode == 403) false else throw exception
        }

    private companion object {
        const val ADMIN_EVENT_STATUS_FUNCTION = "admin-event-status"
    }
}
