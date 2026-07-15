# Indexes — Índices

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Índices por Tabela](#índices-por-tabela)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todos os índices do banco de dados, incluindo justificativa e impacto.

## Descrição

Índices são essenciais para performance de queries. Cada índice é documentado com a query que o utiliza e a justificativa.

## Índices por Tabela

### users

| Índice | Colunas | Tipo | Justificativa |
|---|---|---|---|
| uk_users_email | email | UNIQUE | Login por email |
| idx_users_company_id | company_id | BTREE | Busca por empresa |
| idx_users_status | status | BTREE | Filtro por status |

### contacts

| Índice | Colunas | Tipo | Justificativa |
|---|---|---|---|
| idx_contacts_company_id | company_id | BTREE | Busca por empresa |
| idx_contacts_email | company_id, email | BTREE | Busca por email na empresa |
| idx_contacts_phone | company_id, phone | BTREE | Busca por telefone |
| idx_contacts_created_at | company_id, created_at | BTREE | Ordenação por data |

### leads

| Índice | Colunas | Tipo | Justificativa |
|---|---|---|---|
| idx_leads_company_id | company_id | BTREE | Busca por empresa |
| idx_leads_status | company_id, status | BTREE | Filtro por status |
| idx_leads_score | company_id, score | BTREE | Ordenação por score |
| idx_leads_owner_id | owner_id | BTREE | Busca por responsável |

### opportunities

| Índice | Colunas | Tipo | Justificativa |
|---|---|---|---|
| idx_opportunities_company_id | company_id | BTREE | Busca por empresa |
| idx_opportunities_pipeline_stage | pipeline_id, stage_id | BTREE | Kanban |
| idx_opportunities_status | company_id, status | BTREE | Filtro por status |
| idx_opportunities_value | company_id, value | BTREE | Ordenação por valor |

### conversations

| Índice | Colunas | Tipo | Justificativa |
|---|---|---|---|
| idx_conversations_company_id | company_id | BTREE | Busca por empresa |
| idx_conversations_contact_id | contact_id | BTREE | Busca por contato |
| idx_conversations_status | company_id, status | BTREE | Filtro por status |

### messages

| Índice | Colunas | Tipo | Justificativa |
|---|---|---|---|
| idx_messages_conversation_id | conversation_id | BTREE | Mensagens da conversa |
| idx_messages_created_at | conversation_id, created_at | BTREE | Ordenação cronológica |
| idx_messages_external_id | external_id | BTREE | Busca por ID externo |

### audit_logs

| Índice | Colunas | Tipo | Justificativa |
|---|---|---|---|
| idx_audit_company_id | company_id | BTREE | Busca por empresa |
| idx_audit_user_id | user_id | BTREE | Busca por usuário |
| idx_audit_entity | entity_type, entity_id | BTREE | Histórico de entidade |
| idx_audit_created_at | company_id, created_at | BTREE | Ordenação por data |

## Responsabilidades

- Criar índices baseados em queries reais
- Monitorar uso de índices
- Remover índices não utilizados
- Testar impacto em staging

## Dependências

- [Entities.md](./Entities.md) — Tabelas
- [Performance.md](./Performance.md) — Otimização

## Regras

- Toda FK deve ter índice automático
- Índices compostos seguem ordem das colunas na query
- UNIQUE constraints criam índice automaticamente
- Índices devem ser testados com EXPLAIN ANALYZE
- Não criar índices redundantes

## Futuras Melhorias

- pg_stat_user_indexes para monitoramento
- Índices parciais para queries frequentes
- Índices GIN para JSONB
- Auto-análise de queries lentas

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
