-- V139__card_fees.sql
-- Simulador de vendas no crédito.
--   card_fee_rates        taxas da maquininha de cada clínica (1x a 12x), RLS por empresa.
--   card_fee_presets      modelos de taxa das maquininhas do mercado, globais; só o dono
--   card_fee_preset_rates do CRM altera (checado no backend). Servem de ponto de partida.

CREATE TABLE card_fee_rates (
    id            UUID PRIMARY KEY,
    company_id    UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    installments  SMALLINT NOT NULL CHECK (installments BETWEEN 1 AND 12),
    rate_percent  NUMERIC(5,2) NOT NULL CHECK (rate_percent >= 0 AND rate_percent < 100),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_card_fee_rates_company_installments UNIQUE (company_id, installments)
);

ALTER TABLE card_fee_rates ENABLE ROW LEVEL SECURITY;
ALTER TABLE card_fee_rates FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON card_fee_rates
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON card_fee_rates TO crm_app;

CREATE TABLE card_fee_presets (
    id           UUID PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    estimated    BOOLEAN NOT NULL DEFAULT FALSE,
    checked_at   DATE NULL,
    source_note  VARCHAR(500) NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_card_fee_presets_name ON card_fee_presets (lower(name));

CREATE TABLE card_fee_preset_rates (
    preset_id     UUID NOT NULL REFERENCES card_fee_presets(id) ON DELETE CASCADE,
    installments  SMALLINT NOT NULL CHECK (installments BETWEEN 1 AND 12),
    rate_percent  NUMERIC(5,2) NOT NULL CHECK (rate_percent >= 0 AND rate_percent < 100),
    PRIMARY KEY (preset_id, installments)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON card_fee_presets TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON card_fee_preset_rates TO crm_app;

-- Carga inicial: valores publicados de 1x e 12x (comparador maquinatop.com.br, conferido
-- em 13/11/2026, faixa de menor faturamento após a promoção de entrada). As parcelas de
-- 2x a 11x não são publicadas: estimadas em linha reta entre 1x e 12x (estimated = true).
WITH seed(name, t1, t12) AS (
    VALUES
        ('InfinitePay — 1 dia útil — até R$ 20 mil/mês', 3.15, 12.40),
        ('InfinitePay — na hora (Nitro)',                5.99, 18.79),
        ('SumUp — 1 dia útil — até R$ 20 mil/mês',       3.49, 13.99),
        ('PagBank — na hora',                            4.99, 16.92),
        ('Cielo — tabela padrão',                        4.30, 19.00),
        ('Mercado Pago — na hora — até R$ 3 mil/mês',    3.86, 20.39),
        ('Ton — 1 dia útil — até R$ 3 mil/mês',          3.86, 21.39),
        ('Ton — na hora — até R$ 3 mil/mês',             4.86, 21.99)
), presets AS (
    INSERT INTO card_fee_presets (id, name, estimated, checked_at, source_note)
    SELECT gen_random_uuid(), name, TRUE, DATE '2026-11-13',
           'Taxas de 1x e 12x publicadas; 2x a 11x estimadas. Confirme com a sua maquininha.'
    FROM seed
    RETURNING id, name
)
INSERT INTO card_fee_preset_rates (preset_id, installments, rate_percent)
SELECT p.id, n, ROUND(s.t1 + (s.t12 - s.t1) * (n - 1) / 11.0, 2)
FROM presets p
JOIN seed s ON s.name = p.name
CROSS JOIN generate_series(1, 12) AS n;
