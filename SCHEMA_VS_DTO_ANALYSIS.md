# Детальный анализ: JSON Schemas vs DTO классы
**Дата:** 2026-04-14 | **Статус:** Audit

---

## Методология

Для каждой entity сравниваем:
1. **JSON Schema** (`apps/backend/src/main/resources/entity/{entity}/version_1/{entity}.json`)
2. **DTO класс** (`apps/backend/src/main/java/com/java_template/application/dto/{Entity}DTO.java`)
3. **Service логика** (как инициализируются поля)

**Вопросы для каждой:**
- ✓ Все ли поля из JSON есть в DTO?
- ✓ Все ли поля из DTO есть в JSON?
- ✓ Типы полей совпадают?
- ✓ Даты инициализируются или null?
- ✓ Какие поля required vs optional?

---

## 1. PROJECT

### JSON Schema (project.json)
```json
{
  "id": "proj-001",
  "name": "E-Commerce Platform",
  "description": "Testing suite...",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```
**Поля:** id, name, description, createdAt (String), updatedAt (String)

### DTO (ProjectDTO)
```java
private UUID id;
private String name;
private String description;
private String createdAt;        // ✓ matches JSON
private String updatedAt;        // ✓ matches JSON
```
**Поля:** id, name, description, createdAt (String), updatedAt (String)

### Service (ProjectService.createProject)
```java
String now = Instant.now().toString();
project.setCreatedAt(now);       // ✓ инициализируется
project.setUpdatedAt(now);       // ✓ инициализируется
```

### ✅ ВЕРДИКТ: PERFECT MATCH
- JSON schema ✓ соответствует DTO
- Типы ✓ совпадают (String для дат)
- Service ✓ правильно инициализирует
- **Статус:** НЕ МЕНЯТЬ

---

## 2. SUITE

### JSON Schema (suite.json)
```json
{
  "id": "suite-001",
  "projectId": "ecommerce-v2",
  "name": "Checkout Flow",
  "description": "Test suite covering...",
  "parentSuiteId": null,
  "sortOrder": 1,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```
**Поля:** id, projectId, name, description, parentSuiteId (null), sortOrder, createdAt (String), updatedAt (String)

### DTO (SuiteDTO)
```java
private UUID id;
private UUID projectId;
private String name;
private String description;
// ❌ NO parentSuiteId
private Integer sortOrder;
private LocalDateTime createdAt;    // ❌ type mismatch (Schema: String)
private LocalDateTime updatedAt;    // ❌ type mismatch (Schema: String)
```

### Service (SuiteService.createSuite)
```java
public SuiteDTO createSuite(SuiteDTO suite) {
    if (suite.getSortOrder() == null) {
        int count = getSuitesByProjectId(...).data().size();
        suite.setSortOrder(count);
    }
    return withId(entityService.create(suite));  // ❌ createdAt/updatedAt остаются null!
}
```

### ⚠️ ВЕРДИКТ: MISMATCH (3 проблемы)
1. **parentSuiteId** отсутствует в DTO (может быть скрыта, но есть в JSON)
2. **Типы дат** — JSON: String, DTO: LocalDateTime
3. **Инициализация дат** — NULL (но JSON показывает String)

### РЕКОМЕНДАЦИЯ:
```
ВАРИАНТ A (быстро):
- Изменить DTO: LocalDateTime → String для createdAt/updatedAt
- Оставить Service как есть (даты null)

ВАРИАНТ B (полностью):
- Изменить DTO: LocalDateTime → String
- Обновить Service инициализировать: suite.setCreatedAt(Instant.now().toString())
- Обновить JSON Schema: добавить значения для дат вместо null
```

---

## 3. TESTCASE

### JSON Schema (testcase.json)
```json
{
  "id": "tc-001",
  "projectId": "ecommerce-v2",
  "suiteId": "suite-001",
  "displayId": "TC-1",
  "title": "Verify successful checkout...",
  "description": "User adds items...",
  "preconditions": "User is logged in...",
  "priority": "HIGH",
  "sortOrder": 1,
  "deleted": false,
  "createdAt": "2024-02-10T14:20:00Z",
  "updatedAt": "2024-02-10T14:20:00Z"
}
```
**Поля:** id, projectId, suiteId, displayId, title, description, preconditions, priority, sortOrder, deleted, createdAt (String), updatedAt (String)

### DTO (TestCaseDTO)
```java
private UUID id;
private UUID projectId;
private UUID suiteId;
private String displayId;
private String title;
private String description;
private String preconditions;
private Priority priority;          // ✓ enum
private Integer sortOrder;
private boolean deleted;
private LocalDateTime createdAt;    // ❌ type mismatch (Schema: String)
private LocalDateTime updatedAt;    // ❌ type mismatch (Schema: String)
```

### Service (TestCaseService.createTestCase) — NEED TO CHECK
```bash
# Need to look at actual implementation
```

### ⚠️ ВЕРДИКТ: PARTIAL MISMATCH
1. **Структура** ✓ полностью совпадает
2. **Типы дат** ❌ JSON: String, DTO: LocalDateTime
3. **Priority type** ✓ enum совпадает (HIGH, MEDIUM, LOW)

### РЕКОМЕНДАЦИЯ:
- Изменить DTO: LocalDateTime → String для createdAt/updatedAt

---

## 4. TESTRUN ← ГЛАВНАЯ ПРОБЛЕМА

### JSON Schema (testrun.json)
```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "projectId": "00000000-0000-0000-0000-000000000002",
  "displayId": "TR-1",
  "name": "Sample Regression Run",
  "environment": "staging",
  "buildVersion": "v1.0.0",
  "description": "Sample test run...",
  "status": "initial",
  "passed": 0,
  "failed": 0,
  "skipped": 0,
  "untested": 0,
  "startedAt": null,           // ← NULL!
  "completedAt": null,         // ← NULL!
  "createdAt": null,           // ← NULL!
  "updatedAt": null            // ← NULL!
}
```
**Важно:** ВСЕ даты = NULL в JSON schema!

### DTO (TestRunDTO)
```java
private UUID id;
private UUID projectId;
private String displayId;
private String name;
private String environment;
private String buildVersion;
private String description;
private String status;
private int passed, failed, skipped, untested;
private List<String> caseIds;
private Map<String, String> stepStatuses;
private LocalDateTime startedAt;    // ❌ type (Schema: null)
private LocalDateTime completedAt;  // ❌ type (Schema: null)
private LocalDateTime createdAt;    // ❌ type (Schema: null)
private LocalDateTime updatedAt;    // ❌ type (Schema: null)
```

### Service (TestRunService.createTestRun) ← ПРОБЛЕМА!
```java
public TestRunDTO createTestRun(TestRunDTO testRun) {
    LocalDateTime now = LocalDateTime.now();
    testRun.setCreatedAt(now);       // ❌ sets LocalDateTime, but Schema expects null
    testRun.setStartedAt(now);       // ❌ sets LocalDateTime, but Schema expects null
    // ...
}
```

### 🔴 ВЕРДИКТ: CRITICAL MISMATCH
1. **JSON schema явно говорит:** все даты должны быть **null**
2. **Service делает:** устанавливает **LocalDateTime**
3. **Результат:** Jackson сериализует object вместо null/string → Cyoda validation fails

### РЕШЕНИЕ:
```
ПРАВИЛЬНОЕ (следовать JSON Schema):
- DTO изменить: LocalDateTime → String (или оставить как есть для null)
- Service НЕ МЕНЯТЬ (оставить даты null)
- JSON Schema оставить как есть (null)

ИЛИ если хочется инициализировать даты:
- Обновить JSON Schema: null → "2026-04-14T00:00:00Z"
- DTO: LocalDateTime → String
- Service: инициализировать String с Instant.now().toString()
```

---

## 5. TESTRUNCASE

### JSON Schema (testruncase.json)
```json
{
  "id": "550e8400...",
  "testRunId": "550e8400...",
  "testCaseId": "550e8400...",
  "projectId": "550e8400...",
  "status": "UNTESTED",
  "bugUrl": null,
  "startedAt": null,          // ← NULL
  "completedAt": null         // ← NULL
}
```
**Поля:** id, testRunId, testCaseId, projectId, status, bugUrl, startedAt (null), completedAt (null)

### DTO (TestRunCaseDTO)
```java
private UUID id;
private UUID testRunId;
private UUID testCaseId;
private UUID projectId;
private String status;
private String bugUrl;
private LocalDateTime startedAt;    // ❌ type (Schema: null)
private LocalDateTime completedAt;  // ❌ type (Schema: null)
```

### Service (TestRunCaseService) — NEED TO CHECK

### ⚠️ ВЕРДИКТ: TYPE MISMATCH
- Даты должны быть null (JSON schema)
- DTO использует LocalDateTime
- Service вероятно не инициализирует (остаются null)

### РЕКОМЕНДАЦИЯ:
- DTO: LocalDateTime → String (для консистентности) или оставить как есть

---

## 6. TESTRUNSTEP

### JSON Schema (testrunstep.json)
```json
{
  "id": "550e8400...",
  "testRunCaseId": "550e8400...",
  "testStepId": "550e8400...",
  "status": "UNTESTED",
  "actualResult": null,
  "startedAt": null,          // ← NULL
  "completedAt": null         // ← NULL
}
```

### DTO (TestRunStepDTO)
```java
private UUID id;
private UUID testRunCaseId;
private UUID testStepId;
private String status;
private String actualResult;
private LocalDateTime startedAt;    // ❌ type (Schema: null)
private LocalDateTime completedAt;  // ❌ type (Schema: null)
```

### ✓ ВЕРДИКТ: ACCEPTABLE
- JSON schema null → DTO LocalDateTime (обе приводят к null значениям)
- Не критично, но неконсистентно

---

## 7. TESTSTEP

### JSON Schema (teststep.json)
```json
{
  "id": "550e8400...",
  "testCaseId": "550e8400...",
  "stepNumber": 1,
  "action": "Click on the login button",
  "expectedResult": "Login form is displayed",
  "deleted": false
  // ❌ НЕТ НИКАКИХ ДАТ
}
```

### DTO (TestStepDTO)
```java
private UUID id;
private UUID testCaseId;
private Integer stepNumber;
private String action;
private String expectedResult;
private boolean deleted;
// ✓ НЕТ ДАТ
```

### ✅ ВЕРДИКТ: PERFECT MATCH
- JSON schema ✓ соответствует DTO (оба без дат)

---

## 8. DEFECT

### JSON Schema (defect.json)
```json
{
  "id": "550e8400...",
  "projectId": "550e8400...",
  "displayId": "DEF-01",
  "title": "Login button unresponsive...",
  "description": "The login button does not respond...",
  "severity": "Critical",
  "link": "https://jira.example.com/browse/TMS-42",
  "status": "Open",
  "source": "TC-01",
  "testRunId": "550e8400...",
  "testRunCaseId": "550e8400...",
  "createdAt": "2024-02-15T10:00:00",      // ← STRING (NO Z)
  "updatedAt": "2024-02-15T10:00:00"       // ← STRING (NO Z)
}
```

### DTO (DefectDTO)
```java
private UUID id;
private UUID projectId;
private String displayId;
private String title;
private String description;
private String severity;
private String link;
private String status;
private String source;
private UUID testRunId;
private UUID testRunCaseId;
private LocalDateTime createdAt;    // ❌ type (Schema: String)
private LocalDateTime updatedAt;    // ❌ type (Schema: String)
```

### ⚠️ ВЕРДИКТ: TYPE MISMATCH
- Структура ✓ совпадает
- Типы дат ❌ JSON: String, DTO: LocalDateTime

### РЕКОМЕНДАЦИЯ:
- DTO: LocalDateTime → String

---

## 9. ATTACHMENT

### JSON Schema (attachment.json)
```json
{
  "id": "550e8400...",
  "projectId": "550e8400...",
  "caseId": "550e8400...",
  "defectId": null,
  "fileName": "checkout_screenshot.png",
  "fileType": "image/png",
  "fileSize": 256000,
  "uploadedAt": "2024-02-15T09:45:30",     // ← STRING
  "messageId": "550e8400...",
  "content": null
}
```

### DTO (AttachmentDTO)
```java
private UUID id;
private UUID projectId;
private UUID caseId;
private UUID defectId;
private String fileName;
private String fileType;
private long fileSize;
private LocalDateTime uploadedAt;   // ❌ type (Schema: String)
private UUID messageId;
private String content;
```

### ⚠️ ВЕРДИКТ: TYPE MISMATCH (1 поле)
- Структура ✓ совпадает
- Тип uploadedAt ❌ JSON: String, DTO: LocalDateTime

### РЕКОМЕНДАЦИЯ:
- DTO: LocalDateTime → String

---

## 10. REPORT

### JSON Schema (report.json)
```json
{
  "id": "550e8400...",
  "projectId": "550e8400...",
  "displayId": "REP-1",
  "name": "Weekly Regression - Week 12",
  "type": "Regression",
  "description": "Full regression cycle...",
  "createdBy": "admin",
  "dateFrom": "",
  "dateTo": "",
  "selectedRuns": [],
  "sectionExecutiveSummary": true,
  "sectionSuiteAnalytics": true,
  "sectionDefectTable": true,
  "sectionEnvironmentInfo": true,
  "createdAt": "2026-03-20T10:00:00",      // ← STRING
  "updatedAt": "2026-03-20T10:00:00"       // ← STRING
}
```

### DTO (ReportDTO)
```java
private UUID id;
private UUID projectId;
private String displayId;
private String name;
private String type;
private String description;
private String createdBy;
private String dateFrom;
private String dateTo;
private List<String> selectedRuns;
private boolean sectionExecutiveSummary;
private boolean sectionSuiteAnalytics;
private boolean sectionDefectTable;
private boolean sectionEnvironmentInfo;
private LocalDateTime createdAt;    // ❌ type (Schema: String)
private LocalDateTime updatedAt;    // ❌ type (Schema: String)
```

### ⚠️ ВЕРДИКТ: TYPE MISMATCH
- Структура ✓ совпадает (почти)
- Типы дат ❌ JSON: String, DTO: LocalDateTime

### РЕКОМЕНДАЦИЯ:
- DTO: LocalDateTime → String

---

## 11. PROJECTCOUNTER

### JSON Schema (projectcounter.json)
```json
{
  "id": "550e8400...",
  "projectId": "550e8400...",
  "nextId": 1,
  "nextRunId": 1,
  "nextDefectId": 1,
  "nextReportId": 1
}
```

### DTO (ProjectCounterDTO)
```bash
# NEED TO CHECK
```

---

## ИТОГОВАЯ ТАБЛИЦА: DTO ↔ JSON Schema Мismatch

| Entity | DTO | JSON Schema | Типы дат | Статус | Требует fix |
|--------|-----|-------------|----------|--------|------------|
| **Project** | String ✓ | String ✓ | String → String ✓ | ✅ PERFECT | ❌ НЕТ |
| **Suite** | LocalDateTime ❌ | String ✓ | LocalDateTime → String ❌ | ⚠️ MISMATCH | ✅ ДА |
| **TestCase** | LocalDateTime ❌ | String ✓ | LocalDateTime → String ❌ | ⚠️ MISMATCH | ✅ ДА |
| **TestRun** | LocalDateTime ❌ | null ✓ | LocalDateTime → null ❌ | 🔴 CRITICAL | ✅ ДА |
| **TestRunCase** | LocalDateTime ❌ | null ✓ | LocalDateTime → null ❌ | ⚠️ MISMATCH | ✅ ДА |
| **TestRunStep** | LocalDateTime ❌ | null ✓ | LocalDateTime → null ❌ | ⚠️ MISMATCH | ✅ ДА |
| **TestStep** | NONE ✓ | NONE ✓ | NONE → NONE ✓ | ✅ PERFECT | ❌ НЕТ |
| **Defect** | LocalDateTime ❌ | String ✓ | LocalDateTime → String ❌ | ⚠️ MISMATCH | ✅ ДА |
| **Attachment** | LocalDateTime ❌ | String ✓ | LocalDateTime → String ❌ | ⚠️ MISMATCH | ✅ ДА |
| **Report** | LocalDateTime ❌ | String ✓ | LocalDateTime → String ❌ | ⚠️ MISMATCH | ✅ ДА |

---

## ДЕ ФАКТО: Какие Services инициализируют даты

```bash
# Grep результаты:
```

| Service | Инициализирует? | Как | Тип | Проблема |
|---------|---|---|---|---|
| **ProjectService** | ✓ ДА | `Instant.now().toString()` | String ✓ | ❌ НЕТ |
| **TestRunService** | ✓ ДА | `LocalDateTime.now()` | LocalDateTime ❌ | ✅ ЕСТЬ - блокер! |
| **AttachmentService** | ✓ ДА | `LocalDateTime.now()` | LocalDateTime ❌ | ✅ ЕСТЬ |
| **DefectService** | ✓ ДА | `LocalDateTime.now()` | LocalDateTime ❌ | ✅ ЕСТЬ |
| **ReportService** | ✓ ДА | `LocalDateTime.now()` | LocalDateTime ❌ | ✅ ЕСТЬ |
| **SuiteService** | ❌ НЕТ | (null) | null ✓ | ❌ НЕТ |
| **TestCaseService** | ❌ НЕТ | (null) | null ✓ | ❌ НЕТ |
| **TestRunCaseService** | ? | ? | ? | ? |
| **TestRunStepService** | ? | ? | ? | ? |

### Вывод:
- ✅ ProjectService правильно использует String
- ❌ 4 Services неправильно используют LocalDateTime:
  1. TestRunService ← **ГЛАВНЫЙ БЛОКЕР**
  2. AttachmentService
  3. DefectService
  4. ReportService

---

## ФИНАЛЬНЫЕ РЕКОМЕНДАЦИИ

### Вариант 1: СЛЕДОВАТЬ JSON SCHEMAS (консервативный)

**Что менять:**

1. **DTO изменить (ALL except Project, TestStep):**
   ```java
   // Suite, TestCase, TestRun, TestRunCase, TestRunStep, Defect, Attachment, Report
   // private LocalDateTime createdAt; → private String createdAt;
   // private LocalDateTime updatedAt; → private String updatedAt;
   // private LocalDateTime uploadedAt; → private String uploadedAt;
   // и т.д. для всех дат
   ```

2. **Services НЕ МЕНЯТЬ для Suite/TestCase/TestRun (оставить null):**
   ```java
   // SuiteService: createdAt остаются null ✓
   // TestCaseService: createdAt остаются null ✓
   // TestRunService: DELETE setCreatedAt/setStartedAt ✓
   ```

3. **JSON Schema НЕ МЕНЯТЬ** (все правильно)

**Что это дает:**
- ✅ Полное соответствие DTO ↔ JSON
- ✅ Cyoda FSM validation passes
- ✅ Даты подставляются из метаданных Cyoda автоматически
- ❌ Нужно менять 8 DTO

---

### Вариант 2: УНИФИЦИРОВАТЬ ВСЁ НА STRING (современный)

**Что менять:**

1. **DTO изменить (ALL except Project, TestStep):**
   ```java
   private String createdAt;
   private String updatedAt;
   // и т.д. для всех дат
   ```

2. **JSON Schema изменить (Suite, TestCase, TestRun, TestRunCase, TestRunStep, Defect, Attachment, Report):**
   ```json
   // null → "2026-04-14T00:00:00Z"
   ```

3. **Services изменить (Suite, TestCase, TestRun, Defect, Report):**
   ```java
   String now = Instant.now().toString();
   suite.setCreatedAt(now);
   suite.setUpdatedAt(now);
   ```

**Что это дает:**
- ✅ Единообразный формат (как ProjectDTO/ProjectService)
- ✅ Даты явно инициализированы
- ✅ Cyoda хранит в JSON
- ❌ Нужно менять 8 DTO + 8 JSON files + 5+ Services

---

### Вариант 3: ГИБРИДНЫЙ (рекомендуется)

**НЕМЕДЛЕННО (сегодня, 30 минут):**
1. TestRunDTO: удалить setCreatedAt/setStartedAt из Service
2. Все DTO: изменить LocalDateTime → String для дат (механически)

**ПОТОМ (рефакторинг, когда время):**
1. Services: инициализировать String даты
2. JSON schemas: добавить значения вместо null

---

## СЛЕДУЮЩИЙ ШАГ

**Какой вариант выбираете?**
- **Вариант 1** (следовать JSON) - быстро, консервативно
- **Вариант 2** (унифицировать) - полный рефакторинг
- **Вариант 3** (гибридный) - сегодня fix, потом улучшение

**Мой совет:** Вариант 3 - сегодня убираем блокер (Вариант 1 для TestRun), потом планомерно переходим на Вариант 2.

