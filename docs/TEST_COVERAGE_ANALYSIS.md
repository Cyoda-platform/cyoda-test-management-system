# Test Coverage Analysis — Cyoda TMS

**Дата анализа:** 2026-04-30  
**Анализируемые слои:** Backend (Java/Kotlin), E2E (Cucumber/Gherkin), Frontend (TypeScript/Vitest)

---

## 1. Итоговая таблица покрытия

| Слой | Категория | Файлов | С тестами | Покрытие | Статус |
|------|-----------|--------|-----------|----------|--------|
| Backend | Контроллеры | 17 | 4 | **24%** | Критическая зона |
| Backend | Сервисы | 13 | 9 | **69%** | Удовлетворительно |
| Backend | Процессоры | 6 | 1 | **17%** | Критическая зона |
| Backend | Критерии | 3 | 2 | **67%** | Приемлемо |
| Backend | Репозитории | 8 | 0 | **0%** | Ожидаемо (Spring Data) |
| Backend | Auth-слой | 3 | 0 | **0%** | Требует внимания |
| E2E | Gherkin-сценарии | 3 feature-файла | 29 сценариев | — | Начальный уровень |
| Frontend | Хуки | 6 | 1 | **17%** | Критическая зона |
| Frontend | Страницы | 13 | 0 | **0%** | Критическая зона |
| Frontend | Компоненты (custom) | 20 | 0 | **0%** | Критическая зона |
| Frontend | Утилиты (lib/) | 6 | 2 | **33%** | Частично |

---

## 2. Backend — подробный анализ

### 2.1 Контроллеры

**Протестированы (4/17):**

| Файл | Тест | Кол-во тестов | Покрытые сценарии |
|------|------|---------------|-------------------|
| `AuthController.java` | `AuthControllerTest.java` | ~18 | Login, logout, назначение ролей |
| `ProjectController.java` | `ProjectControllerTest.java` | ~28 | CRUD, пагинация, поиск, валидация |
| `ProjectController.java` | `ProjectRoleAccessControlTest.java` | ~16 | RBAC: admin vs tester |
| `SuiteController.java` | `SuiteControllerTest.java` | ~6 | Основные CRUD-операции |
| `TestCaseController.java` | `TestCaseControllerTest.java` | ~2 | Минимальный smoke |

**Без тестов (13/17):**

| Файл | Риск | Причина высокого риска |
|------|------|------------------------|
| `AttachmentController.java` | Высокий | Загрузка файлов — сложная логика, multipart |
| `DefectController.java` | Высокий | CRUD дефектов, линковка к test run |
| `ExportController.java` | Высокий | Экспорт данных — нет валидации форматов |
| `GlobalSearchController.java` | Средний | Поиск по нескольким сущностям |
| `MessageUploadController.java` | Высокий | Загрузка сообщений — нет контракта |
| `ReportController.java` | Высокий | Генерация отчётов — критически важно |
| `RepositoryController.java` | Средний | Репозитории тест-кейсов |
| `SearchController.java` | Средний | Поиск (дублирует GlobalSearch?) |
| `TestCaseDirectController.java` | Высокий | Прямые операции с тест-кейсами |
| `TestRunCaseController.java` | Высокий | Управление статусами запуска |
| `TestRunController.java` | Высокий | Основная логика test run |
| `TestRunStepController.java` | Высокий | Пошаговое выполнение |
| `ValidationExceptionHandler.java` | Средний | Глобальная обработка ошибок |

> **Ключевой риск:** Контроллеры для TestRun, TestRunCase, TestRunStep — основной функционал приложения — полностью без unit-тестов. Ошибки API-контракта (статус-коды, форматы ответов) обнаруживаются только на E2E-прогоне с живым Cyoda.

### 2.2 Сервисы

**Протестированы (9/13 прямо, 3 косвенно):**

| Файл | Тест-файлы | Дополнительно |
|------|-----------|---------------|
| `AttachmentService.java` | `AttachmentServiceTest.java`, `EvidenceUploadTest.java` | — |
| `BugLinkService.java` | `DefectLinkingTest.java` | — |
| `ProjectService.java` | `ProjectServiceTest.java` | — |
| `SuiteService.java` | `SuiteServiceTest.java`, `SuiteServiceCacheTest.java` | — |
| `TestCaseService.java` | `TestCaseServiceTest.java`, `TestCaseServiceCacheTest.java` | — |
| `TestRunCaseService.java` | `TestRunCaseServiceTest.java`, `TestRunCaseServiceCacheTest.java`, `CaseStatusCalculationTest.java` | — |
| `TestRunService.java` | `TestRunServiceTest.java`, `TestRunServiceCacheTest.java`, `TestRunServiceRaceConditionTest.java`, `TestRunInitializationTest.java`, `RunLockingTest.java` | Race conditions, cache |
| `TestRunStepService.java` | `StepStatusTest.java` | — |
| `DefectService.java` | `DefectLinkingTest.java` (частично) | Основное покрытие через E2E |

**Без прямых unit-тестов (4/13):**

| Файл | Риск | Примечание |
|------|------|-----------|
| `ProjectCounterService.java` | Средний | Счётчики проектов — нет проверки атомарности |
| `ReportService.java` | Высокий | Генерация отчётов, агрегация — только фронтенд-тесты |
| `RepositoryService.java` | Средний | Работает через E2E |
| `SearchService.java` | Средний | Работает через E2E |

**Кросс-срезные тест-файлы (поведенческие, не привязаны к одному сервису):**

| Тест | Что проверяет |
|------|--------------|
| `AuditTrailTest.java` | Ведение журнала аудита (~8 тестов) |
| `BulkOperationsServiceTest.java` | Массовые операции (~6 тестов) |
| `EdgeCasesTest.java` | Граничные случаи (~14 тестов) |
| `MetricsTest.java` | Метрики (~16 тестов) |
| `SoftDeleteFilteringTest.java` | Мягкое удаление (~4 теста) |

### 2.3 Процессоры (Cyoda FSM)

**Состояние: критически недостаточное покрытие (1/6)**

| Файл | Тест | Примечание |
|------|------|-----------|
| `SnapshotProcessor.java` | `SnapshotProcessorTest.java` (~8 тестов) | Единственный покрытый |
| `AtomicFailureProcessor.java` | **Нет** | Обрабатывает провальные переходы FSM |
| `BugLinkProcessor.java` | **Нет** | Линковка дефектов через workflow |
| `EdgeMessageProcessor.java` | **Нет** | Обработка сообщений на переходах |
| `MetricsAggregatorProcessor.java` | **Нет** | Агрегация метрик |
| `TestRunCompleteProcessor.java` | **Нет** | Финализация test run — высокий риск |

> **Важно:** Процессоры в Cyoda FSM нельзя тестировать через `EntityService` (ограничение архитектуры). Тестирование должно быть через `ProcessorSerializer`/mock-контекст. Паттерн есть в `ExampleEntityProcessorTest.java` — 20 тестов, которые демонстрируют правильный подход.

### 2.4 Критерии (Cyoda FSM)

| Файл | Тест | Примечание |
|------|------|-----------|
| `RequireAdminRoleCriterion.java` | `RequireAdminRoleCriterionTest.java` | ✓ |
| `RequireTesterOrAdminRoleCriterion.java` | `RequireTesterOrAdminRoleCriterionTest.java` | ✓ |
| `SystemActionOnlyCriterion.java` | **Нет** | Ограничивает системные действия |

### 2.5 Auth-слой приложения

| Файл | Тест | Примечание |
|------|------|-----------|
| `AuthService.java` | Косвенно через `AuthControllerTest` | Нет прямого unit-теста |
| `AuthorizationFilter.java` | **Нет** | Spring Security-фильтр — критичен |
| `JwtTokenProvider.java` | **Нет** | Выдача и валидация токенов |

> **Примечание:** `common/auth/` содержит 11 тест-файлов (AesGcm, AuthClaims, OBO-токены и т.д.) — это фреймворковые тесты. Прикладной auth-слой в `application/auth/` тестируется только косвенно.

### 2.6 Framework-тесты (`common/`)

Всего 30+ тест-файлов покрывают framework-код (не трогать согласно CLAUDE.md):

- `common/auth/` — 11 файлов (шифрование, OBO-токены, парсинг claims)
- `common/serializer/` — 5 файлов (EvaluationChain, EvaluationOutcome, Jackson-сериализатор)
- `common/grpc/` — 3 файла (CloudEvent, интерсептор авторизации)
- `common/observability/` — 3 файла (gRPC-наблюдаемость)
- `common/service/` — 1 файл (EntityServiceImpl)
- Прочие — 7 файлов

---

## 3. E2E / Cucumber-тесты

**Конфигурация:** требует живого Cyoda-инстанса  
**Запуск:** `./gradlew :apps:backend:cucumberTest`  
**Jacoco:** настроен, но без пороговых значений покрытия

### 3.1 Существующие сценарии

| Feature-файл | Сценариев | Покрытые области |
|-------------|-----------|-----------------|
| `health-check.feature` | 3 | Доступность, статус UP, 404 |
| `project-crud.feature` | 16 | Auth, CRUD, RBAC, валидация |
| `defect-crud.feature` | 10 | CRUD дефектов, пагинация, линковка к test run |
| **Итого** | **29** | |

### 3.2 Отсутствующие E2E-сценарии (высокий приоритет)

| Область | Что нужно добавить |
|---------|--------------------|
| Suite (наборы тестов) | CRUD, вложенность, переупорядочивание |
| TestCase | CRUD, batch import, шаги |
| TestRun | Создание, запуск, завершение, статусы |
| TestRunCase | Изменение статуса кейса, добавление дефекта |
| TestRunStep | Прохождение шагов, загрузка evidence |
| Report | Создание отчёта, агрегация |
| Attachment | Загрузка, скачивание, удаление |
| Search | Полнотекстовый поиск |

---

## 4. Frontend — подробный анализ

**Стек тестирования:** Vitest + Testing Library  
**Запуск:** `cd apps/frontend && npm run test`

### 4.1 Существующие тесты (3 файла, 49 тест-кейсов)

| Файл | Тест | Кол-во | Что проверяет |
|------|------|--------|--------------|
| `useLocalStorage.ts` | `useLocalStorage.test.ts` | 7 | Хранение в localStorage, fallback |
| `reportAggregation.ts` | `reportAggregation.test.ts` | 11 | Дедупликация, группировка по suite |
| `reportSnapshot.ts` | `reportSnapshot.test.ts` | 29 | Snapshot-утилиты для отчётов |

### 4.2 Не покрыто

**Хуки (5/6 без тестов):**

| Файл | Риск | Рекомендация |
|------|------|-------------|
| `useApi.ts` | Высокий | Центральный HTTP-клиент — ошибки, retry, auth |
| `useAuthenticatedImageUrl.ts` | Средний | Auth-заголовки для изображений |
| `useVirtualList.ts` | Средний | Виртуализация списков — граничные случаи |
| `use-mobile.tsx` | Низкий | Простой media-query |
| `use-toast.ts` | Низкий | UI-хелпер |

**Утилиты (4/6 без тестов):**

| Файл | Риск | Рекомендация |
|------|------|-------------|
| `lib/api.ts` | Высокий | API-типы и конфигурация запросов |
| `lib/exportImport.ts` | Высокий | Экспорт/импорт данных — критична корректность |
| `lib/utils.ts` | Средний | Общие утилиты |
| `lib/localTypes.ts` | Низкий | Только типы |

**Страницы (13/13 без тестов):**

Для страниц рекомендуются интеграционные тесты с Playwright E2E или компонентные тесты с Testing Library. Приоритет:

| Страница | Приоритет | Причина |
|----------|-----------|---------|
| `RunExecution.tsx` | Критический | Сложная логика запуска тестов |
| `CreateTestRun.tsx` | Высокий | Форма создания с валидацией |
| `ReportDetail.tsx` | Высокий | Агрегация и отображение данных |
| `Projects.tsx` | Высокий | Центральная страница |
| `Defects.tsx` | Средний | Управление дефектами |
| `CreateReport.tsx` | Средний | Форма отчёта |

---

## 5. Сильные стороны

1. **Сервисный слой хорошо покрыт** — TestRunService имеет 5 специализированных тест-файлов включая race condition и кэш-тесты. Это правильный паттерн.
2. **Кросс-срезные поведенческие тесты** — `AuditTrailTest`, `EdgeCasesTest`, `MetricsTest`, `RunLockingTest` проверяют системное поведение, а не только отдельные методы.
3. **Пример-паттерны** — `com/example/tests/` содержит 5 образцовых тест-файлов (106 тестов) с правильными паттернами для контроллеров, процессоров, критериев и сущностей.
4. **Безопасность фреймворка** — 30+ тестов для auth-криптографии, OBO-токенов, gRPC-интерсепторов.
5. **Специализированные тест-категории** — отдельные тесты для кэша, race condition, soft delete, edge cases.

---

## 6. Рекомендации по приоритетам

### Приоритет 1 — Критический (сделать в первую очередь)

1. **Unit-тесты для процессоров** (5 непокрытых):
   - Паттерн: `ExampleEntityProcessorTest.java`
   - Особенно: `TestRunCompleteProcessor`, `BugLinkProcessor`, `AtomicFailureProcessor`

2. **Контроллеры TestRun-домена** (0 тестов):
   - `TestRunController`, `TestRunCaseController`, `TestRunStepController`
   - Паттерн: `ExampleEntityControllerTest.java` (36 тестов, MockMvc)

3. **E2E: TestRun workflow** — сквозной сценарий создания и выполнения test run.

### Приоритет 2 — Высокий

4. **`TestRunCompleteProcessor`** — завершение запуска: ошибка здесь необнаружима без E2E.
5. **`AttachmentController`** — сложный multipart, нет защиты типов файлов.
6. **`ExportController`** и **`ReportController`** — бизнес-ценные операции.
7. **Frontend `useApi.ts`** — все HTTP-запросы проходят через него.
8. **Frontend `exportImport.ts`** — данные пользователей; ошибки необратимы.

### Приоритет 3 — Средний

9. **`SystemActionOnlyCriterion`** — последний непокрытый критерий.
10. **`ProjectCounterService`** — атомарность счётчиков.
11. **`ReportService`** — backend-тест (существующие — только frontend).
12. **Frontend `useApi`-хук** — компонентные тесты с mock-fetch.

### Приоритет 4 — Желательно

13. **`AuthorizationFilter` и `JwtTokenProvider`** — auth на уровне приложения.
14. **Frontend-компоненты** — `RunExecution.tsx`, `CreateTestRun.tsx` через Playwright.
15. **Jacoco-пороги** — добавить минимальный порог покрытия (рекомендуется 70% по instractions для сервисов).

---

## 7. Метрики для отслеживания

| Метрика | Сейчас | Цель |
|---------|--------|------|
| Backend application — контроллеры | 24% | 70% |
| Backend application — сервисы | 69% | 85% |
| Backend application — процессоры | 17% | 80% |
| E2E Gherkin-сценарии | 29 | 60+ |
| Frontend утилиты (lib/) | 33% | 80% |
| Frontend хуки | 17% | 60% |
| Jacoco branch coverage (E2E) | не задан порог | ≥ 60% |

---

## 8. Команды для запуска

```bash
# Unit-тесты (без Cyoda)
./gradlew :apps:backend:test

# E2E / Cucumber (требует живой Cyoda)
./gradlew :apps:backend:cucumberTest

# Jacoco-отчёт после E2E
./gradlew :apps:backend:jacocoTestReport
# Результат: apps/backend/build/test-report/

# Frontend-тесты
cd apps/frontend && npm run test

# Все тесты
./gradlew :apps:backend:build && cd apps/frontend && npm run test
```

---

## 9. Файлы тестов

### Backend unit-тесты (`apps/backend/src/test/java/`)

```
com/java_template/application/
  controller/
    AuthControllerTest.java
    ProjectControllerTest.java
    ProjectRoleAccessControlTest.java
    SuiteControllerTest.java
    TestCaseControllerTest.java
  service/
    AttachmentServiceTest.java      AuditTrailTest.java
    BulkOperationsServiceTest.java  CaseStatusCalculationTest.java
    DefectLinkingTest.java          EdgeCasesTest.java
    EvidenceUploadTest.java         MetricsTest.java
    ProjectServiceTest.java         RunLockingTest.java
    SoftDeleteFilteringTest.java    StepStatusTest.java
    SuiteServiceCacheTest.java      SuiteServiceTest.java
    TestCaseServiceCacheTest.java   TestCaseServiceTest.java
    TestRunCaseServiceCacheTest.java TestRunCaseServiceTest.java
    TestRunInitializationTest.java  TestRunServiceCacheTest.java
    TestRunServiceRaceConditionTest.java TestRunServiceTest.java
  criterion/
    RequireAdminRoleCriterionTest.java
    RequireTesterOrAdminRoleCriterionTest.java
  processor/
    SnapshotProcessorTest.java
  serializer/
    ProcessingChainTest.java
    ResponseBuilderTest.java

com/example/tests/  (шаблонные примеры)
  ExampleEntityControllerTest.java  ExampleEntityCriterionTest.java
  ExampleEntityProcessorTest.java   ExampleEntityTest.java
  WorkflowConfigurationMarshallingTest.java

e2e/
  GherkinE2eTest.java  E2eTestConfig.java
  steps/DefectApiSteps.java  steps/HealthCheckSteps.java
  steps/ProjectApiSteps.java  support/TmsHttpClient.java
```

### E2E feature-файлы (`apps/backend/src/test/resources/features/`)

```
health-check.feature    (3 сценария)
project-crud.feature   (16 сценариев)
defect-crud.feature    (10 сценариев)
```

### Frontend тесты (`apps/frontend/src/`)

```
hooks/useLocalStorage.test.ts
test/example.test.ts
test/reportAggregation.test.ts
test/reportSnapshot.test.ts
```
