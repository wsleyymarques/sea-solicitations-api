\set ON_ERROR_STOP on

BEGIN;

-- Keep the first-admin check and insert together if two operators run the seed.
LOCK TABLE users IN EXCLUSIVE MODE;

INSERT INTO users (
    id,
    full_name,
    email,
    password_hash,
    role,
    active,
    created_at,
    updated_at,
    version
)
SELECT
    gen_random_uuid(),
    'Local Administrator',
    lower(btrim(:'admin_email')),
    :'admin_hash',
    'ADMIN',
    TRUE,
    now(),
    now(),
    0
WHERE NOT EXISTS (SELECT 1 FROM users WHERE role = 'ADMIN');

COMMIT;

SELECT id, email, role FROM users WHERE role = 'ADMIN' ORDER BY created_at;
