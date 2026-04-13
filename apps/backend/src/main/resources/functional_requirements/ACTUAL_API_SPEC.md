# Actual API Specification

**Last Updated:** 2026-04-09  
**Status:** Current Implementation  
**Purpose:** Document the ACTUAL REST API endpoints implemented in the system

---

## Overview

This document reflects the **current state** of the API as implemented in the backend code (DTOs, Controllers, Services). It complements the Functional Requirements (FR.md) and User Stories (userstories.md).

---

## Authentication

```
POST /api/login
Content-Type: application/json

Request:
{
  "username": "admin",  // "admin" or "tester"
  "password": "admin123"
}

Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "username": "admin",
    "role": "Admin"
  }
}
```

---

## Projects

```
POST /api/projects
Create a new project

GET /api/projects
List all projects

GET /api/projects/{projectId}
Get project details

PUT /api/projects/{projectId}
Update project

DELETE /api/projects/{projectId}
Delete project (soft delete)
```

**ProjectDTO Structure:**
```json
{
  "id": "uuid",
  "name": "Project Name",
  "description": "Project description",
  "status": "Active",     // ADDED: Active | Archived
  "deleted": false,       // ADDED: soft delete flag
  "createdAt": "2026-04-09T10:00:00Z",
  "updatedAt": "2026-04-09T10:00:00Z"
}
```

---

## Suites

```
POST /api/projects/{projectId}/suites
Create suite

GET /api/projects/{projectId}/suites
List suites

PATCH /api/projects/{projectId}/suites/reorder
Reorder suites (ADDED)
```

---

## Test Cases

```
POST /api/projects/{projectId}/suites/{suiteId}/cases
Create single case

POST /api/projects/{projectId}/suites/{suiteId}/cases/batch
Create multiple cases (ADDED)

GET /api/projects/{projectId}/suites/{suiteId}/cases
List cases

GET /api/projects/{projectId}/cases/{caseId}
Get case details

PATCH /api/projects/{projectId}/suites/{suiteId}/cases/{caseId}/move
Move case to another suite (ADDED)

PATCH /api/projects/{projectId}/suites/{suiteId}/cases/reorder
Reorder cases (ADDED)
```

**TestCaseDTO Structure (EXTENDED):**
```json
{
  "id": "uuid",
  "displayId": "TC-001",        // ADDED
  "title": "Test case title",
  "description": "Description",
  "preconditions": "Preconditions",
  "priority": "HIGH",
  "status": "ACTIVE",           // ADDED
  "deleted": false,             // ADDED
  "createdAt": "2026-04-09T10:00:00Z",
  "updatedAt": "2026-04-09T10:00:00Z"
}
```

---

## Test Runs

```
POST /api/projects/{projectId}/runs
Create test run

GET /api/projects/{projectId}/runs
List runs

GET /api/projects/{projectId}/runs/{runId}
Get run details

GET /api/projects/{projectId}/runs/{runId}/details
Get run WITH all cases and metrics (ADDED: composite endpoint)

PATCH /api/projects/{projectId}/runs/{runId}
Update run status, unlock

DELETE /api/projects/{projectId}/runs/{runId}
Delete run
```

**TestRunDTO Structure (EXTENDED):**
```json
{
  "id": "uuid",
  "displayId": "RUN-001",        // ADDED
  "name": "Run name",
  "environment": "Staging",
  "buildVersion": "v2.5.0",      // ADDED (CI/CD tracking)
  "description": "Description",
  "status": "active",
  "passed": 5,                   // ADDED (auto-calculated)
  "failed": 2,                   // ADDED (auto-calculated)
  "skipped": 1,                  // ADDED (auto-calculated)
  "untested": 0,                 // ADDED (auto-calculated)
  "startedAt": "2026-04-09T10:00:00Z",  // ADDED
  "completedAt": null,           // ADDED
  "createdAt": "2026-04-09T10:00:00Z",
  "updatedAt": "2026-04-09T10:00:00Z"
}
```

---

## Defects (NEW)

```
POST /api/projects/{projectId}/defects
Create defect

GET /api/projects/{projectId}/defects
List defects (filterable by severity, status)

GET /api/projects/{projectId}/defects/{defectId}
Get defect details

PUT /api/projects/{projectId}/defects/{defectId}
Update defect

DELETE /api/projects/{projectId}/defects/{defectId}
Delete defect
```

**DefectDTO Structure:**
```json
{
  "id": "uuid",
  "displayId": "DEF-001",
  "title": "Defect title",
  "description": "Defect description",
  "severity": "Critical",        // Critical | Major | Minor
  "link": "https://jira.com/...",
  "status": "Open",              // Open | In Progress | Fixed | Closed
  "source": "run-uuid-or-case-uuid",
  "testRunId": "uuid",
  "testRunCaseId": "uuid",
  "createdAt": "2026-04-09T10:00:00Z",
  "updatedAt": "2026-04-09T10:00:00Z"
}
```

---

## Reports (NEW)

```
POST /api/projects/{projectId}/reports
Create report

GET /api/projects/{projectId}/reports
List reports

GET /api/projects/{projectId}/reports/{reportId}
Get report with aggregated metrics

DELETE /api/projects/{projectId}/reports/{reportId}
Delete report
```

**ReportDTO Structure:**
```json
{
  "id": "uuid",
  "displayId": "REP-001",
  "name": "Regression Report",
  "type": "Summary",             // Summary | Regression | Sprint | Custom
  "description": "Description",
  "createdBy": "admin",
  "dateFrom": "2026-04-01",
  "dateTo": "2026-04-09",
  "selectedRuns": ["uuid1", "uuid2"],
  "sectionExecutiveSummary": true,
  "sectionSuiteAnalytics": true,
  "sectionDefectTable": true,
  "sectionEnvironmentInfo": true,
  "createdAt": "2026-04-09T10:00:00Z",
  "updatedAt": "2026-04-09T10:00:00Z"
}
```

---

## Attachments (via EdgeMessage)

```
POST /message/*
Upload attachment (EdgeMessage API integration)

GET /projects/{projectId}/cases/{caseId}/attachments
List attachments for case
```

---

## Summary of Changes vs Original FR

| Feature | Original FR | Current Implementation |
|---------|-------------|------------------------|
| Project Status | ❌ Not mentioned | ✅ status, deleted fields |
| TestCase displayId | ❌ Not mentioned | ✅ Auto-generated (TC-XXX) |
| TestRun metrics | ⚠️ Real-time mention | ✅ Explicit counters (passed, failed, skipped) |
| TestRun buildVersion | ❌ Not mentioned | ✅ For CI/CD tracking |
| TestRun timestamps | ❌ Not mentioned | ✅ startedAt, completedAt |
| Defect Entity | ⚠️ "Bug URL field" only | ✅ Full DefectDTO with lifecycle |
| Report Entity | ⚠️ "Export to PDF/CSV" only | ✅ Full ReportDTO with sections |
| Repository Reorder | ❌ Not mentioned | ✅ PATCH endpoints |
| Batch Case Import | ❌ Not mentioned | ✅ POST .../cases/batch |

---

## Notes

- All endpoints require authentication (JWT token)
- All DTOs include id, createdAt, updatedAt timestamps
- displayIds are auto-generated and never recomputed
- Soft deletes are implemented for Projects, TestCases (likely TestRuns)
- Data is strictly scoped to Project level
