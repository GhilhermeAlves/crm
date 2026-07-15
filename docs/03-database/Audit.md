# Audit — Auditoria no Banco de Dados

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Implementação](#implementação)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a estratégia de auditoria no banco de dados.

## Descrição

Auditoria registra todas as mudanças em dados sensíveis. Usa tabela dedicada `audit_logs` com dados antes/depois da mudança.

## Implementação

### Tabela audit_logs

| Coluna | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador |
| company_id | UUID | Tenant |
| user_id | UUID | Usuário que fez a ação |
| action | VARCHAR(20) | CREATE, UPDATE, DELETE |
| entity_type | VARCHAR(100) | Tipo da entidade |
| entity_id | UUID | ID da entidade |
| old_values | JSONB | Dados antes da mudança |
| new_values | JSONB | Dados depois da mudança |
| ip_address | INET | IP do usuário |
| user_agent | TEXT | Browser/app |
| created_at | TIMESTAMP | Data da ação |

### Trigger

```sql
CREATE OR REPLACE FUNCTION audit_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_logs (company_id, user_id, action, entity_type, entity_id, old_values, new_values)
    VALUES (
        current_setting('app.current_company_id')::uuid,
        current_setting('app.current_user_id')::uuid,
        TG_OP,
        TG_TABLE_NAME,
        CASE WHEN TG_OP = 'DELETE' THEN OLD.id ELSE NEW.id END,
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) ELSE NULL END
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;
```

## Responsabilidades

- Registrar todas as mudanças em dados sensíveis
- Manter log imutável de auditoria
- Suportar consultas por entidade, usuário e período
- Garantir compliance (LGPD, SOC2)

## Dependências

- [01-backend/Audit.md](../01-backend/Audit.md) — Lógica de auditoria
- [Entities.md](./Entities.md) — Tabelas auditadas

## Regras

- Tabelas auditadas: users, contacts, leads, opportunities, campaigns, automations
- Logs são imutáveis (nunca DELETE/UPDATE)
- Retenção mínima: 5 anos
- Dados sensíveis são mascarados (CPF, email parcial)
- Logs podem ser exportados para storage frio

## Futuras Melhorias

- Particionamento da tabela de auditoria
- Indexação otimizada para consultas
- Integração com SIEM
- Dashboard de auditoria
- Alertas para ações suspeitas

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
