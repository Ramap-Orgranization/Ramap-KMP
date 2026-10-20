package com.peto.ramap.network.di

import android.content.Context
import com.peto.ramap.network.auth.AndroidKeystoreKeyProvider
import com.peto.ramap.network.auth.AndroidLegacySessionStorage
import com.peto.ramap.network.auth.AndroidSecureSessionStorage
import com.peto.ramap.network.auth.AndroidSessionCipher
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
                secureStorage =
                    AndroidSecureSessionStorage(
                        file = File(context.noBackupFilesDir, "auth/${keys.session}.enc"),
                        cipher = AndroidSessionCipher(AndroidKeystoreKeyProvider("ramap-${keys.session}")),
                    ),
                legacyStorage =
                    AndroidLegacySessionStorage(
                        preferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE),
                        keys = keys,
                    ),
            )
        }
    }
