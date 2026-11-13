# Changelog

Todos los cambios relevantes de este proyecto se documentan en este archivo.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [Versionado Semántico](https://semver.org/lang/es/).

## [1.1.0] - 2026-11-13

### Added
- Turnos como entidad propia: cada registro pertenece a un turno y el turno abierto se recupera al reabrir la app.
- Puntos de control configurables desde el panel administrador: alta, cambio de nombre y activación de porterías y bicicleteros. Un punto desactivado conserva su historial.
- Salida con **Deshacer** en portería y bicicletero.
- Autocompletado por RUT en portería y búsqueda por RUT o nombre en el bicicletero.
- Acceso al directorio de personas desde el panel administrador.
- Reporte PDF guardado automáticamente al cerrar cualquier turno.
- Validación al salir de cada campo, con las mismas reglas que al guardar.
- Suite de pruebas de dominio, ViewModels, DAOs y migraciones con Robolectric, y verificación del grafo de dependencias.
- Dependabot, validación del Gradle wrapper y reportes de test y lint como artefactos en CI.

### Changed
- Arquitectura: contratos de repositorio en `domain`, casos de uso por funcionalidad, ViewModels con `StateFlow` y eventos de una sola vez, y Activities sin acceso directo a repositorios.
- Tiempos en epoch millis con `java.time`.
- Tipo de persona, tipo de turno y punto de control se guardan como códigos estables; los textos visibles se resuelven en la capa de presentación.
- Generación de PDF y verificación del PIN fuera del hilo principal.
- Las porterías de fábrica se muestran como Portería 1, 2 y 3, sin cambiar sus identificadores internos.
- Botones con contraste WCAG AA y estado deshabilitado distinguible, también sobre colores dinámicos.
- Los avisos de los formularios se muestran en línea y las notificaciones de pantalla no cubren las acciones principales.
- Formateo del RUT mientras se escribe y validación estricta de su formato.
- Validación de nombres más estricta y edición del directorio con errores por campo.
- Información de contacto actualizada.

### Fixed
- Registrar un RUT nuevo con una patente ya conocida sobrescribía a la persona existente en el directorio.
- El RUT se normalizaba de formas distintas según la pantalla, lo que hacía fallar las búsquedas.
- La app se cerraba al validar ciertos RUT mal formados.
- Turnos nocturnos con horas de inicio y fin incorrectas, y estadísticas diarias con formatos de fecha mezclados.
- "Borrar registros operativos" no incluía los del bicicletero.
- El listado y los reportes mostraban un punto de control incorrecto o su id técnico.
- Un doble toque en "Registrar" podía duplicar un ingreso.
- Se permitía registrar el ingreso de una patente o RUT que ya estaba dentro en el turno.
- Un fallo al guardar un ingreso o al editar el directorio cerraba la app.

### Security
- `FileProvider` restringido a `files/reports/`.
- Las reglas de respaldo y transferencia entre dispositivos excluyen también `reports/` y los archivos `-wal`/`-shm` de la base.
- Eliminado todo registro de datos operativos en Logcat.

### Removed
- Simulador de examen OS-10, junto con sus tablas.
- Pantallas, clases y layouts inalcanzables.
- Dependencias sin uso: WorkManager, LiveData y Apache POI.

## [1.0.0] - Versión inicial pública

### Added
- Gestión operativa de accesos vehiculares y bicicletero con arquitectura Clean + MVVM + Koin + Room (v17).
- PIN administrativo con hashing `PBKDF2WithHmacSHA256` + salt en `EncryptedSharedPreferences` y bloqueo por intentos fallidos.
- Pruebas unitarias para `PinSecurityHelper`, `RutUtils` y `ValidateRegistroUseCase`.
- Documentación técnica, `SECURITY.md`, `CONTRIBUTING.md` y CI en GitHub Actions.

### Security
- Dataset inicial 100 % sintético, respaldo automático deshabilitado y minificación en release.

[1.1.0]: https://github.com/dur1al/controlate-android/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/dur1al/controlate-android/releases/tag/v1.0.0
