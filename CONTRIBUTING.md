# Guía de Contribución

## Flujo de Trabajo

1. Crea una rama descriptiva desde `main`: `feat/nombre-corto`, `fix/nombre-corto` o `docs/nombre-corto`.
2. Respeta las capas descritas en [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): las Activities solo renderizan estado y delegan en su ViewModel; la lógica de negocio va en casos de uso de `domain/` que dependen de interfaces, nunca de Room directamente.
3. Toda regla nueva de dominio o seguridad llega con su test en `app/src/test/` (JVM o Robolectric). Los ViewModels se prueban con los fakes de `porteria/testing/`.
4. Si modificas una entidad Room: incrementa la versión de `AppDatabase`, agrega una migración explícita y su test con `MigrationTestHelper`, y versiona el nuevo JSON de `app/schemas/`.
5. Externaliza todos los textos visibles en `app/src/main/res/values/strings.xml`.
6. **Privacidad obligatoria:** Nunca subas datos reales (nombres, patentes, RUTs, recintos) ni credenciales/PINs en código, `res/raw/` o tests. Usa únicamente datos sintéticos.

## Verificación Local

Configura JDK 17 (`JAVA_HOME`) y ejecuta antes de abrir un Pull Request:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Convención de Commits

Se utiliza [Conventional Commits](https://www.conventionalcommits.org/):

- `feat(scope): descripción`
- `fix(scope): descripción`
- `refactor(scope): descripción`
- `security(scope): descripción`
- `test(scope): descripción`
- `docs(scope): descripción`
