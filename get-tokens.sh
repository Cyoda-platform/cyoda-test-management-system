#!/bin/bash

# TMS API - Get Authentication Tokens
# Reads user credentials from .env (APP_USERS_0_* and APP_USERS_1_*)

BASE_URL="http://localhost:8080/api"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              TMS API - Getting Authentication Tokens           ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

if ! command -v curl &> /dev/null; then
    echo "❌ Error: curl is not installed"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo "⚠️  Warning: jq is not installed, showing raw JSON"
    echo ""
fi

# Load credentials from .env
if [ -f .env ]; then
  set -a
  source .env
  set +a
else
  echo "❌ .env file not found. Copy .env.example to .env and fill in your credentials."
  exit 1
fi

USER0_NAME="${APP_USERS_0_USERNAME:-}"
USER0_PASS="${APP_USERS_0_PASSWORD:-}"
USER1_NAME="${APP_USERS_1_USERNAME:-}"
USER1_PASS="${APP_USERS_1_PASSWORD:-}"

if [ -z "$USER0_NAME" ] || [ -z "$USER0_PASS" ]; then
  echo "❌ APP_USERS_0_USERNAME / APP_USERS_0_PASSWORD not set in .env"
  exit 1
fi

login() {
  local username="$1"
  local password="$2"
  curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"$password\"}"
}

get_field() {
  local json="$1"
  local field="$2"
  if command -v jq &> /dev/null; then
    echo "$json" | jq -r ".$field"
  else
    echo "$json" | grep -o "\"$field\":\"[^\"]*" | cut -d'"' -f4
  fi
}

echo "🔐 Logging in as $USER0_NAME..."
USER0_RESPONSE=$(login "$USER0_NAME" "$USER0_PASS")
USER0_TOKEN=$(get_field "$USER0_RESPONSE" "token")
USER0_ROLE=$(get_field "$USER0_RESPONSE" "role")
echo "✅ OK — role: $USER0_ROLE"
echo ""

USER1_TOKEN=""
if [ -n "$USER1_NAME" ] && [ -n "$USER1_PASS" ]; then
  echo "🔐 Logging in as $USER1_NAME..."
  USER1_RESPONSE=$(login "$USER1_NAME" "$USER1_PASS")
  USER1_TOKEN=$(get_field "$USER1_RESPONSE" "token")
  USER1_ROLE=$(get_field "$USER1_RESPONSE" "role")
  echo "✅ OK — role: $USER1_ROLE"
  echo ""
fi

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                      TOKENS OBTAINED                          ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "export ${USER0_NAME^^}_TOKEN=\"$USER0_TOKEN\""
if [ -n "$USER1_TOKEN" ]; then
  echo "export ${USER1_NAME^^}_TOKEN=\"$USER1_TOKEN\""
fi
echo ""
echo "To load into your current shell, run the export commands above."
echo "═══════════════════════════════════════════════════════════════════"
