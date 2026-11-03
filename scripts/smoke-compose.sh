#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "$0")/.." && pwd)
scratch_dir=$(mktemp -d)
project_name="sea-smoke-$(openssl rand -hex 6)"
smoke_image="sea-solicitations-api:${project_name}"
override_file="$scratch_dir/compose.override.yaml"

cat > "$override_file" <<EOF
services:
  app:
    image: $smoke_image
EOF

compose() {
  docker compose --project-name "$project_name" \
    --env-file "$repo_dir/.env.example" \
    -f "$repo_dir/compose.yaml" -f "$override_file" "$@"
}

cleanup() {
  local result=$?
  if ((result != 0)); then
    compose logs --tail=80 app postgres elasticsearch >&2 || true
  fi
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
  docker image rm "$smoke_image" >/dev/null 2>&1 || true
  rm -rf "$scratch_dir"
  exit "$result"
}
trap cleanup EXIT

read -r APP_PORT POSTGRES_PORT ELASTICSEARCH_PORT < <(python3 - <<'PY'
import socket
sockets = []
try:
    for _ in range(3):
        listener = socket.socket()
        listener.bind(("127.0.0.1", 0))
        sockets.append(listener)
    print(*(listener.getsockname()[1] for listener in sockets))
finally:
    for listener in sockets:
        listener.close()
PY
)
export APP_PORT POSTGRES_PORT ELASTICSEARCH_PORT
JWT_SECRET=$(openssl rand -base64 32)
export JWT_SECRET

compose up --build --wait --wait-timeout 240

smoke_admin_email="smoke-admin@example.com"
smoke_admin_password="StrongPassword@123"
smoke_admin_hash='$2y$12$dkJFoZrcdFg5JYwVpDninOZ.nictzKKQOObnm5HfiRpJYnRxsqFLC'
{
  printf '\\set admin_email %s\n' "$smoke_admin_email"
  printf '\\set admin_hash %s\n' "$smoke_admin_hash"
  cat "$repo_dir/scripts/bootstrap-admin.sql"
} | compose exec --no-TTY postgres sh -c \
  'psql -X --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1' \
  >/dev/null

python3 - "$APP_PORT" "$ELASTICSEARCH_PORT" "$smoke_admin_email" "$smoke_admin_password" <<'PY'
import json
import sys
import time
import urllib.error
import urllib.request
import uuid

base = f"http://127.0.0.1:{sys.argv[1]}"

def request(path, payload=None, token=None):
    body = None if payload is None else json.dumps(payload).encode()
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    call = urllib.request.Request(base + path, data=body, headers=headers)
    with urllib.request.urlopen(call, timeout=20) as response:
        return response.status, json.load(response)

status, health = request("/actuator/health")
assert status == 200 and health["status"] == "UP", health
status, api = request("/v3/api-docs")
assert status == 200 and api["info"]["title"] == "Sea Solicitations API"

email = f"smoke-{uuid.uuid4()}@example.com"
password = "StrongPassword@123"
status, registered = request("/auth/register", {
    "name": "Smoke Client", "email": email, "password": password
})
assert status == 201 and registered["id"]
status, logged_in = request("/auth/login", {
    "email": email, "password": password
})
assert status == 200 and logged_in["accessToken"]
client_metrics_call = urllib.request.Request(base + "/actuator/prometheus", headers={
    "Authorization": f"Bearer {logged_in['accessToken']}"
})
try:
    urllib.request.urlopen(client_metrics_call, timeout=20)
except urllib.error.HTTPError as error:
    assert error.code == 403, error
else:
    raise AssertionError("CLIENT unexpectedly accessed Prometheus metrics")

status, admin_logged_in = request("/auth/login", {
    "email": sys.argv[3], "password": sys.argv[4]
})
assert status == 200 and admin_logged_in["accessToken"]
admin_metrics_call = urllib.request.Request(base + "/actuator/prometheus", headers={
    "Authorization": f"Bearer {admin_logged_in['accessToken']}"
})
with urllib.request.urlopen(admin_metrics_call, timeout=20) as response:
    metrics = response.read().decode()
assert "sea_outbox_pending" in metrics
assert "sea_audit_write_failures_total" in metrics
status, solicitation = request("/solicitations", {
    "serviceType": "INSTALLATION",
    "title": "Instalacao de teste",
    "description": "Solicitacao criada pelo smoke test do Docker Compose."
}, logged_in["accessToken"])
assert status == 201 and solicitation["id"]

document_url = (
    f"http://127.0.0.1:{sys.argv[2]}/solicitations-v1/_doc/{solicitation['id']}"
)
deadline = time.monotonic() + 90
last_error = None
while time.monotonic() < deadline:
    try:
        with urllib.request.urlopen(document_url, timeout=5) as response:
            document = json.load(response)
        assert document["_source"]["status"] == "DRAFT", document
        assert document["_source"]["title"] == "Instalacao de teste", document
        break
    except urllib.error.HTTPError as error:
        if error.code not in (404, 429, 502, 503, 504):
            raise
        last_error = error
    except (urllib.error.URLError, TimeoutError) as error:
        last_error = error
    time.sleep(1)
else:
    raise AssertionError(
        f"Solicitation was not indexed by the outbox within 90 seconds: {last_error}"
    )

print("Compose smoke passed: health, OpenAPI, registration, role-protected metrics, solicitation and Elasticsearch indexing")
PY
