#!/usr/bin/env bash
# Drops and recreates the six SwiftDrop databases (EVERY ROW IS LOST), then restarts the services so Flyway
# creates the tables again. Local development only. Run from the repository root with the stack running.
set -euo pipefail

CONTAINER=${POSTGRES_CONTAINER:-swiftdrop-postgres}
# database:owner, as created by deploy/compose/postgres-init/init.sql
DATABASES="auth_db:auth pricing_db:pricing parcel_db:parcel dispatch_db:dispatch merchant_db:merchant notification_db:notif"
SERVICES="auth-service pricing-service parcel-service dispatch-service merchant-service notification-service"

args=()
for pair in $DATABASES; do
  db=${pair%%:*}; owner=${pair##*:}
  args+=(-c "DROP DATABASE IF EXISTS $db WITH (FORCE)" -c "CREATE DATABASE $db OWNER $owner")
done
docker exec "$CONTAINER" psql -U postgres -v ON_ERROR_STOP=1 -q "${args[@]}"
echo "Recreated: $DATABASES"

# Queued events and webhook retries belong to the old data; drop them so they do not land in the new databases
RABBIT=${RABBITMQ_CONTAINER:-swiftdrop-rabbitmq}
for queue in $(docker exec "$RABBIT" rabbitmqctl list_queues name -q --no-table-headers 2>/dev/null); do
  docker exec "$RABBIT" rabbitmqctl purge_queue "$queue" -q > /dev/null
done
echo "Emptied the RabbitMQ queues"

docker restart $(for s in $SERVICES; do echo "swiftdrop-$s"; done) > /dev/null
echo "Restarted the services; waiting for them to be healthy..."
for s in $SERVICES; do
  for _ in $(seq 1 60); do
    [ "$(docker inspect -f '{{.State.Health.Status}}' "swiftdrop-$s")" = healthy ] && break
    sleep 3
  done
  echo "  $s: $(docker inspect -f '{{.State.Health.Status}}' "swiftdrop-$s")"
done
