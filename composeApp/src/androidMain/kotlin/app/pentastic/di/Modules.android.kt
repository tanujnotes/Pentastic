package app.pentastic.di

import app.pentastic.db.DatabaseFactory
import app.pentastic.db.createDataStore
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single { createDataStore(androidApplication()) }
        single { DatabaseFactory(androidApplication()) }
    }
