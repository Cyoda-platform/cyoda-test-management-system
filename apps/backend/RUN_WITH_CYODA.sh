#!/bin/bash

# TMS API - Run with Real Cyoda Credentials
# This script starts the application with real Cyoda EU instance

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         TMS API - Starting with Real Cyoda Credentials        ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Load environment variables from .env file
if [ -f ".env" ]; then
    set -a; source .env; set +a
    echo "✅ Loaded credentials from .env"
else
    echo "❌ Error: .env file not found"
    echo "Please copy .env.example to .env and fill in your credentials"
    exit 1
fi

# Verify required credentials
if [ -z "$CYODA_HOST" ] || [ -z "$CYODA_CLIENT_ID" ] || [ -z "$CYODA_CLIENT_SECRET" ]; then
    echo "❌ Error: Missing required environment variables"
    echo "Please ensure .env contains:"
    echo "  - CYODA_HOST"
    echo "  - CYODA_CLIENT_ID"
    echo "  - CYODA_CLIENT_SECRET"
    exit 1
fi

# Set Java path
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"

echo "📋 Configuration:"
echo "   Cyoda Host: $CYODA_HOST"
echo "   Client ID: $CYODA_CLIENT_ID"
echo "   Client Secret: ••••••••••••••••••••"
echo ""

echo "🔨 Building project..."
./gradlew clean build -x test > /dev/null 2>&1
echo "✅ Build successful"
echo ""

echo "🚀 Starting application..."
echo "   Server: http://localhost:8080/api"
echo "   Swagger UI: http://localhost:8080/api"
echo ""
echo "Press Ctrl+C to stop"
echo ""

./gradlew bootRun \
  --args="--app.config.cyoda-host=$CYODA_HOST --app.config.cyoda-client-id=$CYODA_CLIENT_ID --app.config.cyoda-client-secret=$CYODA_CLIENT_SECRET"

