package app.pentastic.di

import app.pentastic.db.DatabaseFactory
import app.pentastic.db.createDataStore
import app.pentastic.notification.ReminderScheduler
import app.pentastic.notification.ReminderSchedulerFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single { createDataStore() }
        single { DatabaseFactory() }
        single { ReminderSchedulerFactory(get()) }
        single<ReminderScheduler> { get<ReminderSchedulerFactory>().create() }
    }