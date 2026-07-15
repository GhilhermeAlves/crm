# Entities — Entidades Principais

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Tabelas por Bounded Context](#tabelas-por-bounded-context)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todas as tabelas do banco de dados, incluindo colunas, tipos e constraints.

## Descrição

Cada tabela é documentada com suas colunas, tipos de dados, constraints e triggers. As tabelas são organizadas por bounded context.

## Tabelas por Bounded Context

### Identity

#### users

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identificador único |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email do usuário |
| password_hash | VARCHAR(255) | NOT NULL | Hash da senha |
| first_name | VARCHAR(100) | NOT NULL | Nome |
| last_name | VARCHAR(100) | | Sobrenome |
| avatar_url | TEXT | | URL do avatar |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | Status |
| last_login_at | TIMESTAMP | | Último login |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Criação |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Atualização |
| deleted_at | TIMESTAMP | | Soft delete |

#### refresh_tokens

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| user_id | UUID | FK → users.id, NOT NULL | Usuário |
| token_hash | VARCHAR(255) | NOT NULL, UNIQUE | Hash do token |
| family | VARCHAR(255) | NOT NULL | Família do token |
| expires_at | TIMESTAMP | NOT NULL | Expiração |
| created_at | TIMESTAMP | NOT NULL | Criação |

### Company

#### companies

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| name | VARCHAR(200) | NOT NULL | Nome da empresa |
| schema_name | VARCHAR(100) | NOT NULL, UNIQUE | Nome do schema |
| logo_url | TEXT | | Logo |
| timezone | VARCHAR(50) | DEFAULT 'America/Sao_Paulo' | Fuso horário |
| language | VARCHAR(10) | DEFAULT 'pt-BR' | Idioma |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | Status |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |
| deleted_at | TIMESTAMP | | Soft delete |

#### company_settings

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL, UNIQUE | Empresa |
| setting_key | VARCHAR(100) | NOT NULL | Chave da configuração |
| setting_value | JSONB | | Valor da configuração |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |

### Contact

#### contacts

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| first_name | VARCHAR(100) | NOT NULL | Nome |
| last_name | VARCHAR(100) | | Sobrenome |
| email | VARCHAR(255) | | Email |
| phone | VARCHAR(20) | | Telefone (E.164) |
| company_name | VARCHAR(200) | | Empresa do contato |
| notes | TEXT | | Observações |
| avatar_url | TEXT | | Avatar |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |
| deleted_at | TIMESTAMP | | Soft delete |

#### tags

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| name | VARCHAR(100) | NOT NULL | Nome da tag |
| color | VARCHAR(7) | DEFAULT '#6B7280' | Cor (hex) |
| created_at | TIMESTAMP | NOT NULL | Criação |

#### contact_tags

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| contact_id | UUID | FK → contacts.id, NOT NULL | Contato |
| tag_id | UUID | FK → tags.id, NOT NULL | Tag |
| created_at | TIMESTAMP | NOT NULL | Criação |

### Pipeline

#### pipelines

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| name | VARCHAR(200) | NOT NULL | Nome do pipeline |
| description | TEXT | | Descrição |
| is_default | BOOLEAN | DEFAULT false | Pipeline padrão |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |
| deleted_at | TIMESTAMP | | Soft delete |

#### stages

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| pipeline_id | UUID | FK → pipelines.id, NOT NULL | Pipeline |
| name | VARCHAR(100) | NOT NULL | Nome do estágio |
| order_index | INTEGER | NOT NULL | Ordem |
| color | VARCHAR(7) | DEFAULT '#6B7280' | Cor (hex) |
| probability | INTEGER | DEFAULT 0 | Probabilidade padrão (%) |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |

#### opportunities

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| pipeline_id | UUID | FK → pipelines.id, NOT NULL | Pipeline |
| stage_id | UUID | FK → stages.id, NOT NULL | Estágio |
| contact_id | UUID | FK → contacts.id, NOT NULL | Contato |
| title | VARCHAR(200) | NOT NULL | Título |
| value | DECIMAL(12,2) | | Valor |
| probability | INTEGER | DEFAULT 0 | Probabilidade (%) |
| expected_close_date | DATE | | Previsão de fechamento |
| status | VARCHAR(20) | DEFAULT 'OPEN' | Status |
| lost_reason | TEXT | | Motivo da perda |
| won_at | TIMESTAMP | | Data de ganho |
| lost_at | TIMESTAMP | | Data de perda |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |
| deleted_at | TIMESTAMP | | Soft delete |

#### opportunity_history

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| opportunity_id | UUID | FK → opportunities.id, NOT NULL | Oportunidade |
| from_stage_id | UUID | FK → stages.id | Estágio anterior |
| to_stage_id | UUID | FK → stages.id | Estágio destino |
| changed_by | UUID | FK → users.id | Usuário que moveu |
| note | TEXT | | Observação |
| created_at | TIMESTAMP | NOT NULL | Criação |

### Communication

#### conversations

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| contact_id | UUID | FK → contacts.id | Contato |
| channel | VARCHAR(20) | NOT NULL | Canal |
| status | VARCHAR(20) | DEFAULT 'OPEN' | Status |
| assigned_to | UUID | FK → users.id | Atendente |
| last_message_at | TIMESTAMP | | Última mensagem |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |

#### messages

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| conversation_id | UUID | FK → conversations.id, NOT NULL | Conversa |
| sender_type | VARCHAR(20) | NOT NULL | AGENT/CONTACT/SYSTEM |
| sender_id | UUID | | ID do remetente |
| content | TEXT | NOT NULL | Conteúdo |
| message_type | VARCHAR(20) | DEFAULT 'TEXT' | Tipo |
| status | VARCHAR(20) | DEFAULT 'PENDING' | Status |
| external_id | VARCHAR(255) | | ID no canal externo |
| metadata | JSONB | | Dados extras |
| created_at | TIMESTAMP | NOT NULL | Criação |

#### message_templates

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| name | VARCHAR(200) | NOT NULL | Nome do template |
| category | VARCHAR(50) | NOT NULL | MARKETING/UTILITY/AUTHENTICATION |
| channel | VARCHAR(20) | NOT NULL | WHATSAPP/SMS/EMAIL |
| body | TEXT | NOT NULL | Corpo do template |
| variables | JSONB | | Variáveis [{name, example}] |
| whatsapp_template_id | VARCHAR(255) | | ID no WhatsApp |
| status | VARCHAR(20) | DEFAULT 'DRAFT' | DRAFT/PENDING/APPROVED/REJECTED |
| language | VARCHAR(10) | DEFAULT 'pt_BR' | Idioma |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |
| deleted_at | TIMESTAMP | | Soft delete |

#### message_attachments

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| message_id | UUID | FK → messages.id, NOT NULL | Mensagem |
| file_name | VARCHAR(500) | NOT NULL | Nome do arquivo |
| file_url | TEXT | NOT NULL | URL do arquivo |
| file_type | VARCHAR(50) | NOT NULL | MIME type |
| file_size | BIGINT | NOT NULL | Tamanho em bytes |
| created_at | TIMESTAMP | NOT NULL | Criação |

### Analytics

#### analytics_events

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| event_type | VARCHAR(50) | NOT NULL | Tipo do evento |
| entity_type | VARCHAR(50) | NOT NULL | Entidade afetada |
| entity_id | UUID | NOT NULL | ID da entidade |
| user_id | UUID | | Usuário que gerou |
| payload | JSONB | | Dados do evento |
| created_at | TIMESTAMP | NOT NULL | Criação |

#### leads

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| contact_id | UUID | FK → contacts.id | Contato associado |
| source | VARCHAR(50) | NOT NULL | Origem do lead |
| status | VARCHAR(20) | DEFAULT 'NEW' | Status |
| score | INTEGER | DEFAULT 0 | Pontuação (0-100) |
| converted_at | TIMESTAMP | | Data de conversão |
| lost_at | TIMESTAMP | | Data de perda |
| lost_reason | TEXT | | Motivo da perda |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |
| deleted_at | TIMESTAMP | | Soft delete |

### Campaign

#### campaigns

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| name | VARCHAR(200) | NOT NULL | Nome da campanha |
| channel | VARCHAR(20) | NOT NULL | Canal |
| status | VARCHAR(20) | DEFAULT 'DRAFT' | Status |
| scheduled_at | TIMESTAMP | | Agendamento |
| started_at | TIMESTAMP | | Início |
| completed_at | TIMESTAMP | | Conclusão |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |
| deleted_at | TIMESTAMP | | Soft delete |

#### campaign_steps

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| campaign_id | UUID | FK → campaigns.id, NOT NULL | Campanha |
| step_order | INTEGER | NOT NULL | Ordem do passo |
| step_type | VARCHAR(50) | NOT NULL | Tipo do passo |
| template_id | UUID | FK → message_templates.id | Template usado |
| delay_hours | INTEGER | DEFAULT 0 | Delay em horas |
| conditions | JSONB | | Condições de execução |
| created_at | TIMESTAMP | NOT NULL | Criação |

### Integration

#### automation_rules

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| name | VARCHAR(200) | NOT NULL | Nome da regra |
| description | TEXT | | Descrição |
| trigger_event | VARCHAR(50) | NOT NULL | Evento gatilho |
| conditions | JSONB | NOT NULL | Condições (If/Else) |
| actions | JSONB | NOT NULL | Ações |
| is_active | BOOLEAN | DEFAULT true | Ativa |
| execution_count | INTEGER | DEFAULT 0 | Contador de execuções |
| last_executed_at | TIMESTAMP | | Última execução |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |
| deleted_at | TIMESTAMP | | Soft delete |

#### automation_triggers

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| automation_rule_id | UUID | FK → automation_rules.id, NOT NULL | Regra |
| event_type | VARCHAR(50) | NOT NULL | Tipo do evento |
| entity_type | VARCHAR(50) | | Entidade alvo |
| is_active | BOOLEAN | DEFAULT true | Ativo |
| created_at | TIMESTAMP | NOT NULL | Criação |

#### automation_actions

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| automation_rule_id | UUID | FK → automation_rules.id, NOT NULL | Regra |
| action_type | VARCHAR(50) | NOT NULL | Tipo da ação |
| action_config | JSONB | NOT NULL | Configuração da ação |
| action_order | INTEGER | NOT NULL | Ordem de execução |
| created_at | TIMESTAMP | NOT NULL | Criação |

### RBAC

#### roles

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| name | VARCHAR(100) | NOT NULL | Nome do papel |
| description | TEXT | | Descrição |
| permissions | JSONB | NOT NULL | Lista de permissões |
| is_system | BOOLEAN | DEFAULT false | Papel do sistema |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |

#### user_roles

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| user_id | UUID | FK → users.id, NOT NULL | Usuário |
| role_id | UUID | FK → roles.id, NOT NULL | Papel |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| created_at | TIMESTAMP | NOT NULL | Criação |

### Subscription

#### subscriptions

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| plan | VARCHAR(50) | NOT NULL | PLANE/STARTER/PROFESSIONAL/ENTERPRISE |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | Status |
| trial_ends_at | TIMESTAMP | | Fim do trial |
| current_period_start | TIMESTAMP | | Início do período |
| current_period_end | TIMESTAMP | | Fim do período |
| cancel_at | TIMESTAMP | | Agendamento de cancelamento |
| stripe_subscription_id | VARCHAR(255) | | ID no Stripe |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |

### Contact (extensões)

#### contact_addresses

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| contact_id | UUID | FK → contacts.id, NOT NULL | Contato |
| type | VARCHAR(20) | NOT NULL | HOME/WORK/OTHER |
| street | VARCHAR(255) | | Rua |
| number | VARCHAR(20) | | Número |
| complement | VARCHAR(100) | | Complemento |
| neighborhood | VARCHAR(100) | | Bairro |
| city | VARCHAR(100) | | Cidade |
| state | VARCHAR(50) | | Estado |
| zip_code | VARCHAR(10) | | CEP |
| country | VARCHAR(50) | DEFAULT 'BR' | País |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |

#### contact_custom_fields

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| contact_id | UUID | FK → contacts.id, NOT NULL | Contato |
| field_key | VARCHAR(100) | NOT NULL | Chave do campo |
| field_value | TEXT | | Valor |
| created_at | TIMESTAMP | NOT NULL | Criação |
| updated_at | TIMESTAMP | NOT NULL | Atualização |

### Events

#### events

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| aggregate_id | UUID | NOT NULL | ID do agregado |
| aggregate_type | VARCHAR(50) | NOT NULL | Tipo do agregado |
| event_type | VARCHAR(100) | NOT NULL | Tipo do evento |
| payload | JSONB | NOT NULL | Dados do evento |
| metadata | JSONB | | Metadados |
| created_at | TIMESTAMP | NOT NULL | Criação |
| published | BOOLEAN | DEFAULT false | Publicado |

### Audit

#### audit_logs

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador |
| company_id | UUID | FK → companies.id, NOT NULL | Empresa |
| user_id | UUID | FK → users.id | Usuário |
| action | VARCHAR(50) | NOT NULL | CREATE/UPDATE/DELETE |
| entity_type | VARCHAR(50) | NOT NULL | Entidade afetada |
| entity_id | UUID | NOT NULL | ID da entidade |
| old_values | JSONB | | Valores anteriores |
| new_values | JSONB | | Valores novos |
| ip_address | VARCHAR(45) | | IP do cliente |
| user_agent | TEXT | | User agent |
| created_at | TIMESTAMP | NOT NULL | Criação |

## Responsabilidades

- Documentar schema completo do banco
- Servir de referência para migrations
- Facilitar onboarding de novos devs

## Dependências

- [ERD.md](./ERD.md) — Diagrama visual
- [00-core/Architecture.md](../00-core/Architecture.md) — Bounded contexts

## Regras

- Toda tabela deve ter id (UUID), created_at, updated_at
- Soft delete via deleted_at (nullable)
- Colunas nullable só quando realmente opcional
- JSONB para metadata flexível
- VARCHAR com tamanho definido

## Futuras Melhorias

- Auto-generation de documentação a partir do schema
- Schema visual interativo
- Validação de documentação contra schema real

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.2.0 | 2026-07-15 | Architect | Adicionadas 8 tabelas ER faltantes: company_settings, tags, contact_tags, pipelines, stages, opportunity_history, conversations, audit_logs |
| 1.1.0 | 2026-07-15 | Architect | Adicionadas 11 tabelas: message_templates, message_attachments, analytics_events, leads, campaigns, campaign_steps, automation_triggers, automation_actions, roles, user_roles, subscriptions, contact_addresses, contact_custom_fields, events |
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
