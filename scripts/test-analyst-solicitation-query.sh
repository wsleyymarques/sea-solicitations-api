#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "$0")/.." && pwd)
container_name="sea-query-test-$$"
trap 'docker rm --force "$container_name" >/dev/null 2>&1 || true' EXIT

docker run --detach --name "$container_name" \
  --publish 127.0.0.1::5432 \
  --env POSTGRES_PASSWORD=test-password \
  --env POSTGRES_DB=solicitations \
  postgres:17.11-alpine3.24 >/dev/null

for attempt in {1..30}; do
  if docker exec "$container_name" pg_isready --username postgres --dbname solicitations >/dev/null 2>&1; then
    break
  fi
  if [[ $attempt -eq 30 ]]; then
    echo 'Disposable PostgreSQL did not become ready' >&2
    exit 1
  fi
  sleep 1
done

published_address=$(docker port "$container_name" 5432/tcp)
postgres_port=${published_address##*:}

cd "$repo_dir"
RUN_DB_INTEGRATION=1 ./mvnw \
  -Dtest=SpringDataSolicitationJpaRepositoryIntegrationTest \
  -Dspring.datasource.url="jdbc:postgresql://127.0.0.1:${postgres_port}/solicitations" \
  -Dspring.datasource.username=postgres \
  -Dspring.datasource.password=test-password \
  test
