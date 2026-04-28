# 📋 Entity Schema and Workflow Import Guide

## ✅ Correct Way to Import Entity Schemas and Workflows to Cyoda

This document describes a **verified and working** method for importing all entity schemas and workflows.

---

## 🚀 Quick Start

```bash
# 1. Ensure .env file has correct credentials
cat .env | grep CYODA

# 2. ⚠️ MANDATORY: Delete all tenant entities (cascading deletion)
# Run deletion in correct order (from leaves to root):
# TestRunStep → TestRunCase → TestRun → Attachment → Defect →
# TestStep → TestCase → Suite → Report → ProjectCounter → Project

# 3. Unlock and delete all models
# Then run import script:

# 3. Run import script
./import-schemas.sh
```

---

## 🚨 IMPORTANT: Cascading Deletion Before Re-Import

**ALWAYS delete all tenant entities BEFORE re-importing!**

### Correct Deletion Order (Bottom-Up / from leaves to root):

```bash
# Delete entities in correct order (dependencies!)
TestRunStep    # ← leaves (deepest)
TestRunCase
TestRun
Attachment
Defect
TestStep
TestCase
Suite
Report
ProjectCounter
Project        # ← root (top level)
```

### Why is Order Important?

If you delete in the wrong order (for example, start with Project), Cyoda won't be able to delete child entities (TestCase, Suite, etc.) and they will remain as **orphaned entities**.

### Automatic Deletion of All Entities (Cascading):

```bash
set -a; source .env; set +a; TOKEN=$(curl -s -u "$CYODA_CLIENT_ID:$CYODA_CLIENT_SECRET" -H 'Content-Type: application/x-www-form-urlencoded' -d 'grant_type=client_credentials&scope=ROLE_M2M' "https://$CYODA_HOST/api/oauth/token" | python3 -c 'import sys, json; print(json.load(sys.stdin).get("access_token",""))');

MODELS_ORDER=("TestRunStep" "TestRunCase" "TestRun" "Attachment" "Defect" "TestStep" "TestCase" "Suite" "Report" "ProjectCounter" "Project")

for model in "${MODELS_ORDER[@]}"; do
  curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/entity/$model/1" > /dev/null 2>&1
done
```

---

## 📝 What Gets Imported

**11 Entity Schemas:**
- Project, Suite, TestCase, TestStep
- Defect, TestRun, TestRunCase, TestRunStep
- Attachment, Report, ProjectCounter

**11 Workflows:**
- Project, Suite, TestCase, TestStep, Defect
- TestRun, TestRunCase, TestRunStep, Attachment, Report, ProjectCounter

---

## 🔧 How It Works

### Step 1: Prepare Entity Schemas

Entity schemas are stored in JSON files:
```
apps/backend/src/main/resources/entity/
├── project/version_1/project.json
├── suite/version_1/suite.json
├── testcase/version_1/testcase.json
└── ... (others)
```

**Each JSON file contains:**
- All entity fields
- Valid example values
- All required fields

**Important:** Cyoda determines the schema from the **structure of the JSON file**. Fields with empty values (`[]`, `{}`) may be defined incorrectly. Use realistic example data!

### Step 2: Import Entity Schemas via HTTP API

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  --data @project.json \
  "https://$CYODA_HOST/api/model/import/JSON/SAMPLE_DATA/Project/1"
```

### Step 3: Import Workflows

Workflows are stored in:
```
apps/backend/src/main/resources/workflow/
├── project/version_1/Project.json
├── suite/version_1/Suite.json
└── ... (others)
```

Each workflow is imported via:
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"workflows": [WORKFLOW_JSON]}' \
  "https://$CYODA_HOST/api/model/Project/1/workflow/import"
```

### Step 4: Lock Models

After import, all models must be locked:
```bash
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  "https://$CYODA_HOST/api/model/Project/1/lock"
```

---

## 📜 Script Usage (Recommended)

The `import-schemas.sh` script automates the entire process:

```bash
./import-schemas.sh
```

The script:
1. Loads credentials from .env
2. Gets JWT token from Cyoda
3. Imports all 11 entity schemas
4. Imports all 11 workflows
5. Locks all models

---

## ⚠️ Important Notes

### 0. FULL CYCLE: Delete entities, models, then re-import

**Before each re-import you need to:**

1. **Delete all tenant entities** (cascading, see above)
2. **Unlock all models:**
```bash
MODELS=("Project" "Suite" "TestCase" "TestStep" "Defect" "TestRun" "TestRunCase" "TestRunStep" "Attachment" "Report" "ProjectCounter")
for model in "${MODELS[@]}"; do
  curl -s -X PUT -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/model/$model/1/unlock"
done
```

3. **Delete all models:**
```bash
for model in "${MODELS[@]}"; do
  curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/model/$model/1"
done
```

4. **Then run import:**
```bash
./import-schemas.sh
```

### 1. Models Must Be Deleted Before Re-Import

If models already exist, HTTP 400 is returned. You need to delete:

```bash
# Unlock
curl -X PUT -H "Authorization: Bearer $TOKEN" \
  "https://$CYODA_HOST/api/model/Project/1/unlock"

# Delete
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  "https://$CYODA_HOST/api/model/Project/1"
```

### 2. JSON Files DEFINE the Schema

Cyoda reads the schema from the structure of the JSON file. For example:

```json
{
  "id": "uuid-value",
  "name": "string-value",
  "caseIds": ["uuid1", "uuid2"],
  "stepStatuses": {"key::step": "PASSED"}
}
```

**Rule:** Use realistic examples so Cyoda correctly identifies types:
- Empty `[]` → Cyoda might identify as STRING
- Array with data `["item"]` → Cyoda identifies as ARRAY ✅

### 3. WorkflowImportTool is NOT Used

❌ Old method with `WorkflowImportTool` is ignored.
✅ Only HTTP curl import via `import-schemas.sh` is used.

---

## 🐛 Troubleshooting

**Problem:** HTTP 400 during import
- **Solution:** Delete model via unlock/delete, then re-import

**Problem:** Fields missing in Cyoda
- **Solution:** Check that JSON file contains all fields with realistic examples

**Problem:** Workflow import returns 400
- **Solution:** Ensure that entity model already exists before importing workflow

---

## ✨ Summary

**Correct approach:**
1. Prepare JSON files in `src/main/resources/entity/` and `src/main/resources/workflow/`
2. Run `./import-schemas.sh`
3. Verify that all models and workflows are imported in Cyoda UI

**Quick command:**
```bash
./import-schemas.sh
```
