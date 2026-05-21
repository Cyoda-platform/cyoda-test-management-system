#!/usr/bin/env bash
# Usage: ./scripts/hash-password.sh <password>
# Outputs a bcrypt hash suitable for users-seed.yml passwordHash field.
# Requires: htpasswd (part of apache2-utils / httpd-tools) OR Python 3 with bcrypt.

set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <password>" >&2
    exit 1
fi

PASSWORD="$1"

if command -v htpasswd &>/dev/null; then
    # -B = bcrypt, -n = print to stdout, -C 12 = cost factor 12
    htpasswd -bnBC 12 "" "$PASSWORD" | tr -d ':\n' | sed 's/^\$/\$/'
    echo
elif python3 -c "import bcrypt" &>/dev/null 2>&1; then
    python3 -c "
import bcrypt, sys
pw = sys.argv[1].encode()
print(bcrypt.hashpw(pw, bcrypt.gensalt(rounds=12)).decode())
" "$PASSWORD"
else
    echo "Error: install apache2-utils (htpasswd) or Python bcrypt: pip install bcrypt" >&2
    exit 1
fi
