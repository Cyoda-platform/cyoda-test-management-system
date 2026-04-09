#!/usr/bin/env python3
"""
Tenant Cleanup Script (Direct Cyoda API)
Directly calls Cyoda API to delete all entity data
"""

import json
import sys
import subprocess
import time
import re

def get_config():
    """Load Cyoda config from RUN_WITH_CYODA.sh"""
    config = {}
    try:
        with open("/Users/Victoria/PycharmProjects/cyoda-test-management-system/backend/RUN_WITH_CYODA.sh") as f:
            for line in f:
                if line.startswith("export CYODA_"):
                    key, val = line.replace('export ', '').strip().split('=', 1)
                    config[key] = val.strip('"\'')
    except Exception as e:
        print(f"❌ Error reading config: {e}")
        sys.exit(1)
    return config

def get_token(host, client_id, client_secret):
    """Get OAuth2 token from Cyoda"""
    import base64
    auth = base64.b64encode(f"{client_id}:{client_secret}".encode()).decode()
    cmd = f"""curl -sS -u {client_id}:{client_secret} -H 'Content-Type: application/x-www-form-urlencoded' \\
      --data 'grant_type=client_credentials' \\
      "https://{host}/api/oauth/token" | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])'"""
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout.strip() if result.returncode == 0 else None

def delete_all_entities(host, token, entity_name):
    """Delete all entities using Cyoda REST API"""
    url = f"https://{host}/api/entity/{entity_name}"
    cmd = f"""curl -sS -X DELETE -H "Authorization: Bearer {token}" "{url}" 2>/dev/null"""
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)

    if not result.stdout:
        return 0

    try:
        # Try to parse JSON response
        body = json.loads(result.stdout)
        if isinstance(body, dict):
            return body.get("deletedCount", body.get("data", 0))
        return 0
    except:
        # Try to extract number from text response
        match = re.search(r'(\d+)', result.stdout)
        return int(match.group(1)) if match else 0

def main():
    config = get_config()
    host = config.get("CYODA_HOST")
    client_id = config.get("CYODA_CLIENT_ID")
    client_secret = config.get("CYODA_CLIENT_SECRET")

    if not all([host, client_id, client_secret]):
        print("❌ Missing Cyoda config. Check RUN_WITH_CYODA.sh")
        sys.exit(1)

    print(f"🔐 Connecting to Cyoda: {host}")
    token = get_token(host, client_id, client_secret)

    if not token:
        print("❌ Failed to get OAuth2 token")
        sys.exit(1)

    print(f"✅ Token obtained\n")

    entity_types = [
        "TestRunStep",
        "TestRunCase",
        "TestRun",
        "Attachment",
        "Defect",
        "TestCase",
        "Suite",
        "Project",
        "TestStep",
        "ProjectCounter"
    ]

    print("🗑️  Deleting all entity data...\n")
    total_deleted = 0

    for entity_type in entity_types:
        print(f"⏳ {entity_type}...", end=" ", flush=True)
        deleted = delete_all_entities(host, token, entity_type)

        if deleted > 0:
            print(f"✅ {deleted}")
            total_deleted += deleted
        else:
            print(f"⏭️  (none)")

        time.sleep(0.3)

    print(f"\n✨ Cleanup complete! Deleted {total_deleted} total entities")

if __name__ == "__main__":
    main()
