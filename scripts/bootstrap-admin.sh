#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! "$1" =~ ^[[:alnum:]_.+%-]+@[[:alnum:].-]+\.[[:alpha:]]{2,}$ ]]; then
  echo 'Usage: scripts/bootstrap-admin.sh admin@example.com' >&2
  exit 2
fi

for command in docker htpasswd; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Required command not found: $command" >&2
    exit 2
  fi
done

script_dir=$(cd "$(dirname "$0")" && pwd)
admin_email=$1
echo 'Enter the initial administrator password twice:' >&2
entry=$(htpasswd -nB -C 12 admin)
admin_hash=${entry#*:}
unset entry

bcrypt_pattern='^\$2[aby]\$12\$[./[:alnum:]]{53}$'
if [[ ! "$admin_hash" =~ $bcrypt_pattern ]]; then
  echo 'BCrypt hash generation failed' >&2
  exit 1
fi

cd "$script_dir/.."
{
  printf '\\set admin_email %s\n' "$admin_email"
  printf '\\set admin_hash %s\n' "$admin_hash"
  cat "$script_dir/bootstrap-admin.sql"
} | docker compose exec --no-TTY postgres sh -c \
  'psql -X --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1'
