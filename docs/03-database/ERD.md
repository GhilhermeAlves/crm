# ERD — Diagrama Entidade-Relacionamento

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Diagrama](#diagrama)
- [Tabelas Principais](#tabelas-principais)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o diagrama entidade-relacionamento do banco de dados.

## Descrição

O ERD representa todas as entidades do sistema e seus relacionamentos. Será implementado com ferramenta visual (dbdiagram.io ou Mermaid).

## Diagrama

```
companies ──┐
            │
users ──────┤── company_id (FK)
            │
contacts ───┤── company_id (FK)
            │
leads ──────┤── company_id (FK)
            │── contact_id (FK)
            │
pipelines ──┤── company_id (FK)
            │
stages ─────┤── pipeline_id (FK)
            │
opportunities ─┤── pipeline_id (FK)
               │── stage_id (FK)
               │── contact_id (FK)
               │
conversations ─┤── company_id (FK)
               │── contact_id (FK)
               │
messages ──────┤── conversation_id (FK)
               │
campaigns ─────┤── company_id (FK)
               │
automations ───┤── company_id (FK)
               │
audit_logs ────┤── company_id (FK)
               │── user_id (FK)
```

## Tabelas Principais

### Identity

| Tabela | Descrição |
|---|---|
| users | Usuários do sistema |
| roles | Papéis/funções |
| user_roles | Usuários x Roles |
| refresh_tokens | Tokens de refresh |

### Company

| Tabela | Descrição |
|---|---|
| companies | Empresas (tenants) |
| company_settings | Configurações |
| subscriptions | Planos de assinatura |

### Contact

| Tabela | Descrição |
|---|---|
| contacts | Contatos |
| contact_addresses | Endereços |
| contact_custom_fields | Campos customizados |
| tags | Etiquetas |
| contact_tags | Contatos x Tags |

### Pipeline

| Tabela | Descrição |
|---|---|
| pipelines | Pipelines |
| stages | Estágios |
| opportunities | Oportunidades |
| opportunity_history | Histórico |

### Communication

| Tabela | Descrição |
|---|---|
| conversations | Conversas |
| messages | Mensagens |
| message_templates | Templates |
| message_attachments | Anexos |

### Campaign

| Tabela | Descrição |
|---|---|
| campaigns | Campanhas |
| campaign_steps | Passos |
| automations | Automações |
| automation_triggers | Gatilhos |
| automation_actions | Ações |

### Audit

| Tabela | Descrição |
|---|---|
| audit_logs | Logs de auditoria |
| events | Event store |

## Responsabilidades

- Documentar todas as entidades e relacionamentos
- Servir de referência para implementação
- Facilitar revisões de schema

## Dependências

- [Entities.md](./Entities.md) — Detalhes das entidades
- [Relationships.md](./Relationships.md) — Detalhes dos relacionamentos

## Regras

- ERD deve ser mantido atualizado
- Mudanças de schema devem ser documentadas antes da migration
- Usar Mermaid para diagramas em código

## Futuras Melhorias

- ERD interativo online
- Auto-generation a partir do código
- Versionamento do ERD
- Documentação de colunas com exemplos

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
