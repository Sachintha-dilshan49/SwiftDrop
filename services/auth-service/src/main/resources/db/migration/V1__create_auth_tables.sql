
-- V1: Initial authentication and user management schema

-- 1. Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(30),
    password_hash VARCHAR(255),
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_account_status
        CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_users_email_lower
    ON users (LOWER(email));

-- 2. Supported roles
CREATE TABLE roles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,

    CONSTRAINT chk_roles_name
        CHECK (name IN (
            'CUSTOMER',
            'MERCHANT',
            'COURIER',
            'DISPATCHER',
            'ADMIN'
        ))
);

INSERT INTO roles (name)
VALUES
    ('CUSTOMER'),
    ('MERCHANT'),
    ('COURIER'),
    ('DISPATCHER'),
    ('ADMIN');

-- 3. Role assignments
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    assignment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users(id),

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT chk_user_roles_status
        CHECK (assignment_status IN (
            'PENDING',
            'ACTIVE',
            'REJECTED',
            'SUSPENDED'
        ))
);

CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- 4. Refresh tokens: store only token hashes, never raw tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_tokens(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 5. External OAuth accounts, including Google
CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_oauth_provider_account
        UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);

-- 6. Merchant registration details
CREATE TABLE merchant_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    business_name VARCHAR(200) NOT NULL,
    business_registration_number VARCHAR(100) NOT NULL UNIQUE,
    business_category VARCHAR(100) NOT NULL,
    pickup_location TEXT NOT NULL,
    contact_person VARCHAR(150) NOT NULL,
    br_certificate_path TEXT,
    owner_nic_path TEXT,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_merchant_approval_status
        CHECK (approval_status IN (
            'PENDING', 'APPROVED', 'REJECTED'
        ))
);

-- 7. Courier registration details
CREATE TABLE courier_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    nic_number VARCHAR(30) NOT NULL UNIQUE,
    date_of_birth DATE NOT NULL,
    delivery_zones TEXT NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    nic_document_path TEXT,
    licence_document_path TEXT,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_courier_approval_status
        CHECK (approval_status IN (
            'PENDING', 'APPROVED', 'REJECTED'
        ))
);
