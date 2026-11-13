package com.dur1al.controlate.porteria.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager
import com.dur1al.controlate.porteria.data.local.security.InMemoryKeyValueStore
import com.dur1al.controlate.porteria.testing.MainDispatcherRule
import com.dur1al.controlate.porteria.ui.admin.AdminViewModel
import com.dur1al.controlate.porteria.ui.admin.ControlPointsViewModel
import com.dur1al.controlate.porteria.ui.admin.ShiftReportsViewModel
import com.dur1al.controlate.porteria.ui.bicicletero.BicicleteroViewModel
import com.dur1al.controlate.porteria.ui.db.DirectoryViewModel
import com.dur1al.controlate.porteria.ui.login.LoginViewModel
import com.dur1al.controlate.porteria.ui.porteria.PorteriaViewModel
import com.dur1al.controlate.porteria.ui.settings.SettingsViewModel
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Verifica que el grafo de Koin resuelve cada ViewModel. Koin valida dependencias en tiempo
 * de ejecución, así que sin este test un error de cableado solo aparecería al abrir la pantalla.
 */
@RunWith(AndroidJUnit4::class)
class KoinGraphTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun every_view_model_resolves() {
        val koin = koinApplication {
            androidContext(ApplicationProvider.getApplicationContext<Context>())
            // EncryptedSharedPreferences requiere Android Keystore, ausente en Robolectric.
            modules(appModules + module { single { AdminPinManager(InMemoryKeyValueStore(), get()) } })
        }.koin

        assertNotNull(koin.get<LoginViewModel>())
        assertNotNull(koin.get<PorteriaViewModel>())
        assertNotNull(koin.get<BicicleteroViewModel>())
        assertNotNull(koin.get<AdminViewModel>())
        assertNotNull(koin.get<ShiftReportsViewModel>())
        assertNotNull(koin.get<DirectoryViewModel>())
        assertNotNull(koin.get<SettingsViewModel>())
        assertNotNull(koin.get<ControlPointsViewModel>())
        koin.close()
    }
}
