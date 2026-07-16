ALTER TABLE users ADD COLUMN first_name VARCHAR(255);
ALTER TABLE users ADD COLUMN last_name VARCHAR(255);
ALTER TABLE users ADD COLUMN phone VARCHAR(30);
ALTER TABLE users ADD COLUMN department VARCHAR(255);
ALTER TABLE users ADD COLUMN job_title VARCHAR(255);
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(512);
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN language VARCHAR(10) NOT NULL DEFAULT 'pt-BR';
ALTER TABLE users ADD COLUMN timezone VARCHAR(100) NOT NULL DEFAULT 'America/Sao_Paulo';
ALTER TABLE users ADD COLUMN notes TEXT;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN invite_token VARCHAR(500) NULL;
ALTER TABLE users ADD COLUMN invited_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN invited_by UUID NULL;

UPDATE users SET first_name = name WHERE first_name IS NULL;

ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN name DROP NOT NULL;

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_company_status ON users(company_id, status);
CREATE INDEX idx_users_invite_token ON users(invite_token);
