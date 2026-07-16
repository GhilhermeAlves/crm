CREATE TABLE companies (
    id                       UUID PRIMARY KEY,
    legal_name               VARCHAR(255) NOT NULL,
    trading_name             VARCHAR(255) NOT NULL,
    cnpj                     VARCHAR(20)  NOT NULL,
    state_registration       VARCHAR(50)  NULL,
    municipal_registration   VARCHAR(50)  NULL,
    email                    VARCHAR(255) NOT NULL,
    phone                    VARCHAR(20)  NOT NULL,
    website                  VARCHAR(255) NULL,
    address_zip_code         VARCHAR(10)  NOT NULL,
    address_street           VARCHAR(255) NOT NULL,
    address_number           VARCHAR(20)  NOT NULL,
    address_complement       VARCHAR(255) NULL,
    address_neighborhood     VARCHAR(255) NOT NULL,
    address_city             VARCHAR(255) NOT NULL,
    address_state            VARCHAR(2)   NOT NULL,
    address_country          VARCHAR(10)  NOT NULL DEFAULT 'Brasil',
    plan                     VARCHAR(20)  NOT NULL DEFAULT 'STARTER',
    status                   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    max_users                INTEGER      NOT NULL DEFAULT 5,
    max_storage_mb           INTEGER      NOT NULL DEFAULT 1024,
    logo_url                 VARCHAR(512) NULL,
    notes                    TEXT         NULL,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_companies_cnpj ON companies (cnpj);
CREATE UNIQUE INDEX idx_companies_email ON companies (email);
