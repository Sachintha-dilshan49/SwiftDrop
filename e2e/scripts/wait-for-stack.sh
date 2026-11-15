#!/usr/bin/env bash
# Waits until the gateway, every service, Mailpit and the webhook receiver answer their health checks.
# Usage: bash scripts/wait-for-stack.sh [timeout-seconds]   (default 600)
set -euo pipefail

timeout="${1:-600}"
deadline=$(( $(date +%s) + timeout ))

# name|url. ai-assistant is left out: the gateway does not wait for it either (slow first start, optional LLM key).
checks=(
  "auth-service|http://localhost:8081/actuator/health"
  "pricing-service|http://localhost:8082/actuator/health"
  "parcel-service|http://localhost:8083/actuator/health"
  "dispatch-service|http://localhost:8084/actuator/health"
  "merchant-service|http://localhost:8085/actuator/health"
  "notification-service|http://localhost:8086/actuator/health"
  "api-gateway|http://localhost:8080/actuator/health"
  "mailpit|http://localhost:8025/api/v1/info"
  "webhook-receiver|http://localhost:8089/__admin/health"
)

for check in "${checks[@]}"; do
  name="${check%%|*}"
  url="${check#*|}"
  printf 'Waiting for %-22s' "$name"
  until body=$(curl -fs --max-time 5 "$url" 2>/dev/null) && [[ "$name" == mailpit || "$name" == webhook-receiver || "$body" == *'"UP"'* ]]; do
    if (( $(date +%s) > deadline )); then
      echo " timed out after ${timeout}s"
      exit 1
    fi
    printf '.'
    sleep 5
  done
  echo " up"
done
