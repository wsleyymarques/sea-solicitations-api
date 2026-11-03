# Flyer Mandatory Contract Alignment Design

## Intent

Align the public HTTP contract with the requirements explicitly stated in the backend challenge flyer, without adding inferred client endpoints or optional features.

## Goals

- Make `q` the text-search parameter on `GET /analyst/solicitations/search`.
- Return search results as exactly `items`, `page`, `size`, and `total`.
- Accept decision commands `APPROVE` and `REJECT` while preserving terminal solicitation statuses `APPROVED` and `REJECTED`.
- Synchronize automated tests, README, frontend test console, and Postman examples with that contract.

## Non-goals

- Add client draft retrieval or listing endpoints.
- Expand the OpenAPI scope beyond what follows automatically from the contract changes.
- Remove unrelated backwards-compatible date, sort, or decision-path aliases.

## Approach

The flyer contract becomes canonical at the web boundary. Internal search result types may keep their existing domain-oriented names; the response DTO performs the boundary mapping. Decision action names change in the input enum, and the service maps them to the existing terminal statuses.

## Constraints

- Preserve role, owner, and UF authorization behavior.
- Preserve pagination and Elasticsearch behavior.
- Preserve `APPROVED` and `REJECTED` as persisted and returned solicitation statuses.
- Implement each behavior test-first.
