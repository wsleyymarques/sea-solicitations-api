#!/usr/bin/env bash
set -euo pipefail

if [[ "${ALLOW_TEST_SEED:-}" != "1" ]]; then
  echo 'This seed creates demo accounts with a shared test password.' >&2
  echo 'Run only against a local test database with ALLOW_TEST_SEED=1.' >&2
  exit 2
fi

if ! command -v docker >/dev/null 2>&1; then
  echo 'Required command not found: docker' >&2
  exit 2
fi

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_dir=$(cd "$script_dir/.." && pwd)
cd "$repo_dir"

docker compose exec --no-TTY postgres \
  psql -X --username "${POSTGRES_USER:-solicitations}" \
       --dbname "${POSTGRES_DB:-solicitations}" \
       --set ON_ERROR_STOP=1 \
  < "$script_dir/seed-test-users.sql"

cat <<'INFO'

Demo login for all three accounts:
  Password: StrongPassword@123
  CLIENT:   cliente.teste@example.com
  ANALYST:  analista.teste@example.com (coverage: SP, RJ)
  ADMIN:    admin.teste@example.com
INFO
