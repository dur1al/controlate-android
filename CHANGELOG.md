# Changelog

Todos los cambios relevantes de este proyecto se documentan en este archivo.

## [1.0.0] - Versión Inicial Pública Limpia

### Added
- Gestión operativa de accesos vehiculares y bicicletero con arquitectura Clean + MVVM + Koin + Room (v17).
- Configuración inicial de PIN administrativo con hashing `PBKDF2WithHmacSHA256` + salt en `EncryptedSharedPreferences` y bloqueo por intentos fallidos.
- Pruebas unitarias para `PinSecurityHelper`, `RutUtils` y `ValidateRegistroUseCase`.
- Documentación técnica con diagramas Mermaid, ADRs (`docs/ARCHITECTURE.md`), `SECURITY.md`, `CONTRIBUTING.md` y CI en GitHub Actions.

### Security
- Eliminados datos personales reales y archivos CSV/XLSX de ejemplo sin uso.
- Reemplazado el directorio inicial por dataset 100% sintético.
- Deshabilitado respaldo automático (`android:allowBackup="false"`) y excluidos archivos sensibles en `backup_rules.xml` y `data_extraction_rules.xml`.
- Habilitada minificación y shrink de recursos (`R8`/`ProGuard`) en compilaciones `release`.
