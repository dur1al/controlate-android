package com.dur1al.controlate.porteria.di

import com.dur1al.controlate.porteria.data.local.database.AppDatabase
import com.dur1al.controlate.porteria.data.local.security.SecurityManager
import com.dur1al.controlate.porteria.data.local.session.SessionManager
import com.dur1al.controlate.porteria.data.repository.BicicletaRepository
import com.dur1al.controlate.porteria.data.repository.ExamenRepository
import com.dur1al.controlate.porteria.data.repository.PersonaRepository
import com.dur1al.controlate.porteria.data.repository.RegistroRepository
import com.dur1al.controlate.porteria.domain.usecase.CalcularResultadoExamenUseCase
import com.dur1al.controlate.porteria.domain.usecase.ExportShiftReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.GenerarExamenUseCase
import com.dur1al.controlate.porteria.domain.usecase.MarkExitUseCase
import com.dur1al.controlate.porteria.domain.usecase.SaveRegistroUseCase
import com.dur1al.controlate.porteria.domain.usecase.SearchPersonaUseCase
import com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase
import com.dur1al.controlate.porteria.ui.admin.AdminViewModel
import com.dur1al.controlate.porteria.ui.bicicletero.BicicleteroViewModel
import com.dur1al.controlate.porteria.ui.dashboard.DashboardViewModel
import com.dur1al.controlate.porteria.ui.examen.ExamenViewModel
import com.dur1al.controlate.porteria.utils.ReportManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Core (Database, Session, Managers)
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().registroDao() }
    single { get<AppDatabase>().personaDao() }
    single { get<AppDatabase>().bicicletaDao() }
    single { get<AppDatabase>().examenDao() }

    single { SessionManager(androidContext()) }
    single { ReportManager(androidContext()) }
    single { SecurityManager(androidContext()) }

    // Repositories
    single { RegistroRepository(get()) }
    single { PersonaRepository(get()) }
    single { BicicletaRepository(get()) }
    single { ExamenRepository(get()) }

    // Use Cases
    single { SaveRegistroUseCase(get(), get()) }
    single { MarkExitUseCase(get()) }
    single { SearchPersonaUseCase(get()) }
    single { ValidateRegistroUseCase() }
    single { ExportShiftReportUseCase(get(), get(), get()) }
    single { GenerarExamenUseCase(get()) }
    single { CalcularResultadoExamenUseCase(get()) }

    // ViewModels
    viewModel {
        DashboardViewModel(
            registroRepository = get(),
            personaRepository = get(),
            searchPersonaUseCase = get(),
            saveRegistroUseCase = get(),
            markExitUseCase = get()
        )
    }

    viewModel { BicicleteroViewModel(get()) }

    viewModel { AdminViewModel(get(), get()) }

    viewModel { ExamenViewModel(get(), get()) }
}
