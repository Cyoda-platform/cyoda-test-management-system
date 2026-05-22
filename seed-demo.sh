#!/bin/bash
# seed-demo.sh — seeds the "E-commerce Platform" demo project into a running TMS instance.
#
# Usage:
#   ./seed-demo.sh
#   BACKEND_URL=http://host:8080/api ./seed-demo.sh
#
# The script prompts for admin username and password interactively —
# no plaintext credentials are stored in any file.
#
# The script:
#   1. Waits until the backend health endpoint responds.
#   2. Prompts for admin credentials.
#   3. Obtains a JWT token.
#   4. Calls POST /api/demo/seed.
#   5. Prints the result and exits 0 on success, 1 on failure.
#
# Idempotent: if the demo project already exists the seed is skipped without error.

set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8080/api}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-120}"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           TMS - Demo Data Seeder                              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "🎯 Target: $BACKEND_URL"
echo ""

# ── 1. Wait for backend ───────────────────────────────────────────────────────
echo "⏳ Waiting for backend to be ready (max ${MAX_WAIT_SECONDS}s)..."
ELAPSED=0
until curl -sf "${BACKEND_URL}/actuator/health" -o /dev/null 2>/dev/null; do
  if [ "$ELAPSED" -ge "$MAX_WAIT_SECONDS" ]; then
    echo "❌ Backend did not become ready within ${MAX_WAIT_SECONDS}s. Aborting."
    exit 1
  fi
  sleep 2
  ELAPSED=$((ELAPSED + 2))
done
echo "✅ Backend is ready!"
echo ""

# ── 2. Prompt for credentials ─────────────────────────────────────────────────
echo "🔑 Enter your admin credentials (used once to seed demo data — not stored):"
read -r -p "   Username: " TMS_USER
read -r -s -p "   Password: " TMS_PASS
echo ""
echo ""

# ── 3. Obtain JWT ─────────────────────────────────────────────────────────────
echo "🔑 Authenticating as '$TMS_USER'..."
AUTH_RESPONSE=$(curl -sf -X POST "${BACKEND_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${TMS_USER}\",\"password\":\"${TMS_PASS}\"}" 2>/dev/null || true)

TOKEN=$(echo "$AUTH_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
  echo "❌ Authentication failed. Check your username and password."
  exit 1
fi
echo "✅ Authenticated."
echo ""

# ── 4. Trigger seed ───────────────────────────────────────────────────────────
echo "🌱 Triggering demo seed (this may take a few minutes)..."
HTTP_CODE=$(curl -s -o /tmp/tms-seed-response.json -w "%{http_code}" \
  -X POST "${BACKEND_URL}/demo/seed" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  --max-time 600)

BODY=$(cat /tmp/tms-seed-response.json)

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
  STATUS=$(echo "$BODY" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
  if [ "$STATUS" = "skipped" ]; then
    echo "ℹ️  Demo project already exists — seed skipped."
    echo "   $BODY"
  else
    echo "✅ Demo project seeded successfully!"
    echo "   $BODY"
  fi
  exit 0
else
  echo "❌ Seed failed (HTTP $HTTP_CODE)."
  echo "   $BODY"
  exit 1
fi
