#!/usr/bin/env bash
# Saves each container's log to stack-logs/<service>.log (used by CI on every run).
set -uo pipefail

cd "$(dirname "$0")/.."
mkdir -p stack-logs
compose=(docker compose -f ../deploy/compose/docker-compose.yml -f compose/docker-compose.e2e.yml)
for service in $("${compose[@]}" config --services); do
  "${compose[@]}" logs --no-color --timestamps "$service" > "stack-logs/${service}.log" 2>&1 || true
done
"${compose[@]}" ps > stack-logs/ps.txt 2>&1 || true
echo "Logs written to e2e/stack-logs/"
