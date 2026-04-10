CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_app_user_username_not_blank CHECK (btrim(username) <> ''),
    CONSTRAINT chk_app_user_password_hash_not_blank CHECK (btrim(password_hash) <> ''),
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_app_user_username ON app_user (username);
CREATE INDEX idx_app_user_created_at ON app_user (created_at DESC);
