-- V119__anamnesis_open_links.sql
-- Link aberto de um modelo de anamnese: um endereço fixo que a clínica manda
-- por qualquer canal. Quem responde informa nome e telefone; o backend acha o
-- paciente pelo telefone (ou cadastra) e grava a resposta como um formulário
-- comum (patient_form_requests).
--
-- O token aqui não é segredo de uma pessoa — é um link público para divulgar —
-- e por isso fica em texto puro (a tela mostra o link de novo). A rota pública
-- reaproveita o GUC app.form_token (V115) para liberar só a linha do token.

CREATE TABLE anamnesis_open_links (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    model_id    UUID NOT NULL REFERENCES anamnesis_models(id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL,
    created_by  UUID,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_anamnesis_open_links_token ON anamnesis_open_links (token);
CREATE UNIQUE INDEX uq_anamnesis_open_links_model ON anamnesis_open_links (model_id);

ALTER TABLE anamnesis_open_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE anamnesis_open_links FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON anamnesis_open_links
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON anamnesis_open_links TO crm_app;

CREATE POLICY open_link_token_select_policy ON anamnesis_open_links
    FOR SELECT USING (token = app.current_form_token());
