#!/bin/bash

set -a; source .env; set +a

TOKEN=$(curl -s -u "$CYODA_CLIENT_ID:$CYODA_CLIENT_SECRET" -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&scope=ROLE_M2M' \
  "https://$CYODA_HOST/api/oauth/token" | python3 -c 'import sys, json; print(json.load(sys.stdin).get("access_token",""))')

MODELS=("Project" "Suite" "TestCase" "Defect" "TestRun" "TestRunCase" "TestRunStep" "Attachment" "Report" "ProjectCounter")
WORKFLOWS=("Project" "Suite" "TestCase" "Defect" "TestRun" "TestRunCase" "TestRunStep" "Attachment" "Report" "ProjectCounter")

echo "═══════════════════════════════════════════════════════════════"
echo "DIRECT HTTP IMPORT - Entity Schemas"
echo "═══════════════════════════════════════════════════════════════"
echo;

for model in "${MODELS[@]}"; do
  MODEL_LOWER=$(echo $model | tr '[:upper:]' '[:lower:]')
  FILE="apps/backend/src/main/resources/entity/$MODEL_LOWER/version_1/${MODEL_LOWER}.json"
  
  if [ -f "$FILE" ]; then
    echo -n "📥 Importing $model from JSON file... ";
    CODE=$(curl -s -w '%{http_code}' -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      --data @"$FILE" "https://$CYODA_HOST/api/model/import/JSON/SAMPLE_DATA/$model/1" -o /dev/null)
    
    if [ "$CODE" = "200" ]; then
      echo "✅ HTTP $CODE";
    else
      echo "⚠️  HTTP $CODE";
    fi
  else
    echo "❌ $model: File not found at $FILE";
  fi
done

echo;
echo "═══════════════════════════════════════════════════════════════"
echo "IMPORT - Workflows"
echo "═══════════════════════════════════════════════════════════════"
echo;

for model in "${WORKFLOWS[@]}"; do
  MODEL_LOWER=$(echo $model | tr '[:upper:]' '[:lower:]')
  FILE="apps/backend/src/main/resources/workflow/$MODEL_LOWER/version_1/$model.json"
  
  if [ -f "$FILE" ]; then
    # Read and wrap workflow
    WORKFLOW=$(cat "$FILE")
    WRAPPED=$(echo "{\"workflows\": [$WORKFLOW], \"importMode\": \"REPLACE\"}")
    
    echo -n "📥 Importing $model workflow... ";
    CODE=$(curl -s -w '%{http_code}' -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d "$WRAPPED" "https://$CYODA_HOST/api/model/$model/1/workflow/import" -o /dev/null)
    
    if [ "$CODE" = "200" ]; then
      echo "✅ HTTP $CODE";
    else
      echo "⚠️  HTTP $CODE";
    fi
  else
    echo "❌ $model: File not found at $FILE";
  fi
done

echo;
echo "═══════════════════════════════════════════════════════════════"
echo "LOCKING ALL MODELS"
echo "═══════════════════════════════════════════════════════════════"
echo;

for model in "${MODELS[@]}"; do
  echo -n "🔒 Locking $model... ";
  curl -s -X PUT -H "Authorization: Bearer $TOKEN" "https://$CYODA_HOST/api/model/$model/1/lock" > /dev/null 2>&1
  echo "✅";
done

echo;
echo "═══════════════════════════════════════════════════════════════"
echo "✅ IMPORT COMPLETE!"
echo "═══════════════════════════════════════════════════════════════"
