\set ON_ERROR_STOP on

BEGIN;

INSERT INTO users (
    id, full_name, email, password_hash, role, active,
    created_at, updated_at, version
)
VALUES
    ('90000000-0000-0000-0000-000000000001', 'Cliente de Teste',
     'cliente.teste@example.com',
     '$2y$12$P3jyRB5RDj.JbhUCVY7GZ.ZS1LqvT0Eb8SDYzN3LkTCR9oTYDWki2',
     'CLIENT', TRUE, now(), now(), 0),
    ('90000000-0000-0000-0000-000000000002', 'Analista de Teste',
     'analista.teste@example.com',
     '$2y$12$P3jyRB5RDj.JbhUCVY7GZ.ZS1LqvT0Eb8SDYzN3LkTCR9oTYDWki2',
     'ANALYST', TRUE, now(), now(), 0),
    ('90000000-0000-0000-0000-000000000003', 'Administrador de Teste',
     'admin.teste@example.com',
     '$2y$12$P3jyRB5RDj.JbhUCVY7GZ.ZS1LqvT0Eb8SDYzN3LkTCR9oTYDWki2',
     'ADMIN', TRUE, now(), now(), 0)
ON CONFLICT (email) DO NOTHING;

INSERT INTO analyst_coverages (analyst_id, state)
SELECT id, state
FROM users
CROSS JOIN (VALUES ('SP'), ('RJ')) AS coverage(state)
WHERE email = 'analista.teste@example.com'
  AND role = 'ANALYST'
ON CONFLICT DO NOTHING;

COMMIT;

SELECT email, role FROM users
WHERE email IN (
    'cliente.teste@example.com',
    'analista.teste@example.com',
    'admin.teste@example.com'
)
ORDER BY role;
