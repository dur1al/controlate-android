# Changelog

Todos los cambios relevantes de este proyecto se documentan en este archivo.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased]

### Added
- **Puntos de control administrables** (Panel admin › Puntos de control): agregar porterías y bicicleteros, renombrarlos y activarlos o desactivarlos. Se guardan en la tabla `puntos_control` (migración 19 → 20) con los puntos de fábrica precargados. Un punto nunca se borra: desactivarlo conserva sus turnos y reportes, y siempre queda al menos uno activo por tipo.
- Validación al salir de cada campo (patente, nombre, RUT, carrera) con las mismas reglas que al guardar.
- Batería de pruebas QA de validaciones: matriz de formatos de RUT, formateo tecla a tecla (también sobre un `EditText` real), nombre, patente, carrera, padrón, CSV y edición del directorio.

### Removed
- Simulador de examen OS-10: botón de inicio, pantallas, casos de uso, banco de preguntas y tablas (`examen_preguntas`, `examen_resultados`, migración 18 → 19).

### Changed
- Puntos de control vehiculares visibles como **Portería 1, 2 y 3**. Los ids persistidos (`porteria_2..4`) no cambian, para no dejar huérfanos los turnos y reportes existentes.
- Contacto en "Información": `github.com/dur1al`, con botón para abrir el perfil.
- Botones con contraste WCAG AA (texto ≥ 4.5:1) y estado deshabilitado distinguible; con colores dinámicos por punto, el texto y los iconos se eligen por contraste.
- El aviso de autocompletado y los errores al guardar se muestran en línea dentro del formulario, sin snackbars que tapen el botón "Registrar". Los snackbars de las pantallas principales se anclan sobre la barra inferior.
- Los formularios de ingreso se abren expandidos, llevan el foco al primer campo con error y limpian solo el error del campo corregido.

### Fixed
- Formateo del RUT al escribir: el cursor quedaba antes del último dígito y desordenaba lo escrito (`1,2,3,4` → `124-3`); borrar un punto o el guion no tenía efecto.
- Validación del RUT estricta: rechaza una `K` en el cuerpo, puntos mal ubicados y cuerpos que empiezan en 0 (antes se "limpiaban" y se validaba otro RUT). El campo solo admite dígitos, `K`, punto y guion.
- La edición del directorio no validaba RUT ni nombre y cerraba el diálogo aunque hubiera errores; ahora los muestra en cada campo y mantiene lo escrito.
- Nombres: obligan al menos una letra y rechazan dígitos y símbolos (máximo 60 caracteres).
- Doble toque en "Registrar" podía duplicar un ingreso: el botón se deshabilita mientras se guarda.
- Se rechaza el ingreso de una patente (o de un RUT en el bicicletero) que ya está dentro en el turno.
- Un fallo al guardar un ingreso cerraba la app; ahora se informa en el formulario.

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
