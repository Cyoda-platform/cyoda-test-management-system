# Анализ рисков: рефакторинг caseIds (коммиты b5a1c2d8 → 8e3bfc38)

**Дата:** 2026-05-27  
**Ветка:** main  
**Диапазон коммитов:** HEAD~5..HEAD  
**Статус тестов:** BUILD SUCCESSFUL (22 tasks, все unit-тесты прошли)

---

## 1. Что было изменено

В пяти последних коммитах решалась одна проблема: Cyoda имеет лимит в 150 полей на модель при подписке, и массив `caseIds: string[]` съедал по одному слоту на каждый элемент. Решение — хранить `caseIds` в Cyoda как одну JSON-строку `"[\"uuid1\",\"uuid2\"]"`, а на HTTP-границе отдавать как массив.

### Затронутые файлы

| Файл | Суть изменения |
|---|---|
| `TestRunDTO.java` | Поле `caseIds: List<String>` → `String`; добавлены `CaseIdsSerializer` и `CaseIdsDeserializer` |
| `TestRunController.java` | `@JsonView(Views.Http.class)` добавлен на все 7 эндпоинтов |
| `TestRunService.java` | Тип `savedCaseIds` и условия `.isEmpty()` → `.isBlank()` |
| `SnapshotProcessor.java` | `parseCaseIds` переписан: сначала пробует строку, потом legacy-массив |
| `DemoSeederService.java` | `setCaseIds` теперь принимает `objectMapper.writeValueAsString(list)` |
| `application.yml` | Добавлено `spring.jackson.mapper.default-view-inclusion: true` |
| `testrun.json` (schema) | Пример caseIds изменён с массива на строку |
| `api.ts` (frontend) | `testCasesApi.list` добавил `?size=1000` |

---

## 2. Результаты тестов

```
./gradlew :apps:backend:test
BUILD SUCCESSFUL in 48s
22 actionable tasks: 9 executed, 13 up-to-date
```

Все unit-тесты прошли. E2E/Cucumber тесты не запускались (требуют живой Cyoda-инстанс).

---

## 3. Найденные риски (ранжированы по серьёзности)

### 🔴 КРИТИЧЕСКИЙ — Незавершённое создание TestRun при сбое обновления

**Файл:** `TestRunService.java` ~строка 116–130  
**Вердикт:** CONFIRMED

**Суть:** `createTestRun` работает в два шага: (1) `entityService.create()` создаёт сущность с `caseIds=null`, (2) `entityService.update()` восстанавливает `caseIds`. Если второй вызов упадёт (таймаут Cyoda, сетевая ошибка), сущность навсегда остаётся с `caseIds=null`. Нет try/catch, нет компенсирующего удаления, нет отката.

**Сценарий сбоя:**  
Транзиентный таймаут на `update()` → `createTestRun` бросает RuntimeException → клиент видит 500 и повторяет запрос → в Cyoda появляется дубль-сущность. Исходная сущность навсегда без `caseIds`; `SnapshotProcessor` создаст нулевой снепшот для неё.

**Текущий тест:** Отсутствует. `TestRunServiceRaceConditionTest` не покрывает частичный сбой при создании.

---

### 🔴 КРИТИЧЕСКИЙ — Тихая потеря всех caseIds при повреждённой строке

**Файл:** `SnapshotProcessor.java` ~строка 139–156  
**Вердикт:** CONFIRMED

**Суть:** `parseCaseIds` при ошибке парсинга текстового узла (поле хранится как строка, но JSON в ней сломан) логирует предупреждение и переходит к проверке `node.isArray()`. Но если узел текстовый — он никогда не будет массивом. Метод возвращает `List.of()` без единой ошибки. `SnapshotProcessor` создаёт снепшот TestRun без тест-кейсов.

**Сценарий сбоя:**  
Частичная запись (Cyoda таймаут в середине write) → поле `caseIds` в хранилище содержит обрезанный JSON → `readValue` бросает, предупреждение в лог, возврат пустого списка → весь TestRun-снепшот создаётся с нулём шагов, тихо и без ошибки.

**Текущий тест:** Тест для этого пути есть в `SnapshotProcessorTest`, но — см. следующий пункт.

---

### 🟠 ВЫСОКИЙ — Нулевое покрытие нового формата хранения в SnapshotProcessor

**Файл:** `SnapshotProcessorTest.java` ~строка 83, 135, 207, 244  
**Вердикт:** CONFIRMED

**Суть:** Все существующие юнит-тесты `SnapshotProcessor` задают `caseIds` через `runJson.putArray("caseIds").add(...)` — это legacy-формат JSON-массива. Новый путь (`node.isTextual()` → `objectMapper.readValue(...)`) не покрыт ни одним тестом. Ветка, несущая весь production-трафик после рефакторинга, работает «на доверии».

**Риск:** Если в новом пути есть баг (смещение кавычек, символ экранирования, нюанс ObjectMapper) — тесты его не поймают. Узнаем только в проде.

---

### 🟠 ВЫСОКИЙ — Клиент не может очистить caseIds через PUT

**Файл:** `TestRunDTO.java` строка 127, `TestRunService.java` строка 203  
**Вердикт:** CONFIRMED

**Суть:** `CaseIdsDeserializer` возвращает `null` если клиент прислал `caseIds: []`. В `updateTestRun` это триггерит `needsCaseIdsMerge = true`, и сервис восстанавливает старые значения. Намеренная очистка `caseIds` через HTTP API невозможна.

**Сценарий сбоя:**  
Фронтенд отправляет `PUT /runs/{id}` с `caseIds: []` чтобы сбросить выборку — старые ID молча восстанавливаются, фронтенд думает что очистил, но данные не изменились.

**Текущий тест:** Отсутствует. `TestRunControllerTest` не проверяет PUT с пустым массивом.

---

### 🟡 СРЕДНИЙ — `default-view-inclusion: true` — скрытая зависимость без теста

**Файл:** `application.yml` строка ~14  
**Вердикт:** CONFIRMED

**Суть:** Настройка `spring.jackson.mapper.default-view-inclusion: true` необходима для корректной работы всех `@JsonView(Http.class)` эндпоинтов: без неё все поля без аннотации `@JsonView` (id, name, status и т.д.) исчезают из HTTP-ответов. Это глобальная настройка, влияющая на весь контроллерный слой, и она не задокументирована как критическая зависимость. Ни один тест не проверяет наличие полей `id`/`name` в ответе эндпоинта с `@JsonView`.

**Риск:** Случайное удаление или переопределение этой строки в другом профиле разломает все TestRun-эндпоинты, при этом тесты не упадут.

---

### 🟡 СРЕДНИЙ — Неявный контракт с ObjectMapper в EntityService

**Файл:** `TestRunDTO.java` (CaseIdsSerializer), `common/` EntityService  
**Вердикт:** PLAUSIBLE

**Суть:** `CaseIdsSerializer` различает путь HTTP (пишет raw JSON-массив) и путь Cyoda (пишет строку) через `provider.getActiveView() == null`. Корректность записи в Cyoda зависит от того, что `EntityService.valueToTree()` никогда не активирует Jackson view. Сейчас это так — но это неявный контракт с `common/` (framework-код), который нельзя изменять согласно CLAUDE.md, но который могут обновить авторы фреймворка.

**Сценарий сбоя:**  
Обновление `common/` добавляет view-aware сериализацию → все записи TestRun в Cyoda начинают хранить `caseIds` как JSON-массив вместо строки → Cyoda нарушает schema validation на каждом save, всё ломается.

---

## 4. Сводная таблица рисков

| # | Серьёзность | Файл | Описание | Покрыт тестом? |
|---|---|---|---|---|
| 1 | 🔴 Критический | `TestRunService.java:116` | Сущность навсегда без caseIds при сбое update | Нет |
| 2 | 🔴 Критический | `SnapshotProcessor.java:152` | Тихая потеря caseIds при повреждённой строке | Нет |
| 3 | 🟠 Высокий | `SnapshotProcessorTest.java` | Новый формат хранения не покрыт тестами | — |
| 4 | 🟠 Высокий | `TestRunDTO.java:127` | Клиент не может очистить caseIds через PUT `[]` | Нет |
| 5 | 🟡 Средний | `application.yml:14` | `default-view-inclusion: true` — нет теста на эту зависимость | Нет |
| 6 | 🟡 Средний | `TestRunDTO.java` (Serializer) | Неявный контракт с EntityService ObjectMapper | Нет |

---

## 5. Рекомендации

### Приоритет 1 (до следующего деплоя)

**Добавить тест нового пути в SnapshotProcessorTest:**
```java
runJson.put("caseIds", "[\"" + caseId1 + "\",\"" + caseId2 + "\"]");
// убедиться что processorCalculationResult содержит оба caseId
```

**Добавить тест пути PUT с `[]` в TestRunControllerTest или TestRunServiceTest:**
Либо задокументировать что очистка caseIds через HTTP намеренно заблокирована.

### Приоритет 2 (следующий спринт)

**Защитить двухфазное создание:**  
Обернуть `entityService.update()` в `TestRunService.createTestRun` в try/catch с компенсирующим `entityService.delete()` (или хотя бы с явным логированием и механизмом очистки осиротевших сущностей).

**Добавить тест на `default-view-inclusion`:**  
Интеграционный тест, проверяющий что `GET /runs/{id}` возвращает поля `id`, `name`, `status` при активном `@JsonView(Http.class)`.

### Приоритет 3 (технический долг)

**Задокументировать контракт CaseIdsSerializer:**  
Добавить комментарий в `CaseIdsSerializer`, явно указывающий зависимость от `provider.getActiveView() == null` для Cyoda-пути, с указанием на `common/EntityServiceImpl`.

**Рассмотреть разделение DTO (архитектурное):**  
Текущий подход через `@JsonView` и кастомные ser/deser работает, но хрупок. Альтернатива: `TestRunEntity` (Cyoda, `caseIds: String`) + `TestRunResponse` (HTTP, `caseIds: List<String>`) с явным маппером — устраняет все 6 найденных рисков структурно.

---

## 6. Статус E2E тестов

E2E/Cucumber тесты (`./gradlew :apps:backend:cucumberTest`) в этом сеансе **не запускались** — требуют живой Cyoda-инстанс. По памяти из предыдущих сессий последний известный результат: 53/53 тестов прошло. Рекомендуется прогнать E2E вручную перед следующим деплоем с особым вниманием на сценарии создания TestRun и выполнения Run (RunExecution использует `caseIds` для навигации по шагам).
