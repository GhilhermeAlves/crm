# API_MAP — Catálogo de APIs

## Objetivo

Listar todas as APIs REST expostas pelo backend, organizadas por módulo, com endpoints, métodos, autenticação e DTOs.

## Índice

- [1. Autenticação](#1-autenticação)
- [2. Usuários](#2-usuários)
- [3. Empresas](#3-empresas)
- [4. Contatos](#4-contatos)
- [5. Leads](#5-leads)
- [6. Pipeline](#6-pipeline)
- [7. Kanban](#7-kanban)
- [8. Chat/Conversas](#8-chatconversas)
- [9. Mensagens](#9-mensagens)
- [10. Templates](#10-templates)
- [11. Campanhas](#11-campanhas)
- [12. Automações](#12-automações)
- [13. Dashboard](#13-dashboard)
- [14. Relatórios](#14-relatórios)
- [15. Clientes](#15-clientes)
- [16. Notificações](#16-notificações)
- [17. Webhooks](#17-webhooks)
- [18. Auditoria](#18-auditoria)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## 1. Autenticação

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Não | Login com email/senha |
| POST | `/api/v1/auth/refresh` | Refresh Token | Renovar access token |
| POST | `/api/v1/auth/logout` | Bearer | Logout (blacklist token) |
| POST | `/api/v1/auth/forgot-password` | Não | Solicitar reset de senha |
| POST | `/api/v1/auth/reset-password` | Não | Confirmar reset de senha |

**DTOs:** `LoginRequest`, `TokenResponse`, `RefreshRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`

**Fonte:** [01-backend/Auth.md](./01-backend/Auth.md)

---

## 2. Usuários

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/users` | ADMIN+ | Listar usuários |
| GET | `/api/v1/users/{id}` | ADMIN+ | Buscar usuário |
| POST | `/api/v1/users` | ADMIN | Criar usuário |
| PUT | `/api/v1/users/{id}` | ADMIN+ | Atualizar usuário |
| DELETE | `/api/v1/users/{id}` | ADMIN | Desativar usuário |
| POST | `/api/v1/users/{id}/invite` | ADMIN | Convidar usuário |
| GET | `/api/v1/users/me` | Qualquer | Dados do usuário logado |
| PUT | `/api/v1/users/me` | Qualquer | Atualizar próprio perfil |

**DTOs:** `UserResponse`, `CreateUserRequest`, `UpdateUserRequest`, `InviteRequest`

**Fonte:** [01-backend/Users.md](./01-backend/Users.md)

---

## 3. Empresas

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/companies/{id}` | ADMIN+ | Dados da empresa |
| PUT | `/api/v1/companies/{id}` | ADMIN | Atualizar empresa |
| GET | `/api/v1/companies/{id}/settings` | ADMIN+ | Configurações |
| PUT | `/api/v1/companies/{id}/settings` | ADMIN | Atualizar config |
| GET | `/api/v1/companies/{id}/billing` | ADMIN | Status de billing |

**DTOs:** `CompanyResponse`, `UpdateCompanyRequest`, `SettingsResponse`, `UpdateSettingsRequest`

**Fonte:** [01-backend/Companies.md](./01-backend/Companies.md)

---

## 4. Contatos

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/contacts` | AGENT+ | Listar contatos (paginado) |
| GET | `/api/v1/contacts/{id}` | AGENT+ | Buscar contato |
| POST | `/api/v1/contacts` | AGENT+ | Criar contato |
| PUT | `/api/v1/contacts/{id}` | AGENT+ | Atualizar contato |
| DELETE | `/api/v1/contacts/{id}` | MANAGER+ | Arquivar contato |
| POST | `/api/v1/contacts/import` | MANAGER+ | Importar (CSV) |
| GET | `/api/v1/contacts/export` | MANAGER+ | Exportar (CSV) |
| POST | `/api/v1/contacts/{id}/tags` | AGENT+ | Adicionar tag |
| DELETE | `/api/v1/contacts/{id}/tags/{tag}` | AGENT+ | Remover tag |
| GET | `/api/v1/contacts/search` | AGENT+ | Buscar por nome/email/telefone |

**DTOs:** `ContactResponse`, `CreateContactRequest`, `UpdateContactRequest`, `ContactSearchRequest`, `ImportRequest`

**Fonte:** [01-backend/Contacts.md](./01-backend/Contacts.md)

---

## 5. Leads

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/leads` | AGENT+ | Listar leads |
| GET | `/api/v1/leads/{id}` | AGENT+ | Buscar lead |
| POST | `/api/v1/leads` | AGENT+ | Criar lead |
| PUT | `/api/v1/leads/{id}` | AGENT+ | Atualizar lead |
| DELETE | `/api/v1/leads/{id}` | MANAGER+ | Arquivar lead |
| POST | `/api/v1/leads/{id}/convert` | AGENT+ | Converter lead → cliente |
| POST | `/api/v1/leads/{id}/qualify` | AGENT+ | Qualificar lead |
| POST | `/api/v1/leads/{id}/score` | SYSTEM | Recalcular score |
| GET | `/api/v1/leads/scoring/rules` | MANAGER+ | Regras de scoring |

**DTOs:** `LeadResponse`, `CreateLeadRequest`, `UpdateLeadRequest`, `ConvertLeadRequest`, `ScoreRuleRequest`

**Fonte:** [01-backend/Leads.md](./01-backend/Leads.md), [05-business-rules/Lead.md](./05-business-rules/Lead.md)

---

## 6. Pipeline

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/pipelines` | AGENT+ | Listar pipelines |
| GET | `/api/v1/pipelines/{id}` | AGENT+ | Buscar pipeline |
| POST | `/api/v1/pipelines` | MANAGER+ | Criar pipeline |
| PUT | `/api/v1/pipelines/{id}` | MANAGER+ | Atualizar pipeline |
| DELETE | `/api/v1/pipelines/{id}` | ADMIN | Arquivar pipeline |
| GET | `/api/v1/pipelines/{id}/stages` | AGENT+ | Listar estágios |
| POST | `/api/v1/pipelines/{id}/stages` | MANAGER+ | Criar estágio |
| PUT | `/api/v1/stages/{id}` | MANAGER+ | Atualizar estágio |
| DELETE | `/api/v1/stages/{id}` | MANAGER+ | Remover estágio |
| POST | `/api/v1/stages/reorder` | MANAGER+ | Reordenar estágios |
| GET | `/api/v1/pipelines/{id}/opportunities` | AGENT+ | Listar oportunidades |
| POST | `/api/v1/pipelines/{id}/opportunities` | AGENT+ | Criar oportunidade |
| PUT | `/api/v1/opportunities/{id}` | AGENT+ | Atualizar oportunidade |
| DELETE | `/api/v1/opportunities/{id}` | MANAGER+ | Arquivar oportunidade |

**DTOs:** `PipelineResponse`, `CreatePipelineRequest`, `StageResponse`, `CreateStageRequest`, `OpportunityResponse`, `CreateOpportunityRequest`, `ReorderRequest`

**Fonte:** [01-backend/Pipeline.md](./01-backend/Pipeline.md), [01-backend/Stages.md](./01-backend/Stages.md)

---

## 7. Kanban

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/kanban/pipelines/{id}` | AGENT+ | Dados do kanban |
| POST | `/api/v1/kanban/move` | AGENT+ | Mover oportunidade |
| GET | `/api/v1/kanban/filters` | AGENT+ | Filtros disponíveis |

**DTOs:** `KanbanResponse`, `MoveOpportunityRequest`, `KanbanFilters`

**Fonte:** [01-backend/Kanban.md](./01-backend/Kanban.md)

---

## 8. Chat/Conversas

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/conversations` | AGENT+ | Listar conversas |
| GET | `/api/v1/conversations/{id}` | AGENT+ | Buscar conversa |
| POST | `/api/v1/conversations` | AGENT+ | Criar conversa |
| PUT | `/api/v1/conversations/{id}/assign` | MANAGER+ | Atribuir conversa |
| PUT | `/api/v1/conversations/{id}/status` | AGENT+ | Mudar status |
| POST | `/api/v1/conversations/{id}/close` | AGENT+ | Fechar conversa |
| POST | `/api/v1/conversations/{id}/transfer` | AGENT+ | Transferir conversa |

**DTOs:** `ConversationResponse`, `CreateConversationRequest`, `AssignRequest`, `UpdateStatusRequest`, `TransferRequest`

**Fonte:** [01-backend/Conversations.md](./01-backend/Conversations.md)

---

## 9. Mensagens

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/conversations/{id}/messages` | AGENT+ | Listar mensagens |
| POST | `/api/v1/messages` | AGENT+ | Enviar mensagem |
| POST | `/api/v1/messages/bulk` | AGENT+ | Enviar múltiplas |
| GET | `/api/v1/messages/{id}` | AGENT+ | Buscar mensagem |
| POST | `/api/v1/messages/{id}/read` | AGENT+ | Marcar como lida |
| POST | `/api/v1/messages/typing` | AGENT+ | Indicador de digitação |

**DTOs:** `MessageResponse`, `SendMessageRequest`, `BulkMessageRequest`, `TypingIndicator`

**Fonte:** [01-backend/Messages.md](./01-backend/Messages.md)

---

## 10. Templates

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/templates` | AGENT+ | Listar templates |
| GET | `/api/v1/templates/{id}` | AGENT+ | Buscar template |
| POST | `/api/v1/templates` | MANAGER+ | Criar template |
| PUT | `/api/v1/templates/{id}` | MANAGER+ | Atualizar template |
| DELETE | `/api/v1/templates/{id}` | MANAGER+ | Arquivar template |
| POST | `/api/v1/templates/{id}/approve` | ADMIN | Aprovar template |
| GET | `/api/v1/templates/categories` | AGENT+ | Categorias |

**DTOs:** `TemplateResponse`, `CreateTemplateRequest`, `UpdateTemplateRequest`

**Fonte:** [01-backend/Templates.md](./01-backend/Templates.md)

---

## 11. Campanhas

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/campaigns` | MANAGER+ | Listar campanhas |
| GET | `/api/v1/campaigns/{id}` | MANAGER+ | Buscar campanha |
| POST | `/api/v1/campaigns` | MANAGER+ | Criar campanha |
| PUT | `/api/v1/campaigns/{id}` | MANAGER+ | Atualizar campanha |
| DELETE | `/api/v1/campaigns/{id}` | ADMIN | Arquivar campanha |
| POST | `/api/v1/campaigns/{id}/start` | MANAGER+ | Iniciar campanha |
| POST | `/api/v1/campaigns/{id}/pause` | MANAGER+ | Pausar campanha |
| POST | `/api/v1/campaigns/{id}/cancel` | MANAGER+ | Cancelar campanha |
| GET | `/api/v1/campaigns/{id}/stats` | MANAGER+ | Estatísticas |

**DTOs:** `CampaignResponse`, `CreateCampaignRequest`, `UpdateCampaignRequest`, `CampaignStats`

**Fonte:** [01-backend/Campaigns.md](./01-backend/Campaigns.md)

---

## 12. Automações

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/automations` | MANAGER+ | Listar automações |
| GET | `/api/v1/automations/{id}` | MANAGER+ | Buscar automação |
| POST | `/api/v1/automations` | MANAGER+ | Criar automação |
| PUT | `/api/v1/automations/{id}` | MANAGER+ | Atualizar automação |
| DELETE | `/api/v1/automations/{id}` | ADMIN | Arquivar automação |
| POST | `/api/v1/automations/{id}/toggle` | MANAGER+ | Ativar/desativar |
| GET | `/api/v1/automations/{id}/executions` | MANAGER+ | Histórico de execuções |

**DTOs:** `AutomationResponse`, `CreateAutomationRequest`, `UpdateAutomationRequest`, `ExecutionHistory`

**Fonte:** [01-backend/Automations.md](./01-backend/Automations.md)

---

## 13. Dashboard

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/dashboard/kpis` | AGENT+ | KPIs gerais |
| GET | `/api/v1/dashboard/pipeline` | AGENT+ | Métricas do pipeline |
| GET | `/api/v1/dashboard/chat` | AGENT+ | Métricas do chat |
| GET | `/api/v1/dashboard/agent-performance` | MANAGER+ | Performance dos agentes |
| GET | `/api/v1/dashboard/conversion` | MANAGER+ | Taxa de conversão |

**DTOs:** `KPIDashboard`, `PipelineMetrics`, `ChatMetrics`, `AgentPerformance`, `ConversionMetrics`

**Fonte:** [01-backend/Dashboard.md](./01-backend/Dashboard.md)

---

## 14. Relatórios

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/reports/pipeline` | MANAGER+ | Relatório do pipeline |
| GET | `/api/v1/reports/agent-performance` | MANAGER+ | Performance de agentes |
| GET | `/api/v1/reports/campaign` | MANAGER+ | Relatório de campanhas |
| GET | `/api/v1/reports/contact-growth` | MANAGER+ | Crescimento de contatos |
| GET | `/api/v1/reports/export/{type}` | ADMIN | Exportar relatório |

**DTOs:** `PipelineReport`, `AgentReport`, `CampaignReport`, `ExportRequest`

**Fonte:** [01-backend/Reports.md](./01-backend/Reports.md)

---

## 15. Clientes

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/customers` | AGENT+ | Listar clientes |
| GET | `/api/v1/customers/{id}` | AGENT+ | Buscar cliente |
| POST | `/api/v1/customers` | AGENT+ | Criar cliente |
| PUT | `/api/v1/customers/{id}` | AGENT+ | Atualizar cliente |
| DELETE | `/api/v1/customers/{id}` | MANAGER+ | Arquivar cliente |
| GET | `/api/v1/customers/{id}/interactions` | AGENT+ | Interações |

**DTOs:** `CustomerResponse`, `CreateCustomerRequest`, `UpdateCustomerRequest`

**Fonte:** [01-backend/Customers.md](./01-backend/Customers.md)

---

## 16. Notificações

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/notifications` | Qualquer | Listar notificações |
| PUT | `/api/v1/notifications/{id}/read` | Qualquer | Marcar como lida |
| PUT | `/api/v1/notifications/read-all` | Qualquer | Marcar todas como lidas |
| GET | `/api/v1/notifications/preferences` | Qualquer | Preferências |
| PUT | `/api/v1/notifications/preferences` | Qualquer | Atualizar preferências |

**DTOs:** `NotificationResponse`, `NotificationPreferences`

**Fonte:** [01-backend/Notifications.md](./01-backend/Notifications.md)

---

## 17. Webhooks

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/webhooks` | ADMIN | Listar webhooks |
| POST | `/api/v1/webhooks` | ADMIN | Criar webhook |
| PUT | `/api/v1/webhooks/{id}` | ADMIN | Atualizar webhook |
| DELETE | `/api/v1/webhooks/{id}` | ADMIN | Remover webhook |
| POST | `/api/v1/webhooks/{id}/test` | ADMIN | Testar webhook |

**DTOs:** `WebhookResponse`, `CreateWebhookRequest`, `UpdateWebhookRequest`, `WebhookTestResult`

**Fonte:** [01-backend/Webhooks.md](./01-backend/Webhooks.md)

---

## 18. Auditoria

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/audit/logs` | ADMIN | Listar logs de auditoria |
| GET | `/api/v1/audit/logs/{id}` | ADMIN | Detalhe do log |

**DTOs:** `AuditLogResponse`, `AuditLogDetail`

**Fonte:** [01-backend/Audit.md](./01-backend/Audit.md)

---

## Referências

| Documento | Caminho |
|---|---|
| Backend Overview | [01-backend/Overview.md](./01-backend/Overview.md) |
| Módulos | [01-backend/Modules.md](./01-backend/Modules.md) |
| Convenções de Código | [00-core/CodingStandards.md](./00-core/CodingStandards.md) |
| SUMMARY | [SUMMARY.md](./SUMMARY.md) |

---

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial do catálogo de APIs |
