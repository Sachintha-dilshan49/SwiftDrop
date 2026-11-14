#!/usr/bin/env bash
# Loads the mock data dumps into the local Compose Postgres, REPLACING the six databases' current data.
# Run from the repository root. The databases and users must exist (deploy/compose/postgres-init/init.sql
# creates them on the first start of the postgres container).
set -euo pipefail

CONTAINER=${POSTGRES_CONTAINER:-swiftdrop-postgres}
DUMPS=deploy/mock-data/dumps
# database:owner, as created by deploy/compose/postgres-init/init.sql
DATABASES="auth_db:auth pricing_db:pricing parcel_db:parcel dispatch_db:dispatch merchant_db:merchant notification_db:notif"
SERVICES="auth-service pricing-service parcel-service dispatch-service merchant-service notification-service"

running=()
for s in $SERVICES; do
  if [ "$(docker inspect -f '{{.State.Running}}' "swiftdrop-$s" 2>/dev/null)" = true ]; then running+=("swiftdrop-$s"); fi
done
if [ ${#running[@]} -gt 0 ]; then
  echo "Stopping services while loading: ${running[*]}"
  docker stop "${running[@]}" > /dev/null
fi

for pair in $DATABASES; do
  db=${pair%%:*}; owner=${pair##*:}
  # Start from an empty database: a newer version of a service may have added tables the dump does not know about,
  # and Flyway would then fail re-running that migration on top of them
  docker exec "$CONTAINER" psql -U postgres -q -v ON_ERROR_STOP=1 \
    -c "DROP DATABASE IF EXISTS $db WITH (FORCE)" -c "CREATE DATABASE $db OWNER $owner"
  docker exec -i "$CONTAINER" psql -U postgres -d "$db" -q -v ON_ERROR_STOP=1 < "$DUMPS/$db.sql" > /dev/null
  echo "Loaded $db"
done

# Queued events and webhook retries belong to the old data; drop them so they do not land in the loaded data
RABBIT=${RABBITMQ_CONTAINER:-swiftdrop-rabbitmq}
if [ "$(docker inspect -f '{{.State.Running}}' "$RABBIT" 2>/dev/null)" = true ]; then
  for queue in $(docker exec "$RABBIT" rabbitmqctl list_queues name -q --no-table-headers 2>/dev/null); do
    docker exec "$RABBIT" rabbitmqctl purge_queue "$queue" -q > /dev/null
  done
  echo "Emptied the RabbitMQ queues"
fi

if [ ${#running[@]} -gt 0 ]; then
  docker start "${running[@]}" > /dev/null
  echo "Started the services again"
fi
