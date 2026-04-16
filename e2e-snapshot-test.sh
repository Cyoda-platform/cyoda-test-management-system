#!/bin/bash
# =============================================================================
# E2E test: SnapshotProcessor full flow
# Tests: create run → initialize_run → SnapshotProcessor → TestRunCase/Step
#        snapshot fields → set statuses → complete run
#
# Usage: ./e2e-snapshot-test.sh
# Requires: running backend on localhost:8080
# =============================================================================

set -euo pipefail
BASE="http://localhost:8080/api"
PASS=0; FAIL=0

green() { printf '\033[0;32m✓ %s\033[0m\n' "$*"; }
red()   { printf '\033[0;31m✗ %s\033[0m\n' "$*"; }
info()  { printf '\033[0;36m» %s\033[0m\n' "$*"; }
fatal() { red "$*"; exit 1; }

assert_eq() {
  local label="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    green "$label"
    PASS=$((PASS+1))
  else
    red "$label (expected='$expected' actual='$actual')"
    FAIL=$((FAIL+1))
  fi
}

assert_not_empty() {
  local label="$1" val="$2"
  if [ -n "$val" ] && [ "$val" != "null" ]; then
    green "$label"
    PASS=$((PASS+1))
  else
    red "$label (was empty or null)"
    FAIL=$((FAIL+1))
  fi
}

# ── 0. Login ─────────────────────────────────────────────────────────────────
info "Step 0: Login as admin"
LOGIN_RESP=$(curl -sf -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}') || fatal "Login failed"
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
assert_not_empty "Login returns token" "$TOKEN"
AUTH="-H 'Authorization: Bearer $TOKEN'"

call() { eval "curl -sf -H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json' $*"; }

# ── 1. Create Project ─────────────────────────────────────────────────────────
info "Step 1: Create Project"
PROJECT=$(call "-X POST $BASE/projects -d '{\"name\":\"E2E Snapshot Test Project\",\"description\":\"Auto E2E\"}'") || fatal "Create project failed"
PROJECT_ID=$(echo "$PROJECT" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
assert_not_empty "Project created, id present" "$PROJECT_ID"
info "  project_id=$PROJECT_ID"

# ── 2. Create Suite ───────────────────────────────────────────────────────────
info "Step 2: Create Suite"
SUITE=$(call "-X POST $BASE/projects/$PROJECT_ID/suites -d '{\"name\":\"E2E Suite\",\"description\":\"auto\"}'") || fatal "Create suite failed"
SUITE_ID=$(echo "$SUITE" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
assert_not_empty "Suite created, id present" "$SUITE_ID"
info "  suite_id=$SUITE_ID"

# ── 3. Create TestCase ────────────────────────────────────────────────────────
info "Step 3: Create TestCase"
TC_BODY=$(python3 -c "
import json
print(json.dumps({'title':'Snapshot Test Case','description':'E2E description','preconditions':'User is logged in','priority':'HIGH','suiteId':'$SUITE_ID'}))
")
TC=$(call "-X POST $BASE/projects/$PROJECT_ID/suites/$SUITE_ID/cases -d '$TC_BODY'") || fatal "Create test case failed"
TC_ID=$(echo "$TC" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
TC_DISPLAY=$(echo "$TC" | python3 -c "import sys,json; print(json.load(sys.stdin).get('displayId',''))")
assert_not_empty "TestCase created, id present" "$TC_ID"
info "  test_case_id=$TC_ID display=$TC_DISPLAY"

# ── 4. Create TestStep ────────────────────────────────────────────────────────
info "Step 4: Create TestStep"
STEP_BODY='{"action":"Open login page","expectedResult":"Login page displayed","stepNumber":1}'
STEP=$(call "-X POST $BASE/projects/$PROJECT_ID/suites/$SUITE_ID/cases/$TC_ID/steps -d '$STEP_BODY'") || fatal "Create step failed"
STEP_ID=$(echo "$STEP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
assert_not_empty "TestStep created, id present" "$STEP_ID"
info "  step_id=$STEP_ID"

# ── 5. Create TestRun with the case ──────────────────────────────────────────
info "Step 5: Create TestRun"
RUN_BODY=$(python3 -c "import json; print(json.dumps({'name':'E2E Snapshot Run','projectId':'$PROJECT_ID','caseIds':['$TC_ID'],'stepStatuses':'{}'}))")
RUN=$(call "-X POST $BASE/projects/$PROJECT_ID/runs -d '$RUN_BODY'") || fatal "Create test run failed"
RUN_ID=$(echo "$RUN" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
RUN_STATUS=$(echo "$RUN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))")
assert_not_empty "TestRun created, id present" "$RUN_ID"
assert_eq "TestRun initial status is 'initial'" "initial" "$RUN_STATUS"
info "  run_id=$RUN_ID"

# ── 6. Update run → triggers initialize_run + SnapshotProcessor ──────────────
info "Step 6: Update run status to 'active' (triggers initialize_run)"
UPDATE_BODY=$(python3 -c "import json; print(json.dumps({'name':'E2E Snapshot Run','projectId':'$PROJECT_ID','status':'active','caseIds':['$TC_ID'],'stepStatuses':'{}'}))")
UPDATE_RESP=$(call "-X PUT $BASE/projects/$PROJECT_ID/runs/$RUN_ID -d '$UPDATE_BODY'") || fatal "Update run failed"
UPDATE_STATUS=$(echo "$UPDATE_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))")
assert_eq "TestRun status changed to 'active'" "active" "$UPDATE_STATUS"

# ── 7. Wait for ASYNC_NEW_TX SnapshotProcessor to create TestRunCase ─────────
info "Step 7: Waiting for SnapshotProcessor (ASYNC_NEW_TX, up to 30s)"
SNAPSHOT_FOUND=false
for i in $(seq 1 15); do
  sleep 2
  RC_LIST=$(call "$BASE/projects/$PROJECT_ID/runs/$RUN_ID/cases" 2>/dev/null || echo '{"data":[]}')
  RC_COUNT=$(echo "$RC_LIST" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',d if isinstance(d,list) else [])))" 2>/dev/null || echo 0)
  if [ "$RC_COUNT" -gt 0 ]; then
    SNAPSHOT_FOUND=true
    info "  TestRunCase appeared after $((i*2))s (count=$RC_COUNT)"
    break
  fi
  info "  Attempt $i/15: no TestRunCase yet..."
done

if $SNAPSHOT_FOUND; then
  green "SnapshotProcessor created TestRunCase within 30s"
  PASS=$((PASS+1))
else
  red "SnapshotProcessor did NOT create TestRunCase within 30s"
  FAIL=$((FAIL+1))
fi

# ── 8. Verify TestRunCase snapshot fields ────────────────────────────────────
if $SNAPSHOT_FOUND; then
  info "Step 8: Verify TestRunCase snapshot fields"
  RC_LIST=$(call "$BASE/projects/$PROJECT_ID/runs/$RUN_ID/cases")
  RC=$(echo "$RC_LIST" | python3 -c "import sys,json; d=json.load(sys.stdin); arr=d.get('data',d) if isinstance(d,dict) else d; print(json.dumps(arr[0] if arr else {}))")
  RC_ID=$(echo "$RC" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
  RC_TITLE=$(echo "$RC" | python3 -c "import sys,json; print(json.load(sys.stdin).get('title',''))")
  RC_DESC=$(echo "$RC" | python3 -c "import sys,json; print(json.load(sys.stdin).get('description',''))")
  RC_PREC=$(echo "$RC" | python3 -c "import sys,json; print(json.load(sys.stdin).get('preconditions',''))")
  RC_PRIO=$(echo "$RC" | python3 -c "import sys,json; print(json.load(sys.stdin).get('priority',''))")
  RC_DISP=$(echo "$RC" | python3 -c "import sys,json; print(json.load(sys.stdin).get('displayId',''))")
  RC_SUITE=$(echo "$RC" | python3 -c "import sys,json; print(json.load(sys.stdin).get('suiteId',''))")

  assert_not_empty "TestRunCase.id present" "$RC_ID"
  assert_eq "TestRunCase.title = 'Snapshot Test Case'" "Snapshot Test Case" "$RC_TITLE"
  assert_eq "TestRunCase.description = 'E2E description'" "E2E description" "$RC_DESC"
  assert_eq "TestRunCase.preconditions = 'User is logged in'" "User is logged in" "$RC_PREC"
  assert_eq "TestRunCase.priority = 'HIGH'" "HIGH" "$RC_PRIO"
  assert_not_empty "TestRunCase.displayId present" "$RC_DISP"
  assert_eq "TestRunCase.suiteId = suite id" "$SUITE_ID" "$RC_SUITE"

  # ── 9. Verify TestRunStep snapshot fields ──────────────────────────────────
  info "Step 9: Verify TestRunStep snapshot fields"
  RS_LIST=$(call "$BASE/projects/$PROJECT_ID/runs/$RUN_ID/cases/$RC_ID/steps" 2>/dev/null || echo '{"data":[]}')
  RS=$(echo "$RS_LIST" | python3 -c "import sys,json; d=json.load(sys.stdin); arr=d.get('data',d) if isinstance(d,dict) else d; print(json.dumps(arr[0] if arr else {}))")
  RS_ACTION=$(echo "$RS" | python3 -c "import sys,json; print(json.load(sys.stdin).get('action',''))")
  RS_EXPECTED=$(echo "$RS" | python3 -c "import sys,json; print(json.load(sys.stdin).get('expectedResult',''))")
  RS_NUM=$(echo "$RS" | python3 -c "import sys,json; print(json.load(sys.stdin).get('stepNumber',''))")

  assert_eq "TestRunStep.action = 'Open login page'" "Open login page" "$RS_ACTION"
  assert_eq "TestRunStep.expectedResult = 'Login page displayed'" "Login page displayed" "$RS_EXPECTED"
  assert_eq "TestRunStep.stepNumber = 1" "1" "$RS_NUM"

  # ── 10. Update TestRunCase status ──────────────────────────────────────────
  info "Step 10: Update TestRunCase status to PASSED"
  STATUS_UPDATE=$(call "-X PUT '$BASE/projects/$PROJECT_ID/runs/$RUN_ID/cases/$RC_ID/status?status=PASSED'" 2>/dev/null || echo '{}')
  RC_NEW_STATUS=$(echo "$STATUS_UPDATE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null || echo "")
  # Status might be 'passed' or 'PASSED' depending on the workflow state
  if [ -n "$RC_NEW_STATUS" ] && [ "$RC_NEW_STATUS" != "UNTESTED" ]; then
    green "TestRunCase status updated (status='$RC_NEW_STATUS')"
    PASS=$((PASS+1))
  else
    info "  Status update response: $STATUS_UPDATE"
    red "TestRunCase status update did not change from UNTESTED"
    FAIL=$((FAIL+1))
  fi
fi

# ── 11. Complete the run ──────────────────────────────────────────────────────
info "Step 11: Complete the run"
COMPLETE_RESP=$(call "-X POST $BASE/projects/$PROJECT_ID/runs/$RUN_ID/complete" 2>/dev/null || echo '{}')
COMPLETE_STATUS=$(echo "$COMPLETE_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null || echo "")
assert_eq "TestRun status is 'completed'" "completed" "$COMPLETE_STATUS"

# ── Cleanup ────────────────────────────────────────────────────────────────────
info "Cleanup: deleting project"
call "-X DELETE $BASE/projects/$PROJECT_ID" > /dev/null 2>&1 || true

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════════"
if [ "$FAIL" -eq 0 ]; then
  printf '\033[0;32m✅ ALL TESTS PASSED: %d/%d\033[0m\n' "$PASS" "$((PASS+FAIL))"
else
  printf '\033[0;31m❌ FAILED: %d passed, %d failed\033[0m\n' "$PASS" "$FAIL"
fi
echo "═══════════════════════════════════════════════════════"

[ "$FAIL" -eq 0 ]
