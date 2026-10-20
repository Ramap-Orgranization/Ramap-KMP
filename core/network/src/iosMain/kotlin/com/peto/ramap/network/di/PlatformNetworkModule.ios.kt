package com.peto.ramap.network.di

import com.peto.ramap.network.auth.IosKeychainSessionStorage
import com.peto.ramap.network.auth.MigratingSessionManager
import com.peto.ramap.network.auth.SessionStorageKeys
import com.peto.ramap.network.config.RamapSecrets
import io.github.jan.supabase.auth.SessionManager
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformNetworkModule: Module =
    module {
        single<SessionManager> {
            val keys = SessionStorageKeys(RamapSecrets.supabaseUrl)
            MigratingSessionManager(
                storage = IosKeychainSessionStorage(account = keys.session),
            )
        }
    }
