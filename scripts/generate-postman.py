#!/usr/bin/env python3
"""Gera a coleção Postman do fluxo HTTP local a partir dos contratos da API."""

import json
from pathlib import Path
from urllib.parse import parse_qsl, urlsplit


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "postman"
OUT.mkdir(exist_ok=True)


def script(lines):
    return [{"listen": "test", "script": {"type": "text/javascript", "exec": lines}}]


def check(status, extra=None):
    return script(
        ["pm.test('HTTP " + str(status) + "', () => pm.response.to.have.status(" + str(status) + "));"]
        + (extra or [])
    )


def request(name, method, path, *, body=None, token=None, events=None, prerequest=None):
    parsed = urlsplit(path)
    headers = []
    if body is not None:
        headers.append({"key": "Content-Type", "value": "application/json"})
    if token:
        headers.append({"key": "Authorization", "value": "Bearer {{" + token + "}}"})
    item = {
        "name": name,
        "request": {
            "method": method,
            "header": headers,
            "url": {
                "raw": "{{baseUrl}}" + path,
                "host": ["{{baseUrl}}"],
                "path": parsed.path.lstrip("/").split("/"),
                "query": [{"key": key, "value": value} for key, value in parse_qsl(parsed.query)],
            },
        },
        "response": [],
    }
    if body is not None:
        item["request"]["body"] = {"mode": "raw", "raw": body, "options": {"raw": {"language": "json"}}}
    if events:
        item["event"] = events
    if prerequest:
        item.setdefault("event", []).append(
            {"listen": "prerequest", "script": {"type": "text/javascript", "exec": prerequest}}
        )
    return item


collection = {
    "info": {
        "name": "Sea Solicitações — fluxo local",
        "_postman_id": "0f718bdd-76cf-4ec5-bba7-e62aeb47add4",
        "description": "Execute em ordem após iniciar a API e criar o primeiro ADMIN. A busca pode levar alguns segundos para refletir a indexação assíncrona.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "item": [
        request("00 · Saúde da API", "GET", "/actuator/health", events=check(200)),
        request(
            "01 · Cadastrar cliente", "POST", "/auth/register",
            body='{"name":"Cliente Postman","email":"{{clientEmail}}","password":"{{demoPassword}}"}',
            prerequest=["pm.collectionVariables.set('clientEmail', 'sea.client.' + Date.now() + '@example.com');"],
            events=check(201),
        ),
        request(
            "02 · Login cliente", "POST", "/auth/login",
            body='{"email":"{{clientEmail}}","password":"{{demoPassword}}"}',
            events=check(200, ["pm.collectionVariables.set('clientToken', pm.response.json().accessToken);"]),
        ),
        request(
            "03 · Login administrador", "POST", "/auth/login",
            body='{"email":"{{adminEmail}}","password":"{{adminPassword}}"}',
            events=check(200, ["pm.collectionVariables.set('adminToken', pm.response.json().accessToken);"]),
        ),
        request(
            "04 · Criar analista", "POST", "/admin/users", token="adminToken",
            body='{"name":"Analista Postman","email":"{{analystEmail}}","password":"{{demoPassword}}","role":"ANALYST","coveredStates":["SP","RJ"]}',
            prerequest=["pm.collectionVariables.set('analystEmail', 'sea.analyst.' + Date.now() + '@example.com');"],
            events=check(201, ["pm.collectionVariables.set('analystId', pm.response.json().id);"]),
        ),
        request(
            "05 · Login analista", "POST", "/auth/login",
            body='{"email":"{{analystEmail}}","password":"{{demoPassword}}"}',
            events=check(200, ["pm.collectionVariables.set('analystToken', pm.response.json().accessToken);"]),
        ),
        request(
            "06 · Criar solicitação", "POST", "/solicitations", token="clientToken",
            body='{"serviceType":"INSTALLATION","title":"Instalação de equipamento","description":"Solicito a instalação de um novo equipamento no endereço informado."}',
            events=check(201, ["pm.collectionVariables.set('solicitationId', pm.response.json().id);"]),
        ),
        request(
            "07 · Completar endereço", "PUT", "/solicitations/{{solicitationId}}/address", token="clientToken",
            body='{"postalCode":"01001-000","number":"100","complement":"Apto 12"}',
            events=check(200),
        ),
        request(
            "08 · Completar confirmação", "PUT", "/solicitations/{{solicitationId}}/confirmation", token="clientToken",
            body='{"priority":"HIGH","preferredDate":"2099-01-01","estimatedValue":100.00,"termsAccepted":true}',
            events=check(200),
        ),
        request(
            "09 · Enviar solicitação", "POST", "/solicitations/{{solicitationId}}/submit", token="clientToken",
            events=check(200, ["pm.test('Submetida', () => pm.expect(pm.response.json().status).to.eql('SUBMITTED'));"]),
        ),
        request(
            "10 · Listar para analista", "GET", "/analyst/solicitations?page=0&size=20", token="analystToken",
            events=check(200),
        ),
        request(
            "11 · Consultar detalhe como analista", "GET",
            "/analyst/solicitations/{{solicitationId}}", token="analystToken", events=check(200),
        ),
        request(
            "12 · Buscar no Elasticsearch", "GET",
            "/analyst/solicitations/search?state=SP&dateFrom=2020-01-01&dateTo=2099-12-31&sort=SUBMITTED_AT&direction=DESC&page=0&size=20",
            token="analystToken", events=check(200),
        ),
        request(
            "13 · Iniciar análise", "POST", "/analyst/solicitations/{{solicitationId}}/start",
            token="analystToken",
            events=check(200, ["pm.test('Em análise', () => pm.expect(pm.response.json().status).to.eql('IN_REVIEW'));"]),
        ),
        request(
            "14 · Aprovar solicitação", "POST", "/analyst/solicitations/{{solicitationId}}/decide",
            token="analystToken",
            body='{"outcome":"APPROVED","comment":"Parecer favorável para atendimento."}',
            events=check(200, ["pm.test('Aprovada', () => pm.expect(pm.response.json().status).to.eql('APPROVED'));"]),
        ),
        request(
            "15 · Criar solicitação para rejeição", "POST", "/solicitations", token="clientToken",
            body='{"serviceType":"INSPECTION","title":"Inspeção de equipamento","description":"Solicito a inspeção de um equipamento no endereço informado."}',
            events=check(201, ["pm.collectionVariables.set('rejectSolicitationId', pm.response.json().id);"]),
        ),
        request(
            "16 · Completar endereço para rejeição", "PUT", "/solicitations/{{rejectSolicitationId}}/address",
            token="clientToken", body='{"postalCode":"01001-000","number":"200"}', events=check(200),
        ),
        request(
            "17 · Completar confirmação para rejeição", "PUT",
            "/solicitations/{{rejectSolicitationId}}/confirmation", token="clientToken",
            body='{"priority":"MEDIUM","preferredDate":"2099-01-01","termsAccepted":true}',
            events=check(200),
        ),
        request(
            "18 · Enviar solicitação para rejeição", "POST",
            "/solicitations/{{rejectSolicitationId}}/submit", token="clientToken", events=check(200),
        ),
        request(
            "19 · Iniciar análise para rejeição", "POST",
            "/analyst/solicitations/{{rejectSolicitationId}}/start", token="analystToken", events=check(200),
        ),
        request(
            "20 · Rejeitar solicitação", "POST",
            "/analyst/solicitations/{{rejectSolicitationId}}/decide", token="analystToken",
            body='{"outcome":"REJECTED","comment":"Parecer desfavorável após análise do pedido."}',
            events=check(200, ["pm.test('Rejeitada', () => pm.expect(pm.response.json().status).to.eql('REJECTED'));"]),
        ),
    ],
    "variable": [{"key": "demoPassword", "value": "StrongPassword@123"}],
}

environment = {
    "id": "ffcb7553-93ae-41f1-8b73-24f1223ca58e",
    "name": "Sea Solicitações · Local",
    "values": [
        {"key": "baseUrl", "value": "http://localhost:8080", "type": "default", "enabled": True},
        {"key": "adminEmail", "value": "", "type": "default", "enabled": True},
        {"key": "adminPassword", "value": "", "type": "secret", "enabled": True},
    ],
    "_postman_variable_scope": "environment",
    "_postman_exported_using": "Sea project",
}

(OUT / "sea-solicitacoes.postman_collection.json").write_text(
    json.dumps(collection, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
)
(OUT / "sea-local.postman_environment.json").write_text(
    json.dumps(environment, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
)
