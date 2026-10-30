CREATE TABLE merchants (
    id                  UUID PRIMARY KEY,
    user_id             UUID UNIQUE,
    business_name       VARCHAR(200) NOT NULL,
    business_reg_no     VARCHAR(100) NOT NULL UNIQUE,
    contact_name        VARCHAR(150) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    phone               VARCHAR(30)  NOT NULL,
    address             VARCHAR(500) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason    VARCHAR(500),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ
);

CREATE INDEX idx_merchants_user_id ON merchants (user_id);
CREATE INDEX idx_merchants_status ON merchants (status);
CREATE INDEX idx_merchants_email ON merchants (email);
CREATE INDEX idx_merchants_reg_no ON merchants (business_reg_no);
