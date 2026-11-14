#!/usr/bin/env bash
# Writes one plain-SQL pg_dump per SwiftDrop database into deploy/mock-data/dumps/.
# Run from the repository root after generate_mock_data.py, with the Postgres container running.
set -euo pipefail

CONTAINER=${POSTGRES_CONTAINER:-swiftdrop-postgres}
OUT=deploy/mock-data/dumps
mkdir -p "$OUT"

for db in auth_db pricing_db parcel_db dispatch_db merchant_db notification_db; do
  # --clean --if-exists: loading replaces what is there. Owners are kept, so tables belong to each service's user.
  docker exec "$CONTAINER" pg_dump -U postgres --clean --if-exists --no-privileges "$db" > "$OUT/$db.sql"
  echo "$OUT/$db.sql  $(wc -c < "$OUT/$db.sql" | tr -d ' ') bytes"
done
