-- Guarda o pagamento do evento para o job diário reprocessar webhooks que falharam
-- (o Asaas recebeu 200 e não reenvia). Tabela de plataforma, sem RLS; GRANT da V130 continua valendo.
ALTER TABLE billing_webhook_events ADD COLUMN IF NOT EXISTS payment_id VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_billing_webhook_events_pending
    ON billing_webhook_events (received_at) WHERE processed_at IS NULL;
