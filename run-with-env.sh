#!/bin/bash

# Load environment variables from .env file and run the application
# This script sources the .env file to make all environment variables available to gradle

set -e

# Get the project root directory (grandparent of apps/backend directory)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              Cyoda TMS - Running with .env loaded             ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if .env file exists
if [ ! -f "$PROJECT_ROOT/.env" ]; then
    echo "❌ Error: .env file not found at $PROJECT_ROOT/.env"
    echo ""
    echo "Please create a .env file by copying .env.example:"
    echo "  cp .env.example .env"
    echo ""
    echo "Then fill in your Cyoda credentials."
    exit 1
fi

echo "📂 Project root: $PROJECT_ROOT"
echo "📋 Loading environment from: $PROJECT_ROOT/.env"
echo ""

# Load the .env file into current shell environment
set -a
source "$PROJECT_ROOT/.env"
set +a

# Verify required variables are set
required_vars=("CYODA_HOST" "CYODA_CLIENT_ID" "CYODA_CLIENT_SECRET")
missing_vars=()

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        missing_vars+=("$var")
    fi
done

if [ ${#missing_vars[@]} -gt 0 ]; then
    echo "❌ Error: Missing required environment variables:"
    for var in "${missing_vars[@]}"; do
        echo "   - $var"
    done
    exit 1
fi

echo "✅ Environment variables loaded successfully:"
echo "   CYODA_HOST: ${CYODA_HOST:0:20}..."
echo "   CYODA_CLIENT_ID: ${CYODA_CLIENT_ID:0:10}..."
echo "   CYODA_CLIENT_SECRET: ••••••••••"
echo ""

# Run gradle runApp with the loaded environment
cd "$PROJECT_ROOT"
echo "🚀 Starting application..."
echo ""

./gradlew :apps:backend:runApp "$@"
