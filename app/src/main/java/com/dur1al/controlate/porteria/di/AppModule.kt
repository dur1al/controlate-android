package com.dur1al.controlate.porteria.di

import com.dur1al.controlate.porteria.data.local.database.AppDatabase
import com.dur1al.controlate.porteria.data.local.database.DatabaseSeeder
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager
import com.dur1al.controlate.porteria.data.local.security.EncryptedKeyValueStore
import com.dur1al.controlate.porteria.data.local.session.SessionManager
import com.dur1al.controlate.porteria.data.report.PdfReportWriter
import com.dur1al.controlate.porteria.data.report.ReportStore
import com.dur1al.controlate.porteria.data.repository.RoomBikeEntryRepository
import com.dur1al.controlate.porteria.data.repository.RoomExamRepository
import com.dur1al.controlate.porteria.data.repository.RoomPersonaRepository
import com.dur1al.controlate.porteria.data.repository.RoomShiftRepository
import com.dur1al.controlate.porteria.data.repository.RoomVehicleEntryRepository
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.report.ReportLabels
import com.dur1al.controlate.porteria.domain.repository.BikeEntryRepository
import com.dur1al.controlate.porteria.domain.repository.ExamRepository
import com.dur1al.controlate.porteria.domain.repository.PersonaRepository
import com.dur1al.controlate.porteria.domain.repository.SessionStore
import com.dur1al.controlate.porteria.domain.repository.ShiftRepository
import com.dur1al.controlate.porteria.domain.repository.VehicleEntryRepository
import com.dur1al.controlate.porteria.domain.time.Clock
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.CalcularResultadoExamenUseCase
import com.dur1al.controlate.porteria.domain.usecase.ClearOperationalDataUseCase
import com.dur1al.controlate.porteria.domain.usecase.EndShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.FindPersonaUseCase
import com.dur1al.controlate.porteria.domain.usecase.GenerarExamenUseCase
import com.dur1al.controlate.porteria.domain.usecase.GetActiveShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.GetDailyStatsUseCase
import com.dur1al.controlate.porteria.domain.usecase.ImportPersonasUseCase
import com.dur1al.controlate.porteria.domain.usecase.MarkExitUseCase
import com.dur1al.controlate.porteria.domain.usecase.RegisterBikeEntryUseCase
import com.dur1al.controlate.porteria.domain.usecase.RegisterVehicleEntryUseCase
import com.dur1al.controlate.porteria.domain.usecase.StartShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase
import com.dur1al.controlate.porteria.ui.admin.AdminViewModel
import com.dur1al.controlate.porteria.ui.admin.ShiftReportsViewModel
import com.dur1al.controlate.porteria.ui.bicicletero.BicicleteroViewModel
import com.dur1al.controlate.porteria.ui.common.AndroidReportLabels
import com.dur1al.controlate.porteria.ui.db.DirectoryViewModel
import com.dur1al.controlate.porteria.ui.examen.ExamenViewModel
import com.dur1al.controlate.porteria.ui.login.LoginViewModel
import com.dur1al.controlate.porteria.ui.porteria.PorteriaViewModel
import com.dur1al.controlate.porteria.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

/** Persistencia, seguridad y archivos. */
val dataModule = module {
    single<Clock> { Clock.SYSTEM }

    single { SessionManager(androidContext()) } bind SessionStore::class
    single { DatabaseSeeder(database = { get() }, resources = androidContext().resources) }
    single {
        val session: SessionManager = get()
        val seeder: DatabaseSeeder = get()
        AppDatabase.build(
            context = androidContext(),
            legacyActiveShift = session.consumeLegacyActiveShift(),
            onCreated = seeder::onDatabaseCreated
        )
    }
    single { get<AppDatabase>().turnoDao() }
    single { get<AppDatabase>().registroDao() }
    single { get<AppDatabase>().bicicletaDao() }
    single { get<AppDatabase>().personaDao() }
    single { get<AppDatabase>().examenDao() }

    singleOf(::RoomShiftRepository) bind ShiftRepository::class
    singleOf(::RoomVehicleEntryRepository) bind VehicleEntryRepository::class
    singleOf(::RoomBikeEntryRepository) bind BikeEntryRepository::class
    singleOf(::RoomPersonaRepository) bind PersonaRepository::class
    singleOf(::RoomExamRepository) bind ExamRepository::class

    single { AdminPinManager(EncryptedKeyValueStore(androidContext()), get()) }

    single { ReportStore(File(androidContext().filesDir, "reports")) }
    single<ReportExporter> { PdfReportWriter(androidContext(), get(), get()) }
    single<ReportLabels> { AndroidReportLabels(androidContext()) }
}

/** Casos de uso: sin estado, se crean bajo demanda. */
val domainModule = module {
    factoryOf(::ValidateRegistroUseCase)
    factory { StartShiftUseCase(get(), get(), get()) }
    factoryOf(::GetActiveShiftUseCase)
    factoryOf(::EndShiftUseCase)
    factoryOf(::RegisterVehicleEntryUseCase)
    factoryOf(::RegisterBikeEntryUseCase)
    factoryOf(::MarkExitUseCase)
    factoryOf(::FindPersonaUseCase)
    factory { GetDailyStatsUseCase(get(), get()) }
    factoryOf(::ClearOperationalDataUseCase)
    factoryOf(::ImportPersonasUseCase)
    factory { BuildReportUseCase(get(), get(), get(), get(), get(), get()) }
    factoryOf(::GenerarExamenUseCase)
    factoryOf(::CalcularResultadoExamenUseCase)
}

val presentationModule = module {
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModelOf(::PorteriaViewModel)
    viewModelOf(::BicicleteroViewModel)
    viewModelOf(::AdminViewModel)
    viewModelOf(::ShiftReportsViewModel)
    viewModelOf(::DirectoryViewModel)
    viewModel { SettingsViewModel(get()) }
    viewModel { params -> ExamenViewModel(get(), get(), guardiaNombre = params.get()) }
}

val appModules = listOf(dataModule, domainModule, presentationModule)
