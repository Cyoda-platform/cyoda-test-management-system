# 📋 Entity Schema and Workflow Import Guide

## ✅ Правильный способ импорта Entity Schemas и Workflows в Cyoda

Этот документ описывает **проверенный и работающий** метод импорта всех entity schemas и workflows.

---

## 🚀 Быстрый старт

```bash
# 1. Убедись что .env файл имеет правильные credentials
cat .env | grep CYODA

# 2. ⚠️ ОБЯЗАТЕЛЬНО: Удали все tenant entities (каскадное удаление)
# Запусти удаление в правильном порядке (от листьев к корню):
# TestRunStep → TestRunCase → TestRun → Attachment → Defect →
# TestStep → TestCase → Suite → Report → ProjectCounter → Project

# 3. Разлочи и удали все модели
# Потом запусти импорт скрипт:

# 3. Запусти импорт скрипт
./import-schemas.sh
```

---

## 🚨 ВАЖНО: Каскадное удаление перед переimпортом

**ВСЕГДА удаляй все tenant entities ПЕРЕД переimпортом!**

### Правильный порядок удаления (Bottom-Up / от листьев к корню):

```bash
# Удалить entities в правильном порядке (зависимости!)
TestRunStep    # ← листья (самые глубокие)
TestRunCase
TestRun
Attachment
Defect
TestStep
TestCase
Suite
Report
ProjectCounter
Project        # ← корень (самый верхний)
```

### Почему важен порядок?

Если удалять в неправильном порядке (например, сначала Project), то Cyoda не сможет удалить child entities (TestCase, Suite и т.д.) и они останутся как **orphaned entities**.

### Автоматическое удаление всех entities (каскадом):

```bash
set -a; source .env; set +a; TOKEN=$(curl -s -u "$CYODA_CLIENT_ID:$CYODA_CLIENT_SECRET" -H 'Content-Type: application/x-www-form-urlencoded' -d 'grant_type=client_credentials&scope=ROLE_M2M' "https://$CYODA_HOST/api/oauth/token" | python3 -c 'import sys, json; print(json.load(sys.stdin).get("access_token",""))');

MODELS_ORDER=("TestRunStep" "TestRunCase" "TestRun" "Attachment" "Defect" "TestStep" "TestCase" "Suite" "Report" "ProjectCounter" "Project")

for model in "${MODELS_ORDER[@]}"; do
  curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/entity/$model/1" > /dev/null 2>&1
done
```

---

## 📝 Что импортируется

**11 Entity Schemas:**
- Project, Suite, TestCase, TestStep
- Defect, TestRun, TestRunCase, TestRunStep
- Attachment, Report, ProjectCounter

**11 Workflows:**
- Project, Suite, TestCase, TestStep, Defect
- TestRun, TestRunCase, TestRunStep, Attachment, Report, ProjectCounter

---

## 🔧 Как это работает

### Шаг 1: Prepare Entity Schemas

Entity schemas хранятся в JSON файлах:
```
apps/backend/src/main/resources/entity/
├── project/version_1/project.json
├── suite/version_1/suite.json
├── testcase/version_1/testcase.json
└── ... (остальные)
```

**Каждый JSON файл содержит:**
- Все поля entity
- Валидные значения для примера
- Все required fields

**Важно:** Cyoda определяет schema из **структуры JSON файла**. Поля с пустыми значениями (`[]`, `{}`) могут быть определены неправильно. Используй реалистичные примеры данных!

### Шаг 2: Импорт Entity Schemas через HTTP API

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  --data @project.json \
  "https://$CYODA_HOST/api/model/import/JSON/SAMPLE_DATA/Project/1"
```

### Шаг 3: Импорт Workflows

Workflows хранятся в:
```
apps/backend/src/main/resources/workflow/
├── project/version_1/Project.json
├── suite/version_1/Suite.json
└── ... (остальные)
```

Каждый workflow импортируется через:
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"workflows": [WORKFLOW_JSON]}' \
  "https://$CYODA_HOST/api/model/Project/1/workflow/import"
```

### Шаг 4: Lock Models

После импорта все модели должны быть залочены:
```bash
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  "https://$CYODA_HOST/api/model/Project/1/lock"
```

---

## 📜 Использование скрипта (Рекомендуется)

Скрипт `import-schemas.sh` автоматизирует весь процесс:

```bash
./import-schemas.sh
```

Скрипт выполняет:
1. Загружает credentials из .env
2. Получает JWT token от Cyoda
3. Импортирует все 11 entity schemas
4. Импортирует все 11 workflows
5. Залочивает все модели

---

## ⚠️ Важные замечания

### 0. ПОЛНЫЙ ЦИКЛ: Удали entities, модели, потом переimпортируй

**Перед каждым переimпортом нужно:**

1. **Удалить все tenant entities** (каскадом, см. выше)
2. **Разлочить все модели:**
```bash
MODELS=("Project" "Suite" "TestCase" "TestStep" "Defect" "TestRun" "TestRunCase" "TestRunStep" "Attachment" "Report" "ProjectCounter")
for model in "${MODELS[@]}"; do
  curl -s -X PUT -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/model/$model/1/unlock"
done
```

3. **Удалить все модели:**
```bash
for model in "${MODELS[@]}"; do
  curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/model/$model/1"
done
```

4. **Потом запусти импорт:**
```bash
./import-schemas.sh
```

### 1. Модели должны быть удалены перед переimпортом

Если модели уже существуют, HTTP 400 вернется. Нужно удалить:

```bash
# Разлочить
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  "https://$CYODA_HOST/api/model/Project/1/unlock"

# Удалить
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  "https://$CYODA_HOST/api/model/Project/1"
```

### 2. JSON файлы ОПРЕДЕЛЯЮТ schema

Cyoda читает schema из структуры JSON файла. Например:

```json
{
  "id": "uuid-value",
  "name": "string-value",
  "caseIds": ["uuid1", "uuid2"],
  "stepStatuses": {"key::step": "PASSED"}
}
```

**Правило:** Используй реалистичные примеры, чтобы Cyoda правильно определил типы:
- Пустые `[]` → Cyoda может определить как STRING
- Массив с данными `["item"]` → Cyoda определит как ARRAY ✅

### 3. WorkflowImportTool НЕ используется

❌ Старый способ с `WorkflowImportTool` игнорируется.  
✅ Используется только HTTP curl импорт через `import-schemas.sh`.

---

## 🐛 Troubleshooting

**Проблема:** HTTP 400 при импорте
- **Решение:** Удали модель через unlock/delete, потом переimпортируй

**Проблема:** Поля отсутствуют в Cyoda
- **Решение:** Проверь что JSON файл содержит все поля с реалистичными примерами

**Проблема:** Workflow импорт возвращает 400
- **Решение:** Убедись что entity model уже существует перед импортом workflow

---

## ✨ Итог

**Правильный способ:**
1. Подготовь JSON files в `src/main/resources/entity/` и `src/main/resources/workflow/`
2. Запусти `./import-schemas.sh`
3. Проверь что все модели и workflows импортированы в Cyoda UI

**Быстрая команда:**
```bash
./import-schemas.sh
```
