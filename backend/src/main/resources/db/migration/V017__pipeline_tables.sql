-- V017__pipeline_tables.sql

CREATE TABLE pipelines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id     UUID NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    color           VARCHAR(7),
    "order"         INTEGER NOT NULL,
    probability     INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE TABLE opportunities (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title               VARCHAR(200) NOT NULL,
    value               DECIMAL(15, 2) NOT NULL,
    contact_id          UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    pipeline_id         UUID NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    stage_id            UUID NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
    assigned_to         UUID REFERENCES users(id) ON DELETE SET NULL,
    expected_close_date TIMESTAMP,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    won_at              TIMESTAMP,
    lost_at             TIMESTAMP,
    loss_reason         TEXT,
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE opportunity_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id  UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    from_stage_id   UUID REFERENCES stages(id) ON DELETE SET NULL,
    to_stage_id     UUID NOT NULL REFERENCES stages(id) ON DELETE CASCADE,
    changed_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    changed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note            TEXT
);

CREATE INDEX idx_pipelines_company_id ON pipelines (company_id);
CREATE INDEX idx_stages_pipeline_id ON stages (pipeline_id);
CREATE INDEX idx_stages_company_id ON stages (company_id);
CREATE INDEX idx_stages_order ON stages (pipeline_id, "order");
CREATE INDEX idx_opportunities_company_id ON opportunities (company_id);
CREATE INDEX idx_opportunities_pipeline_id ON opportunities (pipeline_id);
CREATE INDEX idx_opportunities_stage_id ON opportunities (stage_id);
CREATE INDEX idx_opportunities_status ON opportunities (status);
CREATE INDEX idx_opportunities_assigned_to ON opportunities (assigned_to);
CREATE INDEX idx_opportunity_history_opportunity_id ON opportunity_history (opportunity_id);

ALTER TABLE stages ADD CONSTRAINT chk_stages_probability CHECK (probability >= 0 AND probability <= 100);
ALTER TABLE stages ADD CONSTRAINT chk_stages_order CHECK ("order" > 0);
ALTER TABLE opportunities ADD CONSTRAINT chk_opportunities_value CHECK (value > 0);
ALTER TABLE opportunities ADD CONSTRAINT chk_opportunities_status CHECK (status IN ('OPEN', 'WON', 'LOST'));
