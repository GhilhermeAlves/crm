-- Leads vindos do Instagram Direct (canal META_INSTAGRAM).
ALTER TABLE leads DROP CONSTRAINT IF EXISTS chk_leads_source;
ALTER TABLE leads ADD CONSTRAINT chk_leads_source
    CHECK (source IN ('WHATSAPP', 'INSTAGRAM', 'FORM', 'API', 'IMPORT', 'MANUAL'));
