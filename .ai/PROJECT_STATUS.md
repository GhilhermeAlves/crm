# Status do Projeto

## Identificação
- **Nome:** CRM SaaS Omnichannel
- **Versão:** 6.2.0 (ver `docs/CHANGELOG.md`)
- **Status Geral:** 🟢 Em desenvolvimento
- **Data da Última Atualização:** 2026-08-23

## Sprint Atual
- **Concluídas recentemente:** Sprint **16 — WhatsApp/Omnichannel** (fechada com hardening,
  CI/CD GREEN e validação na VPS — ver `sprints/16/REPORT.md`) e Sprint **20 — IA**
  (`sprints/20/REPORT.md`); entrega funcional **Notificações In-app**
  (`sprints/notifications/REPORT.md`).
- **Próxima:** Sprint **17 — Campanhas** (planejamento pendente).

## Estado dos Módulos (verificado em 2026-08-23)

### Backend
| Módulo | Nível |
|--------|-------|
| identity, company, membership, invitation, contact, lead, pipeline, activity, task, customer360, audit, storage, workflow, omnichannel, notification, ai | ✅ completo |
| dashboard, onboarding, me | 🟡 parcial |
| analytics, campaign, communication | ❌ pastas vazias (Sprints 17–19) |

### Frontend
| Área | Nível |
|------|-------|
| Auth, Dashboard, Contacts, Leads, Pipeline, Inbox/Channels, Tasks/Activities, Workflows, Tenants, Users/Members/Invitations, Roles/Permissions, Audit/Storage, Profile, Notifications (sino + `/notifications`), IA (Leo: chat/análise/ações) | ✅ |
| Settings | 🟡 (falta `/settings` raiz) |
| Campaigns / Reports | ❌ rota sem página |

### Banco de Dados
- Flyway V001–V054 aplicadas. Destaques: V044/V045 omnichannel, V047/V048 notificações,
  V049/V050/V051/V052 IA, V053 permissões omnichannel por papel, V054 FKs compostas de tenant.

### Omnichannel/WhatsApp
- Canais, inbox e webhook com RLS FORCE; HMAC `X-Hub-Signature-256`; provider fake (default)
  vs Cloud API exclusivos via config; falha de envio persiste `FAILED`.

## Testes & Qualidade
- Backend: 502 unit tests + ITs Testcontainers (`*IT`) verdes no `mvn verify`, incluindo
  `OmnichannelIsolationIT` (7/7).
- Frontend: lint/typecheck/build/testes verdes no CI.

## Infraestrutura & Deploy
- Produção na VPS (`crm-vps`, `/opt/crm/docker`) via GHCR; CI/CD GitHub Actions GREEN
  (commit `2a8c597`); containers backend/auth-service/frontend/postgres/redis/rabbitmq/
  minio/keycloak UP/healthy.

## Débitos Técnicos Ativos
- Omnichannel: N+1 em `listConversations`; rate limit do webhook; token global ignorando
  `secretsRef` por canal; pagination bounds; `docs/WHATSAPP.md`; WebSocket na inbox.
- Notificações: e-mail real não conectado (`ConsoleEmailSender`); frontend usa polling
  apesar do STOMP disponível.
- Auditoria: 500 em busca (`lower(bytea)` — binding bytea em `audit_logs`), pré-existente.
- E2E autenticado manual herdado (sem credenciais de teste).

---

*Atualizado em: 2026-08-23*
