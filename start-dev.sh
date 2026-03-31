#!/bin/bash

# TMS - Start both Backend and Frontend in development mode
# This script starts the backend and frontend together

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           TMS - Starting Backend and Frontend                 ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Set Java path
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"

# Cyoda Configuration
export CYODA_HOST="client-a680fca7878e4c73854cfce50b42a108-dev.eu.cyoda.net"
export CYODA_CLIENT_ID="kLXY45"
export CYODA_CLIENT_SECRET="OAIsUzQMP4LoW19JTwoi"

echo "📋 Configuration:"
echo "   Frontend: http://localhost:5173"
echo "   Backend: http://localhost:8080/api"
echo "   Cyoda Host: $CYODA_HOST"
echo ""

# Function to kill both processes on exit
cleanup() {
  echo ""
  echo "🛑 Stopping services..."
  kill $BACKEND_PID 2>/dev/null || true
  kill $FRONTEND_PID 2>/dev/null || true
  wait $BACKEND_PID 2>/dev/null || true
  wait $FRONTEND_PID 2>/dev/null || true
  echo "✅ Services stopped"
}

trap cleanup EXIT

# Start backend in background
echo "🚀 Starting Backend..."
cd backend
./gradlew bootRun \
  --args="--app.config.cyoda-host=$CYODA_HOST --app.config.cyoda-client-id=$CYODA_CLIENT_ID --app.config.cyoda-client-secret=$CYODA_CLIENT_SECRET" \
  > /tmp/tms-backend.log 2>&1 &
BACKEND_PID=$!
cd ..

echo "✅ Backend PID: $BACKEND_PID"

# Wait for backend to be ready (check port 8080)
echo "⏳ Waiting for Backend to be ready..."
for i in {1..30}; do
  if nc -z localhost 8080 2>/dev/null; then
    echo "✅ Backend is ready!"
    break
  fi
  echo "  Attempt $i/30..."
  sleep 2
done

# Start frontend in background
echo "🚀 Starting Frontend..."
cd frontend
npm run dev > /tmp/tms-frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

echo "✅ Frontend PID: $FRONTEND_PID"
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "🎉 Both services are running!"
echo "   Frontend: http://localhost:5173"
echo "   Backend: http://localhost:8080/api"
echo "   Login: admin / admin123"
echo ""
echo "📋 Logs:"
echo "   Backend: tail -f /tmp/tms-backend.log"
echo "   Frontend: tail -f /tmp/tms-frontend.log"
echo ""
echo "Press Ctrl+C to stop all services"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Wait for both processes
wait $BACKEND_PID
wait $FRONTEND_PID

