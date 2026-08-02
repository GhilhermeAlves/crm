-- V012: Add DEFAULT gen_random_uuid() to companies.id for existing environments
-- This ensures backward compatibility for databases where V005 already ran without the default

ALTER TABLE companies
    ALTER COLUMN id SET DEFAULT gen_random_uuid();
