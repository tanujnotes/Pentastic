package app.pentastic.di

import app.pentastic.db.DatabaseFactory
import app.pentastic.db.createDataStore
import app.pentastic.notification.ReminderScheduler
import app.pentastic.notification.ReminderSchedulerFactory
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single { createDataStore(androidApplication()) }
        single { DatabaseFactory(androidApplication()) }
        single { ReminderSchedulerFactory(androidApplication(), get()) }
        single<ReminderScheduler> { get<ReminderSchedulerFactory>().create() }
    }
