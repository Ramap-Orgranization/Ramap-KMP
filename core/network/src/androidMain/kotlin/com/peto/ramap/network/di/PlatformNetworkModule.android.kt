package com.peto.ramap.network.di

import android.content.Context
import com.peto.ramap.network.auth.AndroidKeystoreKeyProvider
import com.peto.ramap.network.auth.AndroidSessionCipher
import com.peto.ramap.network.auth.AndroidSessionStorage
import com.peto.ramap.network.auth.MigratingSessionManager
import com.peto.ramap.network.auth.SessionStorageKeys
import com.peto.ramap.network.config.RamapSecrets
import io.github.jan.supabase.auth.SessionManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

internal actual val platformNetworkModule: Module =
    module {
        single<SessionManager> {
            val context = androidContext()
            val keys = SessionStorageKeys(RamapSecrets.supabaseUrl)
            MigratingSessionManager(
                storage =
                    AndroidSessionStorage(
                        file = File(context.noBackupFilesDir, "auth/${keys.session}.preferences_pb"),
                        cipher = AndroidSessionCipher(AndroidKeystoreKeyProvider("ramap-${keys.session}")),
                        legacyPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE),
                        keys = keys,
                    ),
            )
        }
    }
