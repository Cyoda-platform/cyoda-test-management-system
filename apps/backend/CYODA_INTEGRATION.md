# 🔗 Cyoda Integration Guide

## ✅ Real Cyoda Credentials Added

Application is now configured to connect to a real Cyoda EU instance.

### 📋 Configuration

**Cyoda Host:** `client-a680fca7878e4c73854cfce50b42a108-dev.eu.cyoda.net`

**Client ID:** `kLXY45`

**Client Secret:** `OAIsUzQMP4LoW19JTwoi`

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
  --args='--app.config.cyoda-host=client-a680fca7878e4c73854cfce50b42a108-dev.eu.cyoda.net \
          --app.config.cyoda-client-id=kLXY45 \
          --app.config.cyoda-client-secret=OAIsUzQMP4LoW19JTwoi'
```

### Option 3: Via Environment Variables

```bash
export CYODA_HOST="client-a680fca7878e4c73854cfce50b42a108-dev.eu.cyoda.net"
export CYODA_CLIENT_ID="kLXY45"
export CYODA_CLIENT_SECRET="OAIsUzQMP4LoW19JTwoi"

./gradlew bootRun
```

---

## 📊 What changed

### application.yml
```yaml
app:
  config:
    cyoda-host: client-a680fca7878e4c73854cfce50b42a108-dev.eu.cyoda.net
    cyoda-client-id: kLXY45
    cyoda-client-secret: OAIsUzQMP4LoW19JTwoi
```

### gRPC Configuration
```
gRPC Address: grpc-client-a680fca7878e4c73854cfce50b42a108-dev.eu.cyoda.net
gRPC Port: 443
```

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
# Clean application.yml
# Set environment variables
export APP_CONFIG_CYODA_HOST="client-a680fca7878e4c73854cfce50b42a108-dev.eu.cyoda.net"
export APP_CONFIG_CYODA_CLIENT_ID="kLXY45"
export APP_CONFIG_CYODA_CLIENT_SECRET="OAIsUzQMP4LoW19JTwoi"

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
description=Unable to resolve host grpc-client-a680fca7878e4c73854cfce50b42a108-dev.eu.cyoda.net
```

**Solution:**
- Check internet connection
- Check that Cyoda server is available
- Check DNS resolution: `nslookup client-a680fca7878e4c73854cfce50b42a108-dev.eu.cyoda.net`

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

