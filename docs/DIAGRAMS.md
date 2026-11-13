# Diagramas del sistema — Controlate Android (v1.1.0)

Vista arquitectónica completa del sistema en formato Mermaid, alineada al código fuente actual (`Room v18`, Clean Architecture + MVVM + Koin).

---

## 1. Contexto funcional y capacidades por actor

```mermaid
flowchart LR
    G(["Guardia en garita"])
    A(["Administrador"])

    subgraph Operacion["Operación offline-first en dispositivo"]
        T["Iniciar / retomar / cerrar turno"]
        PV["Portería vehicular<br/>Ingreso, autocompletado, salida + Deshacer"]
        PB["Bicicletero<br/>Ingreso por RUT, autocompletado, salida + Deshacer"]
        EX["Simulador OS-10<br/>60 preguntas, 60 min, ≥ 70 %"]
    end

    subgraph Administracion["Panel protegido por PIN"]
        PIN["Autenticación PIN<br/>Setup inicial, PBKDF2, bloqueo 30 s"]
        MET["Métricas del día por punto de control"]
        REP["Historial de turnos y exportación PDF"]
        DIR["Directorio de personas<br/>CRUD, importación CSV atómica, PDF"]
        CLR["Limpieza de datos operativos"]
    end

    G --> T
    G --> PV
    G --> PB
    G --> EX

    A --> PIN
    PIN --> MET
    PIN --> REP
    PIN --> DIR
    PIN --> CLR
```

---

## 2. Arquitectura de paquetes y regla de dependencias (DIP)

La regla de dependencia apunta hacia `domain/`: `ui/` orquesta casos de uso y `data/` implementa los puertos definidos en `domain/repository/` y `domain/report/`. El grafo se ensambla en `di/AppModule.kt`.

```mermaid
flowchart TB
    subgraph DI["di/ (Koin: AppModule)"]
        MOD["dataModule · domainModule · presentationModule"]
    end

    subgraph UI["ui/ (Presentación · MVVM)"]
        direction LR
        ACT["Activities & BottomSheets<br/>Login · Porteria · Bicicletero<br/>Admin · ShiftReport · AdminDirectory<br/>DatabaseManager · Settings · Examen"]
        VM["ViewModels<br/>StateFlow&lt;UiState&gt; + EventChannel"]
        LBL["ui/common/<br/>AndroidReportLabels · Labels"]
        ACT -->|intenciones| VM
        VM -->|estado + eventos| ACT
    end

    subgraph DOMAIN["domain/ (Núcleo sin Android)"]
        direction LR
        UC["usecase/<br/>Shift · Entry · Admin<br/>BuildReport · Exam · Validate"]
        VAL["validation/ & importer/<br/>Rut · Patente · PersonaCsvParser"]
        TIME["time/<br/>Clock · TimeFormat"]
        PORT["Contratos (puertos)<br/>Repositories · SessionStore<br/>ReportExporter · ReportLabels"]
        UC --> VAL
        UC --> TIME
        UC --> PORT
    end

    subgraph DATA["data/ (Implementación y I/O)"]
        direction LR
        ROOM["local/database/ & dao/<br/>AppDatabase (v18) · Migration17To18<br/>DatabaseSeeder · DAOs"]
        REPO["repository/<br/>RoomShift · RoomVehicleEntry<br/>RoomBikeEntry · RoomPersona · RoomExam"]
        PDF["report/<br/>PdfReportWriter (Dispatchers.IO)<br/>ReportStore (files/reports)"]
        SEC["local/security/ & session/<br/>AdminPinManager · EncryptedKeyValueStore<br/>SessionManager"]
        REPO --> ROOM
    end

    VM --> UC
    VM --> SEC
    REPO -. implementa .-> PORT
    PDF -. implementa .-> PORT
    LBL -. implementa .-> PORT
    SEC -. implementa SessionStore .-> PORT
    MOD -. cablea .-> UI
    MOD -. cablea .-> DOMAIN
    MOD -. cablea .-> DATA
```

---

## 3. Contratos de dominio e implementaciones (Clases clave)

```mermaid
classDiagram
    class SessionStore {
        <<interface>>
        +activeShiftId() Long?
        +setActiveShift(id: Long)
        +clear()
    }
    class ShiftRepository {
        <<interface>>
        +insert(turno: Turno) Long
        +get(id: Long) Turno?
        +finish(id: Long, at: Long) Boolean
        +observeSummaries() Flow~List~ShiftSummary~~
        +countByPoint(from: Long, to: Long) List~PointCount~
    }
    class VehicleEntryRepository {
        <<interface>>
        +observeByShift(shiftId: Long, query: String) Flow~List~RegistroIngreso~~
        +insert(registro: RegistroIngreso) Long
        +markExit(id: Long, at: Long) Boolean
        +undoExit(id: Long, at: Long) Boolean
    }
    class BikeEntryRepository {
        <<interface>>
        +observeByShift(shiftId: Long, query: String) Flow~List~RegistroBicicleta~~
        +insert(registro: RegistroBicicleta) Long
        +latestByRut(rut: String) RegistroBicicleta?
        +markExit(id: Long, at: Long) Boolean
        +undoExit(id: Long, at: Long) Boolean
    }
    class PersonaRepository {
        <<interface>>
        +search(query: String) Flow~List~Persona~~
        +getByPatente(patente: String) Persona?
        +getByRut(rut: String) Persona?
        +insertIfAbsent(persona: Persona)
        +upsertAll(personas: List~Persona~)
    }
    class ExamRepository {
        <<interface>>
        +randomQuestions(limit: Int) List~ExamenPregunta~
        +saveResult(resultado: ExamenResultado) Long
    }
    class ReportExporter {
        <<interface>>
        +export(document: ReportDocument) File
    }

    SessionStore <|.. SessionManager
    ShiftRepository <|.. RoomShiftRepository
    VehicleEntryRepository <|.. RoomVehicleEntryRepository
    BikeEntryRepository <|.. RoomBikeEntryRepository
    PersonaRepository <|.. RoomPersonaRepository
    ExamRepository <|.. RoomExamRepository
    ReportExporter <|.. PdfReportWriter
```

---

## 4. Modelo de datos (Room v18)

`turnos` es la raíz de agregación. Los instantes se almacenan en **epoch millis** (`iniciadoAt`, `finalizadoAt`, `entradaAt`, `salidaAt`, `realizadoAt`). `personas` y las tablas de examen son independientes de `turnos`.

```mermaid
erDiagram
    TURNOS ||--o{ REGISTROS_INGRESO : "shiftId (CASCADE)"
    TURNOS ||--o{ REGISTROS_BICICLETA : "shiftId (CASCADE)"

    TURNOS {
        long id PK
        string guardia
        string controlPoint "porteria_2..bicicletero_2"
        string tipo "DAY | NIGHT"
        string fecha "yyyy-MM-dd"
        long iniciadoAt
        long finalizadoAt "nullable = abierto"
    }
    REGISTROS_INGRESO {
        long id PK
        long shiftId FK "idx"
        string patente "idx, normalizada"
        string nombreApellido
        string rut "nullable, canónico"
        string departamento
        string descripcionAuto
        string empresa "nullable"
        string tipoPersona "PersonaType.name"
        string observaciones "nullable"
        string padron "nullable"
        boolean esPadron
        long entradaAt "idx"
        long salidaAt "nullable"
    }
    REGISTROS_BICICLETA {
        long id PK
        long shiftId FK "idx"
        string rut "idx, canónico"
        string nombre
        string carrera
        string tipoPersona "PersonaType.name"
        string observacion "nullable"
        long entradaAt "idx"
        long salidaAt "nullable"
    }
    PERSONAS {
        int id PK
        string patente UK
        string nombreApellido
        string departamento
        string descripcionAuto
        string empresa "nullable"
        string rut "idx, nullable"
        string tipoPersona
        string padron "nullable"
    }
    EXAMEN_PREGUNTAS {
        int id PK
        string pregunta UK
        string opcionA
        string opcionB
        string opcionC
        string opcionD
        int respuestaCorrecta
        string categoria
        string explicacion
    }
    EXAMEN_RESULTADOS {
        long id PK
        string guardiaNombre
        long realizadoAt
        int puntaje
        int totalPreguntas
        float porcentaje
        boolean aprobado
        long tiempoEmpleadoSegundos
    }
```

---

## 5. Ciclo de vida del turno y cierre con reporte

```mermaid
stateDiagram-v2
    [*] --> SinTurno
    SinTurno --> Abierto : StartShiftUseCase<br/>(inserta turno, guarda id en SessionStore)
    Abierto --> Abierto : Registro de ingreso / salida / deshacer
    Abierto --> SinTurno : Sesión huérfana<br/>(GetActiveShiftUseCase limpia SessionStore)
    Abierto --> Cerrando : EndShiftUseCase(shiftId)
    Cerrando --> Cerrado : BuildReportUseCase + PdfReportWriter (IO)<br/>ShiftRepository.finish() + SessionStore.clear()
    Cerrando --> Cerrado : Fallo en PDF no bloquea cierre<br/>(devuelve Closed con pdf = null)
    Cerrado --> [*] : ClearOperationalDataUseCase<br/>(borrado en cascada)
```

---

## 6. Secuencias críticas

### 6.1 Registro vehicular con autocompletado y alta segura en directorio

```mermaid
sequenceDiagram
    actor G as Guardia
    participant Sheet as VehicleEntrySheet
    participant VM as PorteriaViewModel
    participant UC as RegisterVehicleEntryUseCase
    participant V as Rut / Patente
    participant VR as VehicleEntryRepository
    participant PR as PersonaRepository

    G->>Sheet: Ingresa patente o RUT
    Sheet->>VM: lookupPlate / lookupRut
    VM->>PR: getByPatente / getByRut
    PR-->>VM: Persona?
    VM-->>Sheet: FormEvent.Autofill

    G->>Sheet: Pulsa "Registrar ingreso"
    Sheet->>VM: register(VehicleEntryInput)
    VM->>UC: invoke(shiftId, input)
    UC->>V: validateVehicle + normalize
    alt Errores de validación
        UC-->>VM: RegisterEntryOutcome.Invalid(errors)
        VM-->>Sheet: formErrors (por campo)
    else Datos válidos
        UC->>VR: insert(RegistroIngreso con entradaAt)
        UC->>PR: getByRut(rut) ?: getByPatente(patente)
        alt Persona no existe en directorio
            UC->>PR: insertIfAbsent(Persona)
        end
        UC-->>VM: RegisterEntryOutcome.Saved(id)
        VM-->>Sheet: FormEvent.Saved
        VR-->>VM: Flow emite lista actualizada
    end
```

### 6.2 Acceso de administrador y bloqueo por intentos fallidos

```mermaid
sequenceDiagram
    actor A as Administrador
    participant L as LoginActivity
    participant VM as LoginViewModel
    participant P as AdminPinManager
    participant K as EncryptedKeyValueStore

    A->>L: Pulsa "Vista Admin"
    L->>VM: onAdminClicked()
    VM->>P: isPinConfigured() / remainingLockoutSeconds()
    alt Sin PIN configurado
        VM-->>L: AskPinSetup
        A->>L: Define PIN (4..8 dígitos) + confirmación
        L->>VM: setupPin(pin, confirm)
        VM->>P: setPin(pin) [Dispatchers.Default, PBKDF2 100k]
        P->>K: Guarda salt + hash
        VM-->>L: OpenAdmin
    else Bloqueado temporalmente
        VM-->>L: Message(espera N s)
    else PIN configurado
        VM-->>L: AskPin
        A->>L: Ingresa PIN
        L->>VM: submitPin(pin)
        VM->>P: verify(pin) [Dispatchers.Default, tiempo constante]
        alt PIN correcto
            P-->>VM: Granted
            VM-->>L: OpenAdmin
        else PIN incorrecto (menos de 5 fallos)
            P-->>VM: Denied(remainingAttempts)
            VM-->>L: AskPin(error)
        else 5.º fallo consecutivo
            P->>K: Fija lockoutUntil (+30 s) con commit síncrono
            P-->>VM: LockedOut(30 s)
            VM-->>L: Message(bloqueado 30 s)
        end
    end
```

---

## 7. Mapa de navegación entre pantallas

```mermaid
flowchart LR
    L["LoginActivity<br/>(Launcher)"]
    P["PorteriaActivity"]
    VS["VehicleEntrySheet"]
    B["BicicleteroDashboardActivity"]
    BS["BikeEntrySheet"]
    E["ExamenActivity"]
    A["AdminActivity<br/>(PIN)"]
    SR["ShiftReportActivity"]
    DM["DatabaseManagerActivity"]
    AD["AdminDirectoryActivity"]
    S["SettingsActivity"]

    L -->|"Turno vehicular"| P
    L -->|"Turno bicicletero"| B
    L -->|"Simulador OS-10"| E
    L -->|"Vista Admin (PIN OK)"| A

    P <--> VS
    B <--> BS
    P -->|"Cerrar turno (CLEAR_TASK)"| L
    B -->|"Cerrar turno (CLEAR_TASK)"| L

    A --> SR
    A --> DM
    A --> S
    DM --> AD
```

---

## 8. Arranque, migración y carga semilla (`App` → `DatabaseSeeder`)

```mermaid
sequenceDiagram
    participant App as App.onCreate()
    participant Koin as Koin (appModules)
    participant Seed as DatabaseSeeder
    participant DB as AppDatabase (Room v18)

    App->>Koin: startKoin(appModules)
    App->>Seed: launch(Dispatchers.IO) { seedIfNeeded() }
    Seed->>DB: examenDao().countPreguntas()
    Note over DB: Al abrir SQLite por 1.ª vez:<br/>si v17 -> Migration17To18;<br/>si nueva -> onDatabaseCreated()
    alt Instalación nueva y personas == 0
        Seed->>DB: personaDao().upsertAll(res/raw/seed_personas.json)
    end
    alt examen_preguntas == 0
        Seed->>DB: examenDao().insertPreguntas(res/raw/seed_preguntas.json)
    end
```

---

## 9. Pipeline de generación y compartición de reportes PDF

Separa la composición de datos (testeable en JVM) del renderizado Android (`PdfDocument` en `Dispatchers.IO`).

```mermaid
flowchart LR
    subgraph Domain["domain/"]
        BR["BuildReportUseCase"]
        RL["ReportLabels<br/>(interfaz)"]
        RD["ReportDocument<br/>(MetaField, Column, rows)"]
        RE["ReportExporter<br/>(interfaz)"]
        BR --> RL
        BR --> RD
        BR --> RE
    end

    subgraph Presentation["ui/common/"]
        ARL["AndroidReportLabels<br/>(strings.xml)"]
    end

    subgraph Data["data/report/"]
        PW["PdfReportWriter<br/>(Dispatchers.IO)"]
        RS["ReportStore<br/>(files/reports/*.pdf)"]
        FP["FileProvider<br/>(@xml/file_paths)"]
        PW --> RS --> FP
    end

    ARL -. implementa .-> RL
    PW -. implementa .-> RE
    FP -->|"ACTION_SEND"| SHARE(["Selector Android"])
```

---

## 10. Flujos de integridad operativa

### 10.1 Salida idempotente y Deshacer seguro

```mermaid
sequenceDiagram
    actor G as Guardia
    participant VM as Porteria / Bicicletero VM
    participant UC as MarkExitUseCase
    participant Repo as Vehicle / Bike Repository
    participant DAO as Room DAO

    G->>VM: markExit(id)
    VM->>UC: vehicle(id) / bike(id)
    UC->>Repo: markExit(id, at = clock.now())
    Repo->>DAO: UPDATE ... SET salidaAt = :at WHERE id = :id AND salidaAt IS NULL
    alt Fila actualizada (> 0)
        UC-->>VM: at (epoch millis)
        VM-->>G: Snackbar con acción "Deshacer"
        opt Guardia pulsa "Deshacer"
            G->>VM: undoExit(id, at)
            VM->>UC: undoVehicle(id, at) / undoBike(id, at)
            UC->>Repo: undoExit(id, at)
            Repo->>DAO: UPDATE ... SET salidaAt = NULL WHERE id = :id AND salidaAt = :at
        end
    else Ya tenía salida (0 filas)
        UC-->>VM: null (no-op idempotente)
    end
```

### 10.2 Importación CSV atómica del directorio

```mermaid
flowchart TD
    IN(["Archivo CSV"]) --> UC["ImportPersonasUseCase"]
    UC --> PARSE["PersonaCsvParser.parse(content)<br/>(Rut.isValid, Patente.isValid)"]
    PARSE --> CHK{"errors.isEmpty()?"}
    CHK -->|"No (cualquier línea inválida)"| ABORT["No escribe en Room<br/>Devuelve errores por línea"]
    CHK -->|"Sí y personas no vacío"| UPSERT["PersonaRepository.upsertAll(personas)"]
    UPSERT --> OK(["Directorio actualizado"])
```
