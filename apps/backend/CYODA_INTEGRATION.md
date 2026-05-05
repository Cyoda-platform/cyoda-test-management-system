# 🔗 Cyoda Integration Guide

## ✅ Connecting to a Real Cyoda Instance

Application connects to a Cyoda EU instance using M2M credentials from `.env`.

### 📋 Configuration

Set the following in your `.env` file (copy from `.env.example`):

**Cyoda Host:** `CYODA_HOST=<your-cyoda-host>`

**Client ID:** `CYODA_CLIENT_ID=<your-client-id>`

**Client Secret:** `CYODA_CLIENT_SECRET=<your-client-secret>`

**Grant Type:** `client_credentials`

---

## 🚀 Running with Real Cyoda

### Option 1: Use Script (RECOMMENDED)

```bash
bash RUN_WITH_CYODA.sh
```

Script automatically:
- Sets Java path
- Builds project
- Runs application with real credentials

### Option 2: Manual Run

```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"

./gradlew bootRun \
  --args='--app.config.cyoda-host=$CYODA_HOST \
          --app.config.cyoda-client-id=$CYODA_CLIENT_ID \
          --app.config.cyoda-client-secret=$CYODA_CLIENT_SECRET'
```

### Option 3: Via Environment Variables

```bash
export CYODA_HOST="<your-cyoda-host>"
export CYODA_CLIENT_ID="<your-client-id>"
export CYODA_CLIENT_SECRET="<your-client-secret>"

./gradlew bootRun
```

---

## 📊 Configuration Reference

### application.yml

Credentials are loaded from environment variables — never hardcode them here:

```yaml
app:
  config:
    cyoda-host: ${CYODA_HOST}
    cyoda-client-id: ${CYODA_CLIENT_ID}
    cyoda-client-secret: ${CYODA_CLIENT_SECRET}
```

### gRPC Configuration

gRPC address is derived from `CYODA_HOST` by adding the `grpc-` prefix. Port: `443`.

---

## ✅ Connection Verification

### 1. Start Application
```bash
bash RUN_WITH_CYODA.sh
```

### 2. Check gRPC Status
```bash
# Get Token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Check gRPC Status
curl http://localhost:8080/api/admin/grpc/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

**Expected Result:**
```json
{
  "connectionState": "READY",
  "observerState": "READY",
  "lastKeepAliveTimestampMs": 1234567890
}
```

### 3. Check Logs
Look for in logs:
- ✅ `gRPC Managed Channel state changed: CONNECTING -> READY`
- ✅ `Keep alive received`
- ✅ `Stream Observer state changes: ... -> READY`

---

## 🔐 Security

### ⚠️ IMPORTANT: Do not commit credentials to git!

Credentials are already added to `application.yml` for development convenience.

**For production:**
1. Delete credentials from `application.yml`
2. Use environment variables
3. Use secrets management (AWS Secrets Manager, HashiCorp Vault, etc.)

### Using environment variables (secure)

```bash
# Set environment variables from .env or manually
export APP_CONFIG_CYODA_HOST="<your-cyoda-host>"
export APP_CONFIG_CYODA_CLIENT_ID="<your-client-id>"
export APP_CONFIG_CYODA_CLIENT_SECRET="<your-client-secret>"

# Start
./gradlew bootRun
```

---

## 📝 Configuration Files

### .env.example
Example file with environment variables. Copy to `.env` and update values.

### RUN_WITH_CYODA.sh
Script for running with real credentials.

### application.yml
Main application config (contains credentials for development).

---

## 🧪 Testing with Real Cyoda

### Run all tests
```bash
bash TESTING_SCRIPT.sh
```

### Check gRPC Connection
```bash
curl http://localhost:8080/api/admin/grpc/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.connectionState'
```

### Reconnect to Cyoda
```bash
curl -X POST "http://localhost:8080/api/admin/grpc/reconnect?force=true" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

---

## 🐛 Troubleshooting

### Problem: "Unable to resolve host"
```
Failed to resolve name. status=Status{code=UNAVAILABLE,
description=Unable to resolve host grpc-<your-cyoda-host>
```

**Solution:**
- Check internet connection
- Check that Cyoda server is available
- Check DNS resolution: `nslookup $CYODA_HOST`

### Problem: "Failed to get access token"
```
Failed to get access token. Will not set the Bearer Token
```

**Solution:**
- Check that credentials are correct
- Check that Cyoda server is available
- Check logs for error details

### Problem: "TRANSIENT_FAILURE"
```
connectionState: TRANSIENT_FAILURE
```

**Solution:**
- This is normal on first connection
- Application will automatically reconnect
- Check logs for details

---

## 📊 Connection status

### Possible states

| Status | Description |
|---|---|
| `IDLE` | Idle |
| `CONNECTING` | Connecting |
| `READY` | ✅ Ready |
| `TRANSIENT_FAILURE` | Temporary error (reconnecting) |
| `SHUTDOWN` | Shutdown |

---

## ✨ Summary

Application is now fully integrated with real Cyoda EU instance.

**Start:**
```bash
bash RUN_WITH_CYODA.sh
```

**Test:**
```bash
bash TESTING_SCRIPT.sh
```

**Swagger UI:**
```
http://localhost:8080/api
```

