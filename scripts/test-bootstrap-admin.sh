#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "$0")/.." && pwd)
container_name="sea-bootstrap-test-$$"
trap 'docker rm --force "$container_name" >/dev/null 2>&1 || true' EXIT

docker run --detach --name "$container_name" \
  --env POSTGRES_PASSWORD=test-password \
  --env POSTGRES_DB=solicitations \
  postgres:17.11-alpine3.24 >/dev/null

for attempt in {1..30}; do
  if docker exec "$container_name" pg_isready --username postgres --dbname solicitations >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

docker exec --interactive "$container_name" psql --username postgres --dbname solicitations \
  --set ON_ERROR_STOP=1 --quiet \
  < "$repo_dir/src/main/resources/db/migration/V1__create_initial_schema.sql"

seed() {
  {
    printf '\\set admin_email %s\n' "$1"
    printf '\\set admin_hash %s\n' "$2"
    cat "$repo_dir/scripts/bootstrap-admin.sql"
  } | docker exec --interactive "$container_name" psql \
    --username postgres --dbname solicitations --set ON_ERROR_STOP=1 --quiet
}

query() {
  docker exec "$container_name" psql --username postgres --dbname solicitations \
    --tuples-only --no-align --command "$1" | tr -d '\n'
}

hash='$2y$12$test-hash-for-seed-test'
docker exec "$container_name" psql --username postgres --dbname solicitations \
  --set ON_ERROR_STOP=1 --quiet --command \
  "INSERT INTO users (id, full_name, email, password_hash, role, active, created_at, updated_at, version) VALUES (gen_random_uuid(), 'Client', 'client@example.com', 'hash', 'CLIENT', true, now(), now(), 0)"

if seed client@example.com "$hash" >/dev/null 2>&1; then
  echo 'Expected the existing client email to reject admin bootstrap' >&2
  exit 1
fi
[[ $(query "SELECT count(*) FROM users WHERE role = 'ADMIN'") == 0 ]]

seed admin@example.com "$hash" >/dev/null
[[ $(query "SELECT count(*) FROM users WHERE role = 'ADMIN'") == 1 ]]
[[ $(query "SELECT password_hash FROM users WHERE email = 'admin@example.com'") == "$hash" ]]

seed another@example.com '$2y$12$another-hash' >/dev/null
[[ $(query "SELECT count(*) FROM users WHERE role = 'ADMIN'") == 1 ]]
[[ $(query "SELECT password_hash FROM users WHERE email = 'admin@example.com'") == "$hash" ]]

echo 'Admin bootstrap SQL integration checks passed'
