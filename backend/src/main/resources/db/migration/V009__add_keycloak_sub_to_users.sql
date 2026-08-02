ALTER TABLE users ADD COLUMN IF NOT EXISTS keycloak_sub VARCHAR(255) NULL;
CREATE INDEX IF NOT EXISTS idx_users_keycloak_sub ON users (keycloak_sub);
