CREATE TABLE users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_full_name_not_blank CHECK (btrim(full_name) <> ''),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email)) AND email <> ''),
    CONSTRAINT ck_users_password_hash_not_blank CHECK (btrim(password_hash) <> ''),
    CONSTRAINT ck_users_role CHECK (role IN ('CLIENT', 'ANALYST', 'ADMIN')),
    CONSTRAINT ck_users_timestamps CHECK (updated_at >= created_at)
);

CREATE TABLE analyst_coverages (
    analyst_id UUID NOT NULL,
    state VARCHAR(2) NOT NULL,

    CONSTRAINT pk_analyst_coverages PRIMARY KEY (analyst_id, state),
    CONSTRAINT fk_analyst_coverages_analyst
        FOREIGN KEY (analyst_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_analyst_coverages_state CHECK (
        state IN (
            'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO',
            'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI',
            'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
        )
    )
);

CREATE INDEX idx_analyst_coverages_state
    ON analyst_coverages (state);

CREATE TABLE solicitations (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL,

    service_type VARCHAR(30),
    title VARCHAR(80),
    description VARCHAR(1000),

    postal_code VARCHAR(8),
    street VARCHAR(150),
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(2),
    address_number VARCHAR(20),
    complement VARCHAR(120),

    priority VARCHAR(10),
    preferred_date DATE,
    estimated_value NUMERIC(19, 2),
    terms_accepted BOOLEAN,

    status VARCHAR(20) NOT NULL,
    current_step VARCHAR(20) NOT NULL,
    assigned_analyst_id UUID,

    decision_analyst_id UUID,
    decision_comment VARCHAR(1000),
    decided_at TIMESTAMPTZ,

    submitted_at TIMESTAMPTZ,
    review_started_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_solicitations_client
        FOREIGN KEY (client_id) REFERENCES users (id),
    CONSTRAINT fk_solicitations_assigned_analyst
        FOREIGN KEY (assigned_analyst_id) REFERENCES users (id),
    CONSTRAINT fk_solicitations_decision_analyst
        FOREIGN KEY (decision_analyst_id) REFERENCES users (id),

    CONSTRAINT ck_solicitations_service_type CHECK (
        service_type IS NULL
        OR service_type IN ('INSTALLATION', 'MAINTENANCE', 'INSPECTION')
    ),
    CONSTRAINT ck_solicitations_title CHECK (
        title IS NULL OR char_length(btrim(title)) BETWEEN 3 AND 80
    ),
    CONSTRAINT ck_solicitations_description CHECK (
        description IS NULL OR char_length(btrim(description)) BETWEEN 20 AND 1000
    ),
    CONSTRAINT ck_solicitations_postal_code CHECK (
        postal_code IS NULL OR postal_code ~ '^[0-9]{8}$'
    ),
    CONSTRAINT ck_solicitations_state CHECK (
        state IS NULL
        OR state IN (
            'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO',
            'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI',
            'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
        )
    ),
    CONSTRAINT ck_solicitations_address_number CHECK (
        address_number IS NULL OR char_length(btrim(address_number)) BETWEEN 1 AND 20
    ),
    CONSTRAINT ck_solicitations_priority CHECK (
        priority IS NULL OR priority IN ('LOW', 'MEDIUM', 'HIGH')
    ),
    CONSTRAINT ck_solicitations_estimated_value CHECK (
        estimated_value IS NULL OR estimated_value >= 0
    ),
    CONSTRAINT ck_solicitations_high_priority_value CHECK (
        priority IS NULL
        OR priority <> 'HIGH'
        OR (estimated_value IS NOT NULL AND estimated_value >= 100.00)
    ),
    CONSTRAINT ck_solicitations_terms_accepted CHECK (
        terms_accepted IS NULL OR terms_accepted = TRUE
    ),
    CONSTRAINT ck_solicitations_status CHECK (
        status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT ck_solicitations_current_step CHECK (
        current_step IN ('BASIC_DATA', 'ADDRESS', 'CONFIRMATION', 'COMPLETED')
    ),
    CONSTRAINT ck_solicitations_basic_data_step CHECK (
        current_step = 'BASIC_DATA'
        OR (service_type IS NOT NULL AND title IS NOT NULL AND description IS NOT NULL)
    ),
    CONSTRAINT ck_solicitations_address_step CHECK (
        current_step NOT IN ('CONFIRMATION', 'COMPLETED')
        OR (
            postal_code IS NOT NULL
            AND street IS NOT NULL
            AND neighborhood IS NOT NULL
            AND city IS NOT NULL
            AND state IS NOT NULL
            AND address_number IS NOT NULL
        )
    ),
    CONSTRAINT ck_solicitations_confirmation_step CHECK (
        current_step <> 'COMPLETED'
        OR (
            priority IS NOT NULL
            AND preferred_date IS NOT NULL
            AND terms_accepted = TRUE
        )
    ),
    CONSTRAINT ck_solicitations_submitted_complete CHECK (
        status = 'DRAFT'
        OR (current_step = 'COMPLETED' AND submitted_at IS NOT NULL)
    ),
    CONSTRAINT ck_solicitations_review CHECK (
        status NOT IN ('IN_REVIEW', 'APPROVED', 'REJECTED')
        OR (assigned_analyst_id IS NOT NULL AND review_started_at IS NOT NULL)
    ),
    CONSTRAINT ck_solicitations_decision CHECK (
        status NOT IN ('APPROVED', 'REJECTED')
        OR (
            decision_analyst_id IS NOT NULL
            AND decision_analyst_id = assigned_analyst_id
            AND decision_comment IS NOT NULL
            AND char_length(btrim(decision_comment)) BETWEEN 10 AND 1000
            AND decided_at IS NOT NULL
        )
    ),
    CONSTRAINT ck_solicitations_timestamps CHECK (updated_at >= created_at)
);

CREATE INDEX idx_solicitations_client_created_at
    ON solicitations (client_id, created_at DESC);

CREATE INDEX idx_solicitations_state_status
    ON solicitations (state, status);

CREATE INDEX idx_solicitations_assigned_analyst
    ON solicitations (assigned_analyst_id)
    WHERE assigned_analyst_id IS NOT NULL;

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID,
    action VARCHAR(40) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_id UUID,
    outcome VARCHAR(10) NOT NULL,
    request_id VARCHAR(100),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_audit_logs_action CHECK (
        action IN (
            'USER_CREATED',
            'ANALYST_STATES_ASSIGNED',
            'SOLICITATION_CREATED',
            'BASIC_DATA_SAVED',
            'ADDRESS_SAVED',
            'CONFIRMATION_SAVED',
            'SOLICITATION_SUBMITTED',
            'REVIEW_STARTED',
            'SOLICITATION_APPROVED',
            'SOLICITATION_REJECTED'
        )
    ),
    CONSTRAINT ck_audit_logs_resource_type CHECK (
        resource_type IN ('USER', 'SOLICITATION')
    ),
    CONSTRAINT ck_audit_logs_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT ck_audit_logs_metadata_object CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX idx_audit_logs_actor_occurred_at
    ON audit_logs (actor_id, occurred_at DESC)
    WHERE actor_id IS NOT NULL;

CREATE INDEX idx_audit_logs_resource_occurred_at
    ON audit_logs (resource_type, resource_id, occurred_at DESC)
    WHERE resource_id IS NOT NULL;

CREATE INDEX idx_audit_logs_occurred_at
    ON audit_logs (occurred_at DESC);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_outbox_events_event_type CHECK (
        event_type IN ('SOLICITATION_INDEX_REQUESTED')
    ),
    CONSTRAINT ck_outbox_events_payload_object CHECK (jsonb_typeof(payload) = 'object'),
    CONSTRAINT ck_outbox_events_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_outbox_events_processed_at CHECK (
        processed_at IS NULL OR processed_at >= occurred_at
    ),
    CONSTRAINT ck_outbox_events_processed_error CHECK (
        processed_at IS NULL OR last_error IS NULL
    )
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (occurred_at)
    WHERE processed_at IS NULL;

COMMENT ON TABLE users IS 'Authenticated users and their access roles.';
COMMENT ON TABLE analyst_coverages IS 'Federative units assigned to analysts.';
COMMENT ON TABLE solicitations IS 'Client solicitations and their complete lifecycle.';
COMMENT ON TABLE audit_logs IS 'Immutable audit trail for relevant application actions.';
COMMENT ON TABLE outbox_events IS 'Transactional events awaiting Elasticsearch indexing.';
