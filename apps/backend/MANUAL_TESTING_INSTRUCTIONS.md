# 🧪 TMS API — Full Manual Testing Guide

## 📌 Quick Start

### Option 1: Automatic Testing (Recommended)
```bash
# Make sure the application is running, then:
bash TESTING_SCRIPT.sh
```

### Option 2: Manual Testing via Swagger UI
```
http://localhost:8080/api
```

### Option 3: Manual Testing via curl
Follow the instructions below

---

## 🚀 Preparation

### Step 1: Start Application
```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
./gradlew bootRun --args='--app.config.cyoda-host=localhost --app.config.cyoda-client-id=prototype --app.config.cyoda-client-secret=prototype'
```

### Step 2: Get Tokens (in a separate terminal)
```bash
# Admin token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Tester token
TESTER_TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tester","password":"tester123"}' | jq -r '.token')

# Check
echo "Admin: $ADMIN_TOKEN"
echo "Tester: $TESTER_TOKEN"
```

---

## 📋 Full Testing Order (31 Endpoints)

### PHASE 1: AUTHENTICATION (2 endpoints)

**1.1 POST /login (Admin)**
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq
```
✅ Check:
- HTTP 200
- Field `token` contains JWT
- Field `role` = "ADMIN"
- Field `expiresAt` contains a date

**1.2 POST /login (Tester)**
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tester","password":"tester123"}' | jq
```
✅ Check:
- HTTP 200
- Field `role` = "TESTER"

---

### PHASE 2: PROJECTS (5 endpoints)

**2.1 POST /projects (Create)**
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name":"E-Commerce Platform",
    "description":"Testing e-commerce website"
  }' | jq
```
💾 Save `PROJECT_ID` from response

**2.2 GET /projects (List all)**
```bash
curl http://localhost:8080/api/projects \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```
✅ Check: Array contains the created project

**2.3 GET /projects/{id} (Get by ID)**
```bash
curl http://localhost:8080/api/projects/$PROJECT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```
✅ Check: One project with complete information

**2.4 PUT /projects/{id} (Update)**
```bash
curl -X PUT http://localhost:8080/api/projects/$PROJECT_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name":"E-Commerce Platform v2",
    "description":"Updated description"
  }' | jq
```
✅ Check:
- `name` updated to "E-Commerce Platform v2"
- `createdAt` saved (not changed)
- `status` = "ACTIVE"

**2.5 DELETE /projects/{id} (Soft delete)**
```bash
curl -X DELETE http://localhost:8080/api/projects/$PROJECT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```
✅ Check: HTTP 200

---

### PHASE 3: SUITES (5 endpoints)

**3.1 POST /projects/{id}/suites (Create)**
```bash
curl -X POST http://localhost:8080/api/projects/$PROJECT_ID/suites \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name":"Authentication Tests",
    "description":"Login, logout, password reset"
  }' | jq
```
💾 Save `SUITE_ID`

**3.2 GET /projects/{id}/suites (List)**
```bash
curl http://localhost:8080/api/projects/$PROJECT_ID/suites \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

**3.3 GET /projects/{id}/suites/{id} (Get by ID)**
```bash
curl http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

**3.4 PUT /projects/{id}/suites/{id} (Update)**
```bash
curl -X PUT http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name":"Authentication & Authorization Tests",
    "description":"Updated suite"
  }' | jq
```
✅ Check: `createdAt` is preserved

**3.5 DELETE /projects/{id}/suites/{id} (Delete)**
```bash
curl -X DELETE http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

### PHASE 4: TEST CASES (5 endpoints)

**4.1 POST /projects/{id}/suites/{id}/cases (Create)**
```bash
curl -X POST http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "title":"Valid Login with Email",
    "description":"User logs in with valid email and password",
    "priority":"HIGH"
  }' | jq
```
💾 Save `CASE_ID`
✅ **IMPORTANT**: Check that field `title` is saved (not `name`)

**4.2 GET /projects/{id}/suites/{id}/cases (List)**
```bash
curl http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

**4.3 GET /projects/{id}/suites/{id}/cases/{id} (Get by ID)**
```bash
curl http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases/$CASE_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

**4.4 PUT /projects/{id}/suites/{id}/cases/{id} (Update)**
```bash
curl -X PUT http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases/$CASE_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "title":"Valid Login with Email or Phone",
    "description":"User can login with email or phone number",
    "priority":"CRITICAL"
  }' | jq
```
✅ Check:
- `title` updated
- `status` = "ACTIVE"
- `createdAt` saved

**4.5 DELETE /projects/{id}/suites/{id}/cases/{id} (Delete)**
```bash
curl -X DELETE http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases/$CASE_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

### PHASE 5: TEST STEPS (5 endpoints)

**5.1 POST /projects/{id}/suites/{id}/cases/{id}/steps (Create)**
```bash
curl -X POST http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases/$CASE_ID/steps \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "description":"Navigate to login page",
    "expectedResult":"Login form is displayed"
  }' | jq
```
💾 Save `STEP_ID`
✅ **IMPORTANT**: Check that field `description` is saved (not `action`)

**5.2 GET /projects/{id}/suites/{id}/cases/{id}/steps (List)**
```bash
curl http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases/$CASE_ID/steps \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

**5.3 GET /projects/{id}/suites/{id}/cases/{id}/steps/{id} (Get by ID)**
```bash
curl http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases/$CASE_ID/steps/$STEP_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

**5.4 PUT /projects/{id}/suites/{id}/cases/{id}/steps/{id} (Update)**
```bash
curl -X PUT http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases/$CASE_ID/steps/$STEP_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "description":"Navigate to login page and wait for load",
    "expectedResult":"Login form is displayed within 3 seconds"
  }' | jq
```

**5.5 DELETE /projects/{id}/suites/{id}/cases/{id}/steps/{id} (Delete)**
```bash
curl -X DELETE http://localhost:8080/api/projects/$PROJECT_ID/suites/$SUITE_ID/cases/$CASE_ID/steps/$STEP_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

### PHASE 6: TEST RUNS (7 endpoints)

**6.1 POST /projects/{id}/runs (Create)**
```bash
curl -X POST http://localhost:8080/api/projects/$PROJECT_ID/runs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TESTER_TOKEN" \
  -d '{
    "name":"Sprint 1 - Regression Testing",
    "environment":"QA"
  }' | jq
```
💾 Save `RUN_ID`
✅ **IMPORTANT**: Check that field `name` is saved (not `title`), `status` = "CREATED"

**6.2 GET /projects/{id}/runs (List)**
```bash
curl http://localhost:8080/api/projects/$PROJECT_ID/runs \
  -H "Authorization: Bearer $TESTER_TOKEN" | jq
```

**6.3 GET /projects/{id}/runs/{id} (Get by ID)**
```bash
curl http://localhost:8080/api/projects/$PROJECT_ID/runs/$RUN_ID \
  -H "Authorization: Bearer $TESTER_TOKEN" | jq
```

**6.4 PUT /projects/{id}/runs/{id} (Update)**
```bash
curl -X PUT http://localhost:8080/api/projects/$PROJECT_ID/runs/$RUN_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TESTER_TOKEN" \
  -d '{
    "name":"Sprint 1 - Full Regression Testing",
    "environment":"Staging"
  }' | jq
```
✅ Check:
- `name` updated
- `status` = "CREATED"
- `createdAt` saved

**6.5 POST /projects/{id}/runs/{id}/complete (Complete)**
```bash
curl -X POST http://localhost:8080/api/projects/$PROJECT_ID/runs/$RUN_ID/complete \
  -H "Authorization: Bearer $TESTER_TOKEN" | jq
```
✅ Check:
- `status` = "COMPLETED"
- `completedAt` is filled

**6.6 POST /projects/{id}/runs/{id}/unlock (Unlock)**
```bash
curl -X POST http://localhost:8080/api/projects/$PROJECT_ID/runs/$RUN_ID/unlock \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```
✅ Check: `status` = "UNLOCKED"

**6.7 DELETE /projects/{id}/runs/{id} (Delete)**
```bash
curl -X DELETE http://localhost:8080/api/projects/$PROJECT_ID/runs/$RUN_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

### PHASE 7: ADMIN GRPC (2 endpoints)

**7.1 GET /admin/grpc/status (Check status)**
```bash
curl http://localhost:8080/api/admin/grpc/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```
✅ Check: JSON with `connectionState`, `observerState`

**7.2 POST /admin/grpc/reconnect (Reconnect)**
```bash
curl -X POST "http://localhost:8080/api/admin/grpc/reconnect?force=true" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```
✅ Check: Reconnection message

---

## ✅ Final Checklist

- [ ] **PHASE 1**: Both login endpoints work, tokens obtained
- [ ] **PHASE 2**: All 5 Project endpoints work, PUT preserves createdAt
- [ ] **PHASE 3**: All 5 Suite endpoints work
- [ ] **PHASE 4**: All 5 TestCase endpoints work, `title` is saved
- [ ] **PHASE 5**: All 5 TestStep endpoints work, `description` is saved
- [ ] **PHASE 6**: All 7 TestRun endpoints work, `name` is saved, complete/unlock work
- [ ] **PHASE 7**: Both gRPC endpoints work
- [ ] All endpoints return HTTP 200/201
- [ ] All DTO fields are properly mapped
- [ ] PUT operations preserve `createdAt` and `status`
- [ ] Swagger UI is available at http://localhost:8080/api

---

## 🐛 If Something Doesn't Work

1. **Check Application Logs** — look for ERROR or WARN
2. **Ensure Token is Valid** — tokens are valid for 24 hours
3. **Verify Correct IDs are Used** — copy from previous response
4. **Run Unit Tests** — `./gradlew test` (all should pass)
5. **Restart Application** — sometimes helps

---

## 📊 Summary

- **Total endpoints**: 31
- **Testing phases**: 7
- **Time for full testing**: ~15-20 minutes
- **Required tools**: curl, jq (optional)

---

## 🎯 Successful Completion

If all checklists are completed ✅, the prototype is **fully ready for use**!
