package app.pentastic.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.pentastic.data.DataStoreRepository
import app.pentastic.data.MyRepository
import app.pentastic.db.DatabaseFactory
import app.pentastic.db.PentasticDatabase
import app.pentastic.ui.viewmodel.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    single {
        get<DatabaseFactory>().create()
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single { get<PentasticDatabase>().noteDao }
    single { get<PentasticDatabase>().pageDao }
    single<MyRepository> { MyRepository(get(), get()) }
    single<DataStoreRepository> { DataStoreRepository(get()) }

    viewModelOf(::MainViewModel)
}