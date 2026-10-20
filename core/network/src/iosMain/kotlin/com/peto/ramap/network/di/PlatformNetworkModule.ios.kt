package com.peto.ramap.network.di

import com.peto.ramap.network.auth.IosKeychainSessionStorage
import com.peto.ramap.network.auth.IosLegacySessionStorage
import com.peto.ramap.network.auth.MigratingSessionManager
import com.peto.ramap.network.auth.SessionStorageKeys
import com.peto.ramap.network.config.RamapSecrets
import io.github.jan.supabase.auth.SessionManager
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

internal actual val platformNetworkModule: Module =
    module {
        single<SessionManager> {
            val keys = SessionStorageKeys(RamapSecrets.supabaseUrl)
            MigratingSessionManager(
                secureStorage = IosKeychainSessionStorage(account = keys.session),
                legacyStorage = IosLegacySessionStorage(NSUserDefaults.standardUserDefaults, keys),
            )
        }
    }
