# Guía de Contribución

## Flujo de Trabajo

1. Crea una rama descriptiva desde `main`: `feat/nombre-corto`, `fix/nombre-corto` o `docs/nombre-corto`.
2. Mantén la separación por capas (`ui` -> `domain` -> `data`). No acoples lógica de negocio ni consultas SQL en `Activity` o `Adapter`.
3. Externaliza todos los textos visibles en `app/src/main/res/values/strings.xml`.
4. **Privacidad obligatoria:** Nunca subas datos reales (nombres, patentes, RUTs, recintos) ni credenciales/PINs en código, `res/raw/` o tests. Usa únicamente datos sintéticos.

## Verificación Local

Configura JDK 17 (`JAVA_HOME`) y ejecuta antes de abrir un Pull Request:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Convención de Commits

Se utiliza [Conventional Commits](https://www.conventionalcommits.org/):

- `feat(scope): descripción`
- `fix(scope): descripción`
- `refactor(scope): descripción`
- `security(scope): descripción`
- `test(scope): descripción`
- `docs(scope): descripción`
