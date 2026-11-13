# Política de Seguridad

## Modelo de Seguridad Local (Offline-First)

`controlate-android` está diseñada para operar sin conexión a internet y sin telemetría externa:

- **Sin secretos en código fuente:** No existen PINs ni credenciales por defecto hardcodeadas. El PIN administrativo se define en el primer acceso y se almacena mediante hash criptográfico `PBKDF2WithHmacSHA256` (100.000 iteraciones, salt aleatorio de 16 bytes) dentro de `EncryptedSharedPreferences` (`AES256_SIV` / `AES256_GCM` respaldado por Android Keystore).
- **Protección contra fuerza bruta:** Tras 5 intentos fallidos consecutivos, el acceso administrativo aplica un bloqueo temporal de 30 segundos.
- **Aislamiento de respaldos:** `android:allowBackup="false"` junto con `backup_rules.xml` y `data_extraction_rules.xml` excluyen explícitamente `controlate_secure_prefs.xml`, `porteria_prefs.xml`, la base `porteria_db` (incluidos `-wal`/`-shm`) y la carpeta `reports/` de respaldos en la nube y transferencias entre dispositivos (en Android 12+ `allowBackup` no cubre la transferencia D2D).
- **Compartir con mínimo privilegio:** el `FileProvider` solo expone `files/reports/`; los PDF se comparten con permisos de lectura temporales.
- **Sin registros sensibles:** el código de producción no escribe datos operativos (nombres, RUT, patentes) en Logcat.
- **Entradas validadas:** RUT (módulo 11), patente, importación CSV con validación por línea y consultas Room parametrizadas.
- **Persistencia local:** Los registros operativos y el directorio residen exclusivamente en SQLite local (Room) dentro del almacenamiento privado de la aplicación.

## Versiones soportadas

Solo la última versión publicada en `main` recibe correcciones de seguridad.

## Reporte de Vulnerabilidades

Si detectas una vulnerabilidad de seguridad o fuga accidental de datos:
1. **No** abras un Issue público con detalles reproducibles del fallo.
2. Reporta el hallazgo de forma privada mediante [GitHub Security Advisories](https://github.com/dur1al/controlate-android/security/advisories/new) indicando pasos de reproducción, versión afectada e impacto estimado.
