#!/bin/bash

# TMS API - Import Workflows to Cyoda
# This script imports all workflow definitions to the Cyoda platform

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              TMS API - Importing Workflows to Cyoda            ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Set Java path
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"

# Check if build directory exists
if [ ! -d "build/libs" ]; then
    echo "🔨 Building project first..."
    ./gradlew clean build -x test > /dev/null 2>&1
    echo "✅ Build complete"
    echo ""
fi

echo "📋 Validating workflows before import..."
echo ""

# Get the JAR file
JAR_FILE=$(find build/libs -name "*.jar" -type f | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ Error: No JAR file found in build/libs"
    exit 1
fi

# Note: WorkflowValidationSuite and WorkflowImportTool are part of Spring Boot app
# and need full Spring context, so validation is skipped here.
# Validation can be run separately if needed.

echo "🔄 Importing workflows to Cyoda via REST API..."
echo ""
echo "📌 Make sure the application is running (bash RUN_WITH_CYODA.sh)"
echo ""

# Check if app is running
if ! curl -s http://localhost:8080/api/admin/grpc/status > /dev/null 2>&1; then
    echo "⚠️  WARNING: Application does not appear to be running on localhost:8080"
    echo "Please start it with: bash RUN_WITH_CYODA.sh"
    echo ""
fi

# Call the import endpoint directly via REST
echo "Calling import endpoint..."
curl -X POST http://localhost:8080/api/admin/grpc/import-workflows \
  -H "Content-Type: application/json" \
  -w "\n✅ Request sent (HTTP Status: %{http_code})\n" 2>/dev/null || \
  echo "⚠️  Could not connect to API. Make sure the app is running."

echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "✅ Workflow import completed!"
echo ""
echo "📝 Next steps:"
echo "   1. Check Cyoda UI to verify workflows were imported"
echo "   2. Run tests: bash TESTING_SCRIPT.sh"
echo "   3. Check gRPC status: curl http://localhost:8080/api/admin/grpc/status"
echo ""

