# Phase 2: Validation Checklist

**Objective:** Verify that backend and frontend implementation matches the updated FR_UPDATED.md and ACTUAL_API_SPEC.md

**Timeline:** Ready to start

---

## 🔍 Backend Validation

### DTOs Check
- [ ] Check ProjectDTO has: id, name, description, status, deleted, createdAt, updatedAt
- [ ] Check TestCaseDTO has: id, displayId, title, description, preconditions, priority, status, deleted, createdAt, updatedAt
- [ ] Check TestRunDTO has: id, displayId, name, environment, buildVersion, description, status, passed, failed, skipped, untested, startedAt, completedAt
- [ ] Check DefectDTO has: id, displayId, title, description, severity, link, status, source, testRunId, testRunCaseId, createdAt, updatedAt
- [ ] Check ReportDTO has: id, displayId, name, type, description, createdBy, dateFrom, dateTo, selectedRuns, sectionExecutiveSummary, sectionSuiteAnalytics, sectionDefectTable, sectionEnvironmentInfo

### Controllers Check
- [ ] POST /projects — implemented
- [ ] GET /projects — implemented
- [ ] GET /projects/{id} — implemented
- [ ] PUT /projects/{id} — implemented
- [ ] DELETE /projects/{id} — implemented (soft delete)
- [ ] PATCH /suites/reorder — implemented
- [ ] PATCH /suites/{id}/cases/reorder — implemented
- [ ] PATCH /suites/{id}/cases/{id}/move — implemented
- [ ] POST /suites/{id}/cases/batch — implemented
- [ ] POST /projects/{id}/defects — implemented
- [ ] GET /projects/{id}/defects — implemented
- [ ] PUT /projects/{id}/defects/{id} — implemented
- [ ] DELETE /projects/{id}/defects/{id} — implemented
- [ ] POST /projects/{id}/reports — implemented
- [ ] GET /projects/{id}/reports — implemented
- [ ] GET /projects/{id}/reports/{id} — implemented
- [ ] DELETE /projects/{id}/reports/{id} — implemented
- [ ] GET /projects/{id}/runs/{id}/details — implemented (composite endpoint)

### Services Check
- [ ] DefectService — exists and has CRUD methods
- [ ] ReportService — exists and has CRUD + generation methods
- [ ] TestRunService — properly tracks metrics (passed, failed, skipped, untested)
- [ ] TestCaseService — auto-generates displayId
- [ ] Repository reordering — move/reorder methods exist
- [ ] Batch import — supports creating multiple cases with steps

### Business Logic Check
- [ ] TestCase displayId is auto-generated and immutable
- [ ] TestRun displayId is auto-generated and immutable
- [ ] Defect displayId is auto-generated and immutable
- [ ] Report displayId is auto-generated and immutable
- [ ] Soft delete (deleted flag) properly implemented
- [ ] Metrics auto-calculated (passed/failed/skipped/untested)
- [ ] Defect status lifecycle works (Open → In Progress → Fixed → Closed)
- [ ] Report sections are toggleable
- [ ] Batch import is transactional

---

## 🎨 Frontend Validation

### API Types Check (src/lib/api.ts)
- [ ] Project interface has status, deleted fields
- [ ] TestCase interface has displayId, status, deleted fields
- [ ] TestRun interface has displayId, buildVersion, metrics counters, timestamps
- [ ] Defect interface defined (or imported from backend types)
- [ ] Report interface defined (or imported from backend types)

### Hooks Check (src/hooks/)
- [ ] useDefects() hook exists
- [ ] useReports() hook exists
- [ ] useTestRun() hook updated for metrics fields
- [ ] useProject() hook updated for status field

### Components Check
- [ ] DefectList component exists and uses new API
- [ ] DefectForm component exists
- [ ] ReportBuilder component exists
- [ ] ReportList component exists
- [ ] ReorderUI component for suites/cases exists
- [ ] MoveCase component exists

### Type Safety Check
- [ ] No TypeScript errors in frontend
- [ ] All API calls use correct types
- [ ] All response types match ACTUAL_API_SPEC.md

---

## 🧪 Test Coverage Check

### Backend Tests
- [ ] DefectService tests exist
- [ ] ReportService tests exist
- [ ] Batch import tests exist
- [ ] Reorder/move operations tests exist
- [ ] Soft delete tests exist
- [ ] displayId generation tests exist
- [ ] Metrics calculation tests exist

### Frontend Tests (if any)
- [ ] Defect component tests
- [ ] Report component tests
- [ ] Reorder functionality tests

### E2E Tests
- [ ] Create defect and link to run — works
- [ ] Create report with sections — works
- [ ] Reorder suites/cases — works
- [ ] Batch import cases — works
- [ ] Metrics update in real-time — works

---

## 📝 Documentation Check

- [ ] README.md updated with new features
- [ ] API documentation (OpenAPI/Swagger) updated
- [ ] Code comments updated for new functionality
- [ ] Wiki/guides updated (if exists)
- [ ] Contributing guide updated (if exists)

---

## 🚀 Readiness Checklist

Before moving to Phase 3 (Tests), all of above should be:
- ✅ Verified to exist
- ✅ Verified to work correctly
- ✅ Verified to match specification

When all checks pass:
1. Mark this checklist as complete
2. Create pull request (if using git)
3. Move to Phase 3: Write comprehensive tests
4. Then Phase 4: Finalize and merge documentation

---

## 📍 Current Status

- **Phase 1:** ✅ COMPLETE - Documentation updated
- **Phase 2:** 🔄 IN PROGRESS - Validation (YOU ARE HERE)
- **Phase 3:** ⏳ PENDING - Write tests
- **Phase 4:** ⏳ PENDING - Finalize & merge docs

---

## 💡 Tips

- Run `./gradlew test` to check backend tests
- Run `npm run test` to check frontend tests
- Use ACTUAL_API_SPEC.md as reference for verification
- Compare with real code to catch any gaps
- Document any discrepancies you find
