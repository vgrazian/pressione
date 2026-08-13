package com.pressione.iperteso

import android.app.Application
import com.pressione.iperteso.data.local.AppDatabase
import com.pressione.iperteso.data.sync.SyncWorker
import com.pressione.iperteso.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class IperTesoApplication : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Room database
        database = AppDatabase.getInstance(this)

        // Initialize Koin
        startKoin {
            androidContext(this@IperTesoApplication)
            modules(appModule)
        }

        // Schedule periodic sync (15 min intervals, requires network)
        SyncWorker.schedule(this)
    }

    companion object {
        lateinit var instance: IperTesoApplication
            private set
    }
}
