-- V015__contacts_tables.sql
-- Contacts, addresses, tags, custom fields tables

CREATE TABLE contacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100),
    email           VARCHAR(255),
    phone           VARCHAR(20),
    company_name    VARCHAR(200),
    notes           TEXT,
    avatar_url      TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE TABLE contact_addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    street          VARCHAR(255),
    number          VARCHAR(20),
    complement      VARCHAR(100),
    neighborhood    VARCHAR(100),
    city            VARCHAR(100),
    state           VARCHAR(50),
    zip_code        VARCHAR(10),
    country         VARCHAR(50) DEFAULT 'BR',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contact_custom_fields (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    field_key       VARCHAR(100) NOT NULL,
    field_value     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tags (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    color           VARCHAR(7) NOT NULL DEFAULT '#6B7280',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contact_tags (
    contact_id      UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    tag_id          UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (contact_id, tag_id)
);

CREATE INDEX idx_contacts_company_id ON contacts (company_id);
CREATE INDEX idx_contacts_email ON contacts (email);
CREATE INDEX idx_contacts_deleted_at ON contacts (deleted_at);
CREATE INDEX idx_contact_addresses_contact_id ON contact_addresses (contact_id);
CREATE INDEX idx_contact_custom_fields_contact_id ON contact_custom_fields (contact_id);
CREATE INDEX idx_tags_company_id ON tags (company_id);
CREATE INDEX idx_contact_tags_contact_id ON contact_tags (contact_id);
CREATE INDEX idx_contact_tags_tag_id ON contact_tags (tag_id);

CREATE UNIQUE INDEX uk_contacts_email_company ON contacts (email, company_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_tags_name_company ON tags (name, company_id);
