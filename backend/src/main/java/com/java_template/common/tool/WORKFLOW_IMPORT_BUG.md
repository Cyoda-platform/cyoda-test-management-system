# 🐛 WORKFLOW IMPORT BUG - Defect & TestRunCase Models

## Issue Summary

When running `./gradlew bootJarWorkflowImport` with `--recreate-models` flag, **Defect** and **TestRunCase** models fail to recreate with error:

```
[400] BAD_REQUEST "cannot save entityModel{name=Defect, version=1} because this model has already been registered"
```

## Root Cause

**CyodaInit.java** has a logic bug in the `ensureEntityModel()` method:

### Timeline of Events:

1. **deleteEntityModel() is NOT being called** for Defect and TestRunCase
2. GET check shows models exist: ✅ `[200] GET ...Defect/1`
3. Log says "deleting..." but NO DELETE HTTP request is sent
4. Later code assumes deletion happened and tries to CREATE
5. Cyoda rejects creation: "already registered"

### Evidence:

In `workflow_full_import.log`:

```
15:26:37.428 INFO: 🗑️  Entity model exists for: Defect, deleting...

(NO DELETE REQUEST IN LOG)

15:26:39.285 INFO: 📝 Entity model not found (404), creating for: Defect

15:26:39.794 ERROR: ❌ [400] "cannot save...already registered"
```

## Impact

- ❌ Defect model not recreated
- ❌ TestRunCase model not recreated  
- ⚠️ 7/9 models successfully imported (others worked fine)
- ❌ Inconsistent state: models partially updated

## Temporary Workaround

1. Don't use `--recreate-models` flag
2. Import without deleting: `java -jar backend-1.0-SNAPSHOT-workflow-import.jar`
3. Manually delete Defect and TestRunCase in Cyoda UI if needed
4. Re-run import

## Next Steps

1. Debug why `deleteEntityModel()` not called for these 2 entities
2. Check if there's exception swallowing in error handling
3. Review `ensureEntityModel()` method logic at lines 313-343
4. Possibly race condition with parallel execution?

## Code Location

- **File**: `backend/src/main/java/com/java_template/common/tool/CyodaInit.java`
- **Method**: `ensureEntityModel()`
- **Lines**: 313-343 (deletion logic)
- **Lines**: 515-561 (`deleteEntityModel()` method)
