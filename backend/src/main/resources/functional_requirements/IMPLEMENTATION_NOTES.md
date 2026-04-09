# Implementation Notes: Deviations from Original MVP Requirements

**Purpose:** Explain what was added to the system beyond the original FR.md and why.

---

## Summary

The original Functional Requirements (FR.md) outlined an MVP for a Test Management System. During implementation, the team added **significant enhancements** to support real-world QA workflows:

- **Defect Tracking** — Expanded from "Bug URL field" to a full entity
- **Advanced Reporting** — Added custom reports with configurable sections
- **Repository Organization** — Added reorder and move operations
- **Batch Operations** — Added bulk import for performance
- **Metrics & Lifecycle** — Enhanced test run tracking

These changes were driven by **practical necessity** and **feedback**, not scope creep.

---

## Detailed Additions

### 1. Display IDs (TC-001, RUN-001, DEF-001, REP-001)

**What:** Auto-generated human-readable identifiers for all major entities

**Why:** Users need easy ways to reference entities in documentation, logs, and communication. UUIDs are not user-friendly.

**Implementation:**
- Generated once at entity creation
- Never recomputed
- Scoped per entity type per project
- Pattern: EntityType-SequenceNumber

**Related FR:** Enhanced FR 2.2, 3.1, 3.9, 4.3

---

### 2. Defect Management (Module 3+)

**What:** Expanded "Bug URL field" to a complete Defect entity with lifecycle

**Original FR 3.5:** "Bug URL field to save links to external bug trackers"

**Problem:** URL-only approach doesn't support:
- Tracking defect severity
- Managing defect status (Open → Fixed → Closed)
- Linking to multiple test runs
- Filtering defects by severity

**Solution:** DefectDTO with:
- displayId, title, description
- severity (Critical, Major, Minor)
- status lifecycle (Open → In Progress → Fixed → Closed)
- testRunId, testRunCaseId references
- external link (optional)

**Related FR:** FR 3.9, 3.10 (new)

---

### 3. Advanced Reporting (Module 4+)

**What:** Full Report entity with configurable sections beyond basic export

**Original FR 4.2:** "Export Test Run results to PDF/CSV"

**Problem:** Static export doesn't support:
- Selecting which runs to include
- Toggling report sections (some stakeholders want metrics, others want defect details)
- Saving report templates for reuse
- Date range filtering

**Solution:** ReportDTO with:
- Custom run selection
- Toggleable sections (Executive Summary, Suite Analytics, Defect Table, Environment)
- Date range filtering
- Multiple report types (Summary, Regression, Sprint, Custom)

**Related FR:** FR 4.3, 4.4, 4.5 (new)

---

### 4. Repository Organization

**What:** Reorder and move operations for Suites and Test Cases

**Original FR:** Not mentioned (implicit assumption of static structure)

**Problem:** Teams need to reorganize tests without recreating them:
- Reorder by priority/coverage
- Group new test cases
- Move between suites during refactoring

**Solution:**
- PATCH /suites/reorder — change suite order
- PATCH /suites/{id}/cases/reorder — change case order within suite
- PATCH /suites/{id}/cases/{id}/move — move case to different suite

**Related FR:** FR 2.9, 2.10 (new)

---

### 5. Batch Test Case Import

**What:** Create multiple test cases with steps in single API call

**Original FR 2.6:** "Bulk-import test cases from CSV/XML"

**Problem:** Creating 100 test cases with 5 steps each requires:
- 100 POST calls for cases
- 500 POST calls for steps
- N+1 problem, poor performance

**Solution:** POST /suites/{id}/cases/batch
- Accept array of cases with nested steps
- Create all in single transaction
- Auto-allocate displayIds

**Related FR:** FR 2.10 (new)

---

### 6. Test Run Metrics & Lifecycle

**What:** Explicit counters (passed, failed, skipped, untested) and timestamps

**Original FR 3.1:** "Real-time metrics" mentioned vaguely

**Problem:** Frontend needed explicit fields:
- Counters for progress display
- startedAt/completedAt for duration tracking
- buildVersion for CI/CD correlation

**Solution:** TestRunDTO enhancements:
- passed, failed, skipped, untested (auto-calculated from steps)
- startedAt, completedAt
- buildVersion (for CI/CD integration)

**Related FR:** FR 3.7, 3.8 (enhanced)

---

### 7. Soft Delete Flags

**What:** deleted boolean flags on Project, TestCase (likely TestRun)

**Original FR:** Mentioned for test cases only

**Problem:** Admins need to remove projects/runs from view but retain history for audits

**Solution:** Boolean deleted flag (soft delete pattern)
- Entity hidden from normal queries when deleted=true
- Preserved in snapshots for historical integrity
- Recoverable if needed

**Related FR:** FR 1.3 (enhanced), FR 2.8 (enhanced)

---

### 8. Composite Endpoints

**What:** GET /projects/{id}/runs/{id}/details bundles run + cases + metrics

**Original FR:** Not mentioned

**Problem:** Frontend needs multiple related objects:
- Test Run details
- All TestRunCase records
- Current metrics
- Without composite endpoint = 3+ HTTP requests

**Solution:** Single endpoint returning bundled object
- Eliminates waterfall requests
- Improves performance
- Easier state management on frontend

**Related FR:** FR 3.8 (new)

---

## Decision Log

| Feature | Decision | Rationale |
|---------|----------|-----------|
| displayIds | Add to all entities | UX: humans need readable identifiers, not UUIDs |
| Defect entity | Expand from URL | Real teams need defect lifecycle tracking |
| Report entity | Extend from export | Stakeholders have diverse reporting needs |
| Reorder/move | Add operations | Admins need to reorganize tests |
| Batch import | Add endpoint | Performance: single call vs N+1 |
| Metrics fields | Make explicit | Frontend needs clear counters, not calculations |

---

## Questions for Product Owner

1. Are these enhancements aligned with your vision?
2. Any features you'd like to REMOVE from the implementation?
3. Should any of these become OPTIONAL (feature flags)?

---

## Next Steps

1. ✅ Update FR.md with new FRs
2. ✅ Update userstories.md with new epics
3. ✅ Create ACTUAL_API_SPEC.md (this is it)
4. ⏳ Create IMPLEMENTATION_NOTES.md (this document)
5. ⏳ Update consolidated_requirements.md if it exists
6. ⏳ Replace old docs with updated versions when ready
