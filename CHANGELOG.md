# Changelog

Todos los cambios relevantes de este proyecto se documentan en este archivo.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [Versionado Semántico](https://semver.org/lang/es/).

## [1.1.0] - Sin publicar

### Added
- Tabla `turnos` como entidad: cada registro pertenece a un turno (FK con borrado en cascada). El turno abierto se recupera al reabrir la app.
- Salida con **Deshacer** en portería y bicicletero.
- Autocompletado por RUT en portería y búsqueda por RUT o nombre en el bicicletero.
- Acceso a **Directorio de personas** desde el panel administrador (la pantalla existía pero no tenía punto de entrada).
- Reporte PDF guardado automáticamente al cerrar cualquier turno.
- Suite de pruebas: dominio, ViewModels, DAOs y migración con Robolectric, y verificación del grafo Koin.
- Dependabot, validación del Gradle wrapper y reportes de test y lint como artefactos en CI.

### Changed
- Arquitectura: contratos de repositorio en `domain`, casos de uso por funcionalidad, ViewModels con `StateFlow` y eventos de una sola vez, y Activities sin acceso directo a repositorios.
- Tiempos en epoch millis con `java.time`, en lugar de textos `HH:mm` con `SimpleDateFormat`.
- `tipoPersona`, tipo de turno y punto de control se guardan como códigos estables; las etiquetas visibles salen de `strings.xml`.
- Generación de PDF fuera del hilo principal y verificación del PIN fuera del hilo principal.
- Migración de base de datos v17 → v18 con conversión de datos heredados.

### Fixed
- Registrar un RUT nuevo con una patente ya conocida sobrescribía a la persona existente en el directorio.
- RUT normalizado de tres formas distintas, lo que hacía fallar búsquedas por RUT.
- Cierre de la app (`NumberFormatException`) al validar un RUT con `K` en el cuerpo.
- Preguntas del examen OS-10 duplicadas en la primera ejecución (carrera entre callbacks de Room).
- Turnos nocturnos mostraban mal la hora de inicio y fin, y las estadísticas diarias mezclaban formatos de fecha.
- "Borrar registros operativos" no borraba los del bicicletero.
- El listado de reportes siempre mostraba "Bicicletero 1", y el reporte de portería mostraba el id técnico en lugar del nombre del punto.
- Doble guardado del resultado del examen si el temporizador y el botón coincidían.

### Security
- `FileProvider` restringido a `files/reports/`.
- Reglas de respaldo y transferencia entre dispositivos amplían la exclusión a `reports/` y a los archivos `-wal`/`-shm` de la base.
- Eliminado todo `Log` de datos operativos.

### Removed
- Pantallas y clases inalcanzables (`AdminDashboardActivity`, `DirectoryActivity`, `HistoryActivity`, `SyncManager`, layouts sin uso).
- Dependencias sin uso: WorkManager, LiveData y las entradas de Apache POI del catálogo.

## [1.0.0] - Versión inicial pública

### Added
- Gestión operativa de accesos vehiculares y bicicletero con arquitectura Clean + MVVM + Koin + Room (v17).
- PIN administrativo con hashing `PBKDF2WithHmacSHA256` + salt en `EncryptedSharedPreferences` y bloqueo por intentos fallidos.
- Pruebas unitarias para `PinSecurityHelper`, `RutUtils` y `ValidateRegistroUseCase`.
- Documentación técnica, `SECURITY.md`, `CONTRIBUTING.md` y CI en GitHub Actions.

### Security
- Dataset inicial 100 % sintético, respaldo automático deshabilitado y minificación en release.
