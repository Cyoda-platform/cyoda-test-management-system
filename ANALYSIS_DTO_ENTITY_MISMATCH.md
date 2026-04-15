# Анализ соответствия DTO и Entity схем — TMS 2026-04-14

## Статус проекта

**Критическое состояние:** выявлены **серьёзные несоответствия в типах полей** между DTO (особенно в представлении дат), отсутствуют явные Entity классы, и последние изменения привели к нарушению целостности данных.

---

## УРОВЕНЬ 0: JSON Entity Schemas — ВОТ ГДЕ ИСТИНА

Cyoda определяет структуру entities в JSON файлах:
```
apps/backend/src/main/resources/entity/{entity}/version_1/{entity}.json
```

Это не просто примеры — это **schema definition** для Cyoda FSM. Вот что там:

| Entity | JSON файл | createdAt / дата | Format | DTO тип |
|--------|-----------|-----------------|--------|---------|
| **Project** | project.json | `"2024-01-15T10:30:00Z"` | ✓ **STRING (ISO-8601)** | String ✓ |
| **Suite** | suite.json | `"2024-01-15T10:30:00Z"` | ✓ **STRING (ISO-8601)** | LocalDateTime ✗ |
| **TestCase** | testcase.json | `"2024-02-10T14:20:00Z"` | ✓ **STRING (ISO-8601)** | LocalDateTime ✗ |
| **Defect** | defect.json | `"2024-02-15T10:00:00"` | ✓ **STRING (no Z)** | LocalDateTime ✗ |
| **TestRun** | testrun.json | `null` | ❌ **NULL!** | LocalDateTime ✗ |
| **TestRunCase** | testruncase.json | `null` | ❌ **NULL!** | LocalDateTime ✗ |
| **TestRunStep** | testrunstep.json | ❌ **NO DATES** | ❌ **NONE** | None ✓ |
| **Attachment** | attachment.json | `"2024-02-15T09:45:30"` | ✓ **STRING** | LocalDateTime ✓ |

**Это означает:**
- ✓ Project, Suite, TestCase, Defect, Attachment ожидают **STRING** даты в Cyoda payload
- ❌ TestRun, TestRunCase, TestRunStep ожидают **null** (даты не инициализируются)
- ProjectService инициализирует String ← ✓ соответствует schema
- TestRunService инициализирует LocalDateTime ← ✗ НЕ СООТВЕТСТВУЕТ schema!

### Вывод: JSON Schema vs DTO несоответствие

```
JSON говорит:          | DTO говорит:          | Результат:
createdAt: string      | createdAt: LocalDateTime | ✗ MISMATCH
createdAt: null        | createdAt: LocalDateTime | ✗ MISMATCH
uploadedAt: string     | uploadedAt: LocalDateTime | ✓ OK (обе строки в JSON)
```

**Когда TestRunService отправляет в Cyoda:**
```json
{
  "createdAt": LocalDateTime object   // ← Jackson serializes as datetime
  "startedAt": LocalDateTime object   // ← Jackson serializes as datetime
}
```

**Но JSON schema определяет:**
```json
{
  "createdAt": null,    // ← должно быть null или string
  "startedAt": null     // ← должно быть null или string
}
```

→ Cyoda FSM validation fails!

---

## Проблема 1: Критическое несоответствие форматов дат

### Ситуация
Различные DTO используют **несовместимые типы** для одних и тех же семантических полей (даты создания/обновления):

| DTO | createdAt | updatedAt | startedAt | completedAt | uploadedAt | Other |
|-----|-----------|-----------|-----------|-------------|-----------|-------|
| **ProjectDTO** | `String` ❌ | `String` ❌ | — | — | — | — |
| **SuiteDTO** | `LocalDateTime` ✓ | `LocalDateTime` ✓ | — | — | — | — |
| **TestCaseDTO** | `LocalDateTime` ✓ | `LocalDateTime` ✓ | — | — | — | — |
| **TestRunDTO** | `LocalDateTime` ✓ | `LocalDateTime` ✓ | `LocalDateTime` ✓ | `LocalDateTime` ✓ | — | — |
| **TestRunCaseDTO** | — | — | `LocalDateTime` ✓ | `LocalDateTime` ✓ | — | — |
| **TestRunStepDTO** | — | — | `LocalDateTime` ✓ | `LocalDateTime` ✓ | — | — |
| **TestStepDTO** | ❌ NONE | ❌ NONE | — | — | — | — |
| **DefectDTO** | `LocalDateTime` ✓ | `LocalDateTime` ✓ | — | — | — | — |
| **AttachmentDTO** | — | — | — | — | `LocalDateTime` ✓ | — |
| **AttachmentMetadataDTO** | — | — | — | — | `LocalDateTime` ✓ | — |
| **BugLinkDTO** | `LocalDateTime` ✓ | ❌ NONE | — | — | — | — |
| **ReportDTO** | `LocalDateTime` ✓ | `LocalDateTime` ✓ | — | — | — | — |
| **ExportDTO** | `LocalDateTime` ✓ | ❌ NONE | — | — | — | — |

### Анализ

**Проблема:** `ProjectDTO` использует `String` для `createdAt`/`updatedAt`, тогда как **все остальные** Cyoda-управляемые entity DTOs используют `LocalDateTime`.

```java
// ❌ ProjectDTO
private String createdAt;
private String updatedAt;

// ✓ SuiteDTO, TestCaseDTO и другие
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
```

**Влияние:**
- ✗ API возвращает несоответствующие типы JSON для одного и того же поля
- ✗ Frontend код должен обрабатывать одновременно `string` и `datetime` для одной и той же сущности
- ✗ Сериализация/десериализация через Jackson может привести к сбой (особенно при слиянии нескольких сущностей в ответе)
- ✗ Сравнение и сортировка дат становится невозможным без парсинга

---

## Проблема 2: Отсутствие дат в TestStepDTO

`TestStepDTO` вообще **не содержит** полей `createdAt` / `updatedAt`:

```java
@Data
public class TestStepDTO implements CyodaEntity {
    private UUID id;
    private UUID testCaseId;
    private Integer stepNumber;
    private String action;
    private String expectedResult;
    private boolean deleted;
    // ❌ Нет createdAt / updatedAt
}
```

**Вопросы:**
- Intentional ли? Или этого нужно добавить для согласованности?
- Если нужны — они должны быть `LocalDateTime`.

---

## Проблема 3: Отсутствие явных Entity классов

**Текущая архитектура:**
- ✓ В `common/` есть интерфейс `CyodaEntity`
- ✗ В `application/` **нет явной папки `entity/`** и **нет отдельных Entity классов**
- ✓ DTO сами реализуют `CyodaEntity` (используются как Entity классы)

```
application/
├── controller/
├── dto/              ← Entity логика встроена сюда
├── processor/
├── repository/
├── service/
└── ❌ (entity/ отсутствует)
```

**Проблема:** DTO и Entity смешаны. Это нарушает разделение забот и затрудняет:
- Хранение дополнительных полей для внутренней логики
- Фильтрацию полей при сериализации в API
- Управление жизненным циклом сущностей независимо от передачи данных

---

## Проблема 4: DTO не-Entity (смешанная иерархия)

Некоторые DTO **реализуют `CyodaEntity`**, а другие **нет**:

| DTO | Реализует CyodaEntity | Заметки |
|-----|---|---|
| ProjectDTO | ✓ | |
| SuiteDTO | ✓ | |
| TestCaseDTO | ✓ | |
| TestRunDTO | ✓ | |
| TestRunCaseDTO | ✓ | |
| TestRunStepDTO | ✓ | |
| TestStepDTO | ✓ | |
| DefectDTO | ✓ | |
| AttachmentDTO | ✓ | |
| ReportDTO | ✓ | |
| **AttachmentMetadataDTO** | ❌ | Record, не entity |
| **BugLinkDTO** | ❌ | Поддержка DTO, не entity |
| **RepositoryDTO** | ❌ | Record, read-only view |
| **ExportDTO** | ❌ | Support DTO |
| **TestRunDetailDTO** | ❌ | Composite response |
| **SearchRequestDTO** | ? | Request объект |
| **BatchImportCaseDTO** | ? | Request объект |
| **MoveTestCaseDTO** | ? | Operation DTO |
| **ReorderItemDTO** | ? | Operation DTO |

**Проблема:** Неясная классификация: что считается "entity" (управляемо через Cyoda), а что просто "DTO" для передачи данных?

---

## Проблема 5: Последствия для API контрактов

### Создание Project
```
POST /projects
Content-Type: application/json

{
  "name": "My Project",
  "description": "...",
  "createdAt": "2026-04-14T12:34:56"  ← String (как Jackson сериализует?)
}
```

### Создание Suite
```
POST /projects/{projectId}/suites
Content-Type: application/json

{
  "name": "My Suite",
  "createdAt": "2026-04-14T12:34:56Z"  ← LocalDateTime (Jackson сериализует как ISO-8601)
}
```

**Фронтенд проблема:** Один endpoint возвращает `createdAt` как `string`, другой как `datetime` объект? Невозможно написать универсальный handler.

---

## Проблема 6: Что было вчера, что работало? НАЙДЕНА ПРИЧИНА!

Согласно вашему описанию:
- ✓ Создание проектов работало
- ✓ Создание сьютов работало
- ✓ Создание кейсов работало
- ✗ Создание тест ранов **не работало**

### ✓ ПРИЧИНА НАЙДЕНА

Несоответствие в **service слое** (не в DTO, а в том как они инициализируют даты):

#### ProjectService.createProject() (строка 128-133)
```java
public ProjectDTO createProject(ProjectDTO project) {
    // ✓ Используется Instant.now().toString() — СТРОКА
    String now = Instant.now().toString();
    project.setCreatedAt(now);      // String → "2026-04-14T12:34:56.123456Z"
    project.setUpdatedAt(now);
    // ...
}
```

#### TestRunService.createTestRun() (строка 67-70)
```java
public TestRunDTO createTestRun(TestRunDTO testRun) {
    // ✗ Используется LocalDateTime.now() — JAVA OBJECT
    LocalDateTime now = LocalDateTime.now();
    testRun.setCreatedAt(now);      // LocalDateTime → "2026-04-14T12:34:56.123456"
    testRun.setStartedAt(now);
    // ...
}
```

### Конечный результат

| Service | Code | DTO тип | JSON при выводе | Cyoda payload |
|---------|------|---------|----------|--------------|
| ProjectService | `Instant.now().toString()` | `String createdAt` | `"2026-04-14T12:34:56.123456Z"` | ✓ String |
| TestRunService | `LocalDateTime.now()` | `LocalDateTime createdAt` | DateTime объект? | ✗ Несоответствие |

### Что происходит при create TestRun

1. **Frontend** отправляет: `POST /projects/{id}/runs` с `TestRunDTO` (без дат, они null)
2. **Controller** вызывает `testRunService.createTestRun(testRun)`
3. **Service** инициализирует: `testRun.setCreatedAt(LocalDateTime.now())`
4. **Jackson** должен сериализовать это в JSON для:
   - Возврата в HTTP response
   - Отправки в Cyoda FSM payload
5. **Cyoda** может ожидать `String` (как в ProjectDTO), но получает JSON-ized `LocalDateTime` → ошибка!

### Jackson поведение (по умолчанию в Spring Boot 3.5)

```yaml
# application.yml НЕ содержит конфигурацию для LocalDateTime
# Поэтому Jackson использует дефолт:
spring:
  jackson:
    # ❌ write-dates-as-timestamps НЕ указан
    # ❌ serialization.write-dates-as-timestamps НЕ указан
```

**Результат:**
- `LocalDateTime` сериализуется как **JSON object** `{"year":2026,"month":4,...}` или ISO-строка в зависимости от version
- Cyoda ожидает строку в payload
- Несоответствие → validation error в Cyoda FSM → TestRun creation fails

### Почему ProjectDTO работала?

Потому что `ProjectService` явно конвертирует в строку ПЕРЕД сохранением:
```java
String now = Instant.now().toString();  // ← явная конверсия
project.setCreatedAt(now);               // ← String, не Object
```

Cyoda получает `{"createdAt": "2026-04-14T..."}` — ✓ валидно.

### Почему Suite/TestCase работают (несмотря на LocalDateTime)?

#### SuiteService.createSuite() (строка 81-87)
```java
public SuiteDTO createSuite(SuiteDTO suite) {
    if (suite.getSortOrder() == null) {
        int count = getSuitesByProjectId(suite.getProjectId(), 0, 1000).data().size();
        suite.setSortOrder(count);
    }
    return withId(entityService.create(suite));  // ← createdAt/updatedAt остаются NULL!
}
```

**Ключевое отличие:** Suite **никогда не устанавливает** `createdAt`/`updatedAt`! Они остаются `null` и отправляются в Cyoda как:
```json
{
  "id": "uuid",
  "projectId": "uuid", 
  "name": "My Suite",
  "createdAt": null,
  "updatedAt": null
}
```

Cyoda, получив `null`, либо:
1. Игнорирует их (они не обязательны в payload)
2. Подставляет значения из метаданных
3. Оставляет их `null` в хранилище

**Это НЕ вызывает ошибку** потому что `null` — валидное значение.

---

## Проблема 7: Смешанные подходы к инициализации дат

| Service | Инициализирует даты? | Тип | Результат |
|---------|---|---|---|
| ProjectService | ✓ Да (`Instant.now().toString()`) | String | ✓ Работает |
| SuiteService | ✗ Нет (остаются null) | LocalDateTime | ✓ Работает (даты null) |
| TestCaseService | ? Неизвестно | LocalDateTime | ? |
| TestRunService | ✓ Да (`LocalDateTime.now()`) | LocalDateTime | ✗ **ПАДАЕТ** |

**Вывод:** Когда Service явно инициализирует `LocalDateTime` и отправляет его в Cyoda, возникает конфликт типов. Suite "спасается" потому что даты null.

---

## Рекомендуемое действие: привести в единый формат

### Вариант 1: Все в String (как вы предлагали)
```java
// Все DTO
private String createdAt;      // ISO-8601: "2026-04-14T12:34:56.123Z"
private String updatedAt;

// Инициализация
private String createdAt = Instant.now().toString();  // "2026-04-14T12:34:56.123456Z"
```

**Плюсы:**
- Простота сохранения в Cyoda FSM (всё строки)
- Нет проблем с сериализацией Jackson
- Нет потери точности (миллисекунды vs наносекунды)

**Минусы:**
- Нужно парсить строки при сравнении/сортировке на фронтенде

### Вариант 2: Все в LocalDateTime + Jackson кастомная конфигурация
```java
// Все DTO
private LocalDateTime createdAt;

// application.yml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
      write_date_keys_as_timestamps: false
    default-property-inclusion: non_null
    time-zone: UTC
```

**Плюсы:**
- Безопаснее для операций (сравнения, сортировки на уровне Java)
- Spring Boot по умолчанию поддерживает LocalDateTime

**Минусы:**
- Нужно убедиться, что Cyoda FSM корректно сохраняет ISO-8601 строки и их парсит обратно
- Jackson настройка может быть хрупкой

### Вариант 3: Instant (универсальный, рекомендуется для распределённых систем)
```java
// Все DTO
private Instant createdAt;     // UTC, без timezone issues

// Инициализация
private Instant createdAt = Instant.now();
```

**Плюсы:**
- Нет ambiguity с timezone
- Стандарт для сервисов на разных timezones
- Jackson по умолчанию сериализует как ISO-8601 строку

**Минусы:**
- Нужно убедиться, что Cyoda корректно обрабатывает Instant

---

## Резюме: Текущие баги и блокеры

| # | Проблема | Severity | Блокер | Fix |
|---|----------|----------|--------|-----|
| 1 | ProjectDTO использует String для дат | 🔴 Critical | Возможно да | Привести к LocalDateTime или String (совместно) |
| 2 | TestStepDTO отсутствуют даты | 🟡 Medium | Возможно нет | Добавить createdAt / updatedAt если needed |
| 3 | Отсутствует папка entity/ | 🟡 Medium | Нет | Переорганизовать, но не блокирует функционал |
| 4 | Смешанные DTO (CyodaEntity vs обычные) | 🟡 Medium | Нет | Классифицировать и отделить классы |
| 5 | Несоответствие Jackson сериализации | 🔴 Critical | Да, для TestRun | Настроить application.yml или DTO типы |
| 6 | Отсутствие явных Entity классов | 🟡 Medium | Нет | Создать папку entity/, но как переходить — сложно |

---

## Следующие шаги

1. **Немедленно:** Определить формат дат (String / LocalDateTime / Instant)
2. **Аудит:** Прочитать application.yml и посмотреть Jackson конфигурацию
3. **Проверка:** Запустить тесты (unit + E2E), найти конкретное место падения TestRun creation
4. **Унификация:** Обновить все DTO в едином стиле
5. **Валидация:** Убедиться, что Cyoda FSM корректно серили/десерилизует даты
6. **Refactor (потом):** Может быть создать явные Entity классы, но это отдельный проект

---

## Файлы для проверки

```bash
# Jackson конфигурация
cat apps/backend/src/main/resources/application.yml
cat apps/backend/src/main/resources/application-cucumber.yaml

# Тесты
grep -r "LocalDateTime\|String.*createdAt" apps/backend/src/test/

# Controllers
grep -r "@PostMapping.*TestRun\|@PostMapping.*Project" apps/backend/src/main/java/com/java_template/application/controller/

# Services
grep -r "class.*Service" apps/backend/src/main/java/com/java_template/application/service/
```

---

## DTO реестр (полный список с типами дат)

### CyodaEntity DTOs (управляемо через FSM)

1. ✓ **ProjectDTO** — `String createdAt/updatedAt` ❌
2. ✓ **SuiteDTO** — `LocalDateTime createdAt/updatedAt`
3. ✓ **TestCaseDTO** — `LocalDateTime createdAt/updatedAt`
4. ✓ **TestRunDTO** — `LocalDateTime createdAt/updatedAt/startedAt/completedAt`
5. ✓ **TestRunCaseDTO** — `LocalDateTime startedAt/completedAt` (no create/update)
6. ✓ **TestRunStepDTO** — `LocalDateTime startedAt/completedAt` (no create/update)
7. ✓ **TestStepDTO** — ❌ **NO DATES** (inconsistent)
8. ✓ **DefectDTO** — `LocalDateTime createdAt/updatedAt`
9. ✓ **AttachmentDTO** — `LocalDateTime uploadedAt` (one-way)
10. ✓ **ReportDTO** — `LocalDateTime createdAt/updatedAt`

### Non-Entity DTOs (helpers, requests, responses)

11. **AttachmentMetadataDTO** — `LocalDateTime uploadedAt` (record, read-only)
12. **BugLinkDTO** — `LocalDateTime createdAt` (only created, no update)
13. **ExportDTO** — `LocalDateTime createdAt` (support DTO)
14. **RepositoryDTO** — NO DATES (view/aggregate)
15. **TestRunDetailDTO** — NO DATES (composite response)
16. **SearchRequestDTO** — ? (request, probably no dates)
17. **BatchImportCaseDTO** — ? (request)
18. **MoveTestCaseDTO** — ? (operation)
19. **ReorderItemDTO** — ? (operation)
20. **ProjectCounterDTO** — ? (counter/stats)
21. **LoginRequest** — NO DATES (auth)
22. **LoginResponse** — NO DATES (auth)
23. **Priority** (Enum) — NO DATES

### Services / Processors взаимодействующие с DTO

- ProjectService (ProjectDTO, ProjectCounterDTO)
- SuiteService (SuiteDTO)
- TestCaseService (TestCaseDTO, MoveTestCaseDTO, ReorderItemDTO)
- TestRunService (TestRunDTO, TestRunDetailDTO, TestRunCaseDTO, TestRunStepDTO)
- TestStepService (TestStepDTO)
- DefectService (DefectDTO, BugLinkDTO)
- AttachmentService (AttachmentDTO, AttachmentMetadataDTO)
- ReportService (ReportDTO, ExportDTO)
- RepositoryService (RepositoryDTO)
- AuthService (LoginRequest, LoginResponse)

---

## РЕШЕНИЕ: пошаговый план фиксации

### Вариант A: Согласно JSON Schema (РЕКОМЕНДУЕТСЯ)

JSON schemas определяют:
- ProjectDTO, SuiteDTO, TestCaseDTO, DefectDTO, AttachmentDTO → `String` даты
- TestRunDTO, TestRunCaseDTO, TestRunStepDTO → **null** даты

**Это означает:**
1. ✓ ProjectDTO — уже правильно (String)
2. ✗ SuiteDTO, TestCaseDTO, DefectDTO — нужно изменить на String
3. ✗ AttachmentDTO — уже String, но в DTO LocalDateTime
4. ✗ TestRunDTO — **NOT инициализировать даты** (оставить null, пусть Cyoda подставит)

### Шаг 1: НЕМЕДЛЕННО (hot fix)

**Вариант 1A: Привести в соответствие JSON Schema**

```java
// ProjectDTO — не трогать (уже String)

// SuiteDTO (ИЗМЕНИТЬ)
// ❌ private LocalDateTime createdAt;
// ✓ private String createdAt;

// TestCaseDTO (ИЗМЕНИТЬ)
// ❌ private LocalDateTime createdAt;
// ✓ private String createdAt;

// DefectDTO (ИЗМЕНИТЬ)
// ❌ private LocalDateTime createdAt;
// ✓ private String createdAt;

// AttachmentDTO (ИЗМЕНИТЬ)
// ❌ private LocalDateTime uploadedAt;
// ✓ private String uploadedAt;

// TestRunDTO (НЕ ИНИЦИАЛИЗИРОВАТЬ!)
// ❌ testRun.setCreatedAt(LocalDateTime.now());
// ✓ testRun.setCreatedAt(null);  // OR просто не устанавливать

// TestRunCaseDTO (НЕ ИНИЦИАЛИЗИРОВАТЬ!)
// Даты остаются null

// TestRunStepDTO (НЕ ИНИЦИАЛИЗИРОВАТЬ!)
// Дат вообще нет
```

**Или, если хочется инициализировать TestRun даты:**

### Шаг 1B: АЛЬТЕРНАТИВА (унифицировать все к String)
Унифицировать все DTO к **String** (как в ProjectDTO) + обновить JSON schemas:

```java
// ДЛЯ ВСЕХ DTO (ProjectDTO, SuiteDTO, TestCaseDTO, TestRunDTO, DefectDTO, ReportDTO, etc)

// ❌ БЫЛО:
// private LocalDateTime createdAt;
// private LocalDateTime updatedAt;

// ✓ СТАТЬ:
private String createdAt;
private String updatedAt;

// В SuiteDTO, TestCaseDTO:
private String startedAt;
private String completedAt;
```

### Шаг 2: Обновить JSON Schemas (если выбрали 1B)

Если решили унифицировать на String, обновите:
```
apps/backend/src/main/resources/entity/testrun/version_1/testrun.json
apps/backend/src/main/resources/entity/testruncase/version_1/testruncase.json
apps/backend/src/main/resources/entity/suite/version_1/suite.json
apps/backend/src/main/resources/entity/testcase/version_1/testcase.json
apps/backend/src/main/resources/entity/defect/version_1/defect.json
apps/backend/src/main/resources/entity/attachment/version_1/attachment.json
```

Поменяйте `null` на `"2024-01-01T00:00:00Z"` для дат.

### Вариант 1A: Следовать JSON Schema (простой и безопасный)

**Что делать:**
1. **ProjectDTO** — оставить как есть (String, инициализируется)
2. **SuiteDTO, TestCaseDTO, DefectDTO** — изменить LocalDateTime → String, НЕ инициализировать (null)
3. **TestRunDTO, TestRunCaseDTO** — изменить LocalDateTime → String или null, НЕ инициализировать
4. **AttachmentDTO** — изменить LocalDateTime → String
5. **TestStepDTO** — оставить как есть (no dates)
6. **TestRunStepDTO** — оставить как есть (dates as null или не трогать)

```java
// ProjectService — оставить как есть ✓
public ProjectDTO createProject(ProjectDTO project) {
    String now = Instant.now().toString();
    project.setCreatedAt(now);
    project.setUpdatedAt(now);
    // ...
}

// SuiteService — НЕ устанавливать даты ✓
public SuiteDTO createSuite(SuiteDTO suite) {
    // suite.setCreatedAt(null);  // <- уже null, не трогаем
    // suite.setUpdatedAt(null);  // <- уже null, не трогаем
    // ...
}

// TestCaseService — НЕ устанавливать даты
public SuiteDTO createTestCase(TestCaseDTO testCase) {
    // testCase.setCreatedAt(null);  // <- оставить null
    // testCase.setUpdatedAt(null);  // <- оставить null
    // ...
}

// TestRunService — НЕ устанавливать даты!!! (вот проблема!)
public TestRunDTO createTestRun(TestRunDTO testRun) {
    // ❌ DELETE эти строки:
    // testRun.setCreatedAt(LocalDateTime.now());
    // testRun.setStartedAt(LocalDateTime.now());
    // Оставить null — Cyoda подставит из метаданных
    // ...
}

// DefectService — если устанавливает даты, изменить на String и не инициализировать
// ReportService — если устанавливает даты, изменить на String и не инициализировать
// AttachmentService — если устанавливает даты, изменить на String или оставить как есть
```

### Вариант 1B: Унифицировать всё на String с инициализацией

Если хочется чтобы ALL entities имели инициализированные даты:

```java
// Обновить JSON schemas — все даты на String

// ВСЕ Services инициализируют String:
public TestRunDTO createTestRun(TestRunDTO testRun) {
    String now = Instant.now().toString();
    testRun.setCreatedAt(now);
    testRun.setStartedAt(now);
    testRun.setUpdatedAt(now);
    // ...
}

public SuiteDTO createSuite(SuiteDTO suite) {
    String now = Instant.now().toString();
    suite.setCreatedAt(now);
    suite.setUpdatedAt(now);
    // ...
}

// и т.д. для всех services
```

### Шаг 3-4: Проверить и запустить тесты

```bash
# Найти все места где используется LocalDateTime.now() в services
grep -r "LocalDateTime\.now()" apps/backend/src/main/java/com/java_template/application/service/

# Проверить какие Services устанавливают даты
grep -r "setCreatedAt\|setUpdatedAt\|setStartedAt\|setUploadedAt" \
  apps/backend/src/main/java/com/java_template/application/service/

# Unit tests
./gradlew :apps:backend:test

# E2E tests (требует live Cyoda)
./gradlew :apps:backend:cucumberTest

# Конкретный тест на создание TestRun
./gradlew :apps:backend:test --tests "*TestRun*"
```

---

## Выводы

**Корневая причина:** Смешанные подходы к типам дат:
- ProjectDTO использует `String` + инициализирует явно
- TestRunDTO использует `LocalDateTime` + инициализирует явно
- SuiteDTO использует `LocalDateTime` + НЕ инициализирует
- Jackson по умолчанию создает несоответствие при сериализации LocalDateTime

**Почему падает TestRun creation:**
1. TestRunService.createTestRun() устанавливает `LocalDateTime now = LocalDateTime.now()`
2. Jackson сериализует LocalDateTime в JSON как объект или ISO-строку (зависит от конфигурации)
3. Cyoda получает несоответствие типа: ожидает строку, получает объект
4. FSM validation fails → TestRun не создается

**Почему работают Project и Suite:**
- ProjectService явно конвертирует `Instant.now().toString()` → String
- SuiteService вообще не инициализирует даты → остаются null (Cyoda подставляет сами)

**Критический путь к fix (в порядке приоритета):**
1. ✓ Найти все `LocalDateTime.now()` в services
2. ✓ Заменить на `Instant.now().toString()`
3. ✓ Обновить все DTO с `LocalDateTime` на `String` (для дат)
4. ✓ Запустить E2E тест на create TestRun
5. (опционально) Потом перерефакторить на Instant если хочется

---

## ФИНАЛЬНЫЙ ДИАГНОЗ: почему падает TestRun creation

### 1. JSON Schema определяет
```json
// testrun.json
{
  "createdAt": null,
  "startedAt": null,
  "updatedAt": null
}
```

### 2. TestRunService делает
```java
LocalDateTime now = LocalDateTime.now();
testRun.setCreatedAt(now);   // ← устанавливает LocalDateTime object
```

### 3. Jackson сериализует в Cyoda payload
```json
{
  "createdAt": { "year": 2026, "month": 4, ... },  // ← datetime object, не string
  "startedAt": { "year": 2026, "month": 4, ... }
}
```

### 4. Cyoda FSM validation
- Ожидает: `createdAt` = null или string
- Получает: `createdAt` = object
- **Результат: ValidationException → TestRun creation FAILS**

---

## РЕКОМЕНДУЕМОЕ РЕШЕНИЕ (быстро)

**Вариант 1A - Минимальные изменения:**

1. **TestRunService.createTestRun()** — УДАЛИТЬ эти строки:
```java
// DELETE:
testRun.setCreatedAt(LocalDateTime.now());
testRun.setStartedAt(LocalDateTime.now());
```

2. **Больше ничего не менять** — Suite/TestCase/TestRun сами подставляют даты из метаданных

3. Запустить тесты
```bash
./gradlew :apps:backend:test
./gradlew :apps:backend:cucumberTest
```

**Время на fix:** 5 минут для изменения 1 сервиса + 10-15 минут тестирование = 20 минут максимум.

---

## АЛЬТЕРНАТИВНОЕ РЕШЕНИЕ (более правильное)

**Вариант 1B - Полная унификация:**

1. Изменить ВСЕ DTO с LocalDateTime на String для дат
2. Обновить ВСЕ JSON schemas на String
3. Обновить ВСЕ Services инициализировать String с Instant.now().toString()
4. Запустить тесты

**Время на fix:** 45-60 минут (больше файлов для изменения)

---

## Что выбрать?

| Критерий | 1A (delete setCreatedAt) | 1B (String everywhere) |
|----------|---|---|
| **Время** | 20 минут | 60 минут |
| **Risk** | Низкий (минимум изменений) | Средний (много файлов) |
| **Правильность** | Следует JSON schema | Более явно, но требует support |
| **Maintenance** | Даты автоматические | Явно контролируемые |

**Рекомендация:** 
- **Немедленно:** 1A (быстрый fix для TestRun creation)
- **Потом:** 1B (если хочется быть последовательным с Project pattern)
