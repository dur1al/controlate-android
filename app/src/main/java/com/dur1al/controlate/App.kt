package com.dur1al.controlate

import android.app.Application
import com.dur1al.controlate.porteria.data.local.database.DatabaseSeeder
import com.dur1al.controlate.porteria.di.appModules
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    /**
     * Ámbito de la aplicación para trabajo que no pertenece a ninguna pantalla (seed inicial).
     * Un fallo del seed no debe cerrar la app: los datos demo son opcionales.
     */
    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> /* seed opcional: se reintenta en el próximo arranque */ }
    )

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModules)
        }
        val seeder: DatabaseSeeder = get()
        appScope.launch { seeder.seedIfNeeded() }
    }
}
