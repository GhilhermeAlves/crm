# Gerenciador Mestre de Execução do Projeto

**Projeto:** CRM SaaS Omnichannel
**Versão:** 1.2.0
**Última Atualização:** 2026-08-17

> **Fonte de verdade:** [`sprints/SPRINT_INDEX.md`](../sprints/SPRINT_INDEX.md).
> Este arquivo resume o roadmap e o estado atual. Numeração de sprints segue o índice canônico
> (Segurança 5–6.10, Identidade 7.x, SaaS 8.x, CRM 9–15, Omnichannel 16–18, Analytics 19, IA 20).

---

## 1. Visão Geral das Sprints (resumo)

| Sprint | Fase | Módulo | Status |
|--------|------|--------|--------|
| 0–3.3 | Fundação / Knowledge / AI Runtime | Planejamento + Layers | ✅ Concluídas |
| 4.1–4.5 | Infraestrutura | Auth monolítico (JWT) | ↪️ Absorvida pelo gateway OIDC/Keycloak |
| 5–6.10 | Segurança | Tenant/RLS + Access Gateway OIDC + sessão Redis | ✅ Concluídas |
| 7.0–7.5 | Identidade / Autenticação | Google, account linking, telefone/OTP, recuperação | ✅ Concluídas |
| 8–8.6 | SaaS | Empresas, membership, onboarding, switcher, convites, hardening | ✅ Concluídas |
| 9 | CRM | User & Permission Management / Contatos | ✅ Concluída |
| 10 | CRM | Leads | ✅ Concluída |
| 11 | CRM | Pipeline | ✅ Concluída |
| 12 | CRM | Orientado à ação (Activities/Tasks/Dashboard) | ✅ Concluída |
| 13–15 | CRM | Automação de Workflows + auditoria | ✅ Concluídas |
| 16 | Omnichannel | WhatsApp (base) | 🚧 Em andamento (deploy pendente) |
| 17 | Omnichannel | Campanhas | ⏳ Pendente |
| 18 | Omnichannel | Automações | ⏳ Pendente |
| 19 | Analytics | Dashboard | ⏳ Pendente |
| 20 | IA | IA Features / Sugestão de resposta | ⏳ Pendente |

---

## 2. Estado Real dos Módulos (verificado em 2026-08-17)

> Legenda: **✅** completo/robusto · **🟡** parcial/esqueleto · **❌** inexistente.

### Backend (`backend/src/main/java/com/becommerce/crm/`)

| Módulo | Nível | Observações |
|--------|-------|-------------|
| identity | ✅ | Auth/OIDC, users, roles, permissions, OTP, reset — o maior módulo |
| company | ✅ | CRUD, settings, plano, quota/uso, eventos |
| membership | ✅ | Associação usuário-empresa, membros, papéis |
| invitation | ✅ | Convites token-based, rate limit Redis, e-mail |
| onboarding | 🟡 | Orquestra criação de empresa (sem DTOs próprios) |
| contact | ✅ | CRUD completo |
| lead | ✅ | CRUD + enums + isolamento RLS |
| pipeline | ✅ | Pipelines, stages, opportunities, métricas |
| activity | ✅ | Timeline CRUD |
| task | ✅ | CRUD + transições |
| customer360 | ✅ | Agregação de leitura (contato+pipeline+tarefas+atividades) |
| dashboard | 🟡 | Operacional determinístico (leitura/KPIs) |
| audit | ✅ | Auditoria transversal (AOP + eventos) |
| storage | ✅ | Upload/download/lista + quota |
| workflow | ✅ | Automação (triggers/condições/ações/runs/dry-run) |
| omnichannel | ✅ | Canais, conversas, mensagens, webhook WhatsApp |
| **notification** | **🟡/❌** | **Só `EmailSender` (interface) + `ConsoleEmailSender` (fake/log). Sem tabela, controller, DTO, push/WebSocket.** |
| **analytics** | **❌** | Pastas vazias (`application/analytics`, `domain/analytics`) |
| **campaign** | **❌** | Pastas vazias (`application/campaign`, `domain/campaign`) |
| **communication** | **❌** | Pastas vazias |
| shared | ❌ | Pastas vazias (compartilhado é por módulo) |
| **IA/OpenAI** | **❌** | Pasta `infrastructure/integration/openai` **vazia**. Nenhum código LLM. |

### Frontend (`frontend/src/`)

| Área | Nível | Observações |
|------|-------|-------------|
| Auth | ✅ | Login/registro/forgot/reset/link/callback (OIDC) |
| Dashboard | ✅ | Página `/dashboard` orientada à ação |
| Contacts | ✅ | `/contacts`, `/contacts/[id]` (Customer 360) |
| Leads | ✅ | `/leads` + new/edit/[id] |
| Pipeline | ✅ | `/pipeline` (board) |
| Inbox/Channels | ✅ | `/inbox` (chat WhatsApp), `/channels` |
| Tasks/Activities | ✅ | `/tasks`, `/activities` |
| Workflows | ✅ | `/workflows` + new/edit/[id] |
| Tenants | ✅ | `/tenants` + wizard de criação |
| Users/Members/Invitations | ✅ | `/users`, `/members`, `/invitations` |
| Roles/Permissions | ✅ | `/roles`, `/permissions` |
| Settings | 🟡 | Só `settings/users` e `settings/roles` (falta `/settings` raiz) |
| Audit/Storage | ✅ | `/audit`, `/storage` |
| Profile | ✅ | `/profile` |
| **Notifications** | **❌** | Só sino decorativo hardcoded no `Header.tsx`. Sem página, hook, serviço, badge real, WebSocket/SSE. |
| **Campaigns** | **❌** | Rota `/campaigns` na Sidebar **sem page.tsx** |
| **Reports** | **❌** | Rota `/reports` na Sidebar **sem page.tsx** |
| **Chat real-time** | **⚠️** | REST + React Query; **sem WebSocket/SSE** — novas mensagens exigem refetch manual |
| **IA/Sugestão** | **❌** | Não existe. Único `suggestion` é regra de negócio do dashboard |

### Banco de Dados (`backend/src/main/resources/db/migration/V001–V046`)

- **Sem tabela de notificações.** Única menção: coluna `notification_preferences` em `company_settings` (V014).
- **Sem tabela de campanhas/relatórios.**
- WhatsApp/Omnichannel: `omnichannel_channels`, `omnichannel_conversations`, `omnichannel_messages` (V044) ✅.
- Workflows: 5 tabelas (V041/V043) ✅.

---

## 3. Sprints Pendentes (próximas)

| Prioridade | Sprint | Módulo | Por quê |
|-----------|--------|--------|---------|
| 🔴 1 | **Notifications** | Notificações in-app + WebSocket/SSE + e-mail real | Sino hardcoded, sem backend. Requisito para UX e para o chat real-time |
| 🔴 2 | 20 | IA / Sugestão de resposta | Funil (leads/pipeline) pronto; dashboard já sugere (determinístico); próximo passo IA |
| 🟠 3 | 17 | Campanhas | Backend vazio + rota sem página |
| 🟠 4 | 18 | Automações (Omnichannel) | Depende de 17 |
| 🟠 5 | 19 | Analytics / Dashboard avançado | Depende de 9, 10, 12 |
| 🟡 6 | 16 | WhatsApp (fechamento) | Código + testes + build OK; falta deploy/VPS + IT Testcontainers + E2E |

> **Nota de sequência:** Notificações e IA não são sprints numeradas no índice canônico, mas o usuário
> priorizou: **Notificações → IA/Sugestão de resposta**. Campanhas/Analytics permanecem no backlog
> (17/18/19/20).

---

## 4. Fases por Estado

| Fase | Total | ✅ | 🚧 | ⏳ | ↪️ |
|------|-------|----|----|----|----|
| Planejamento | 3 | 3 | 0 | 0 | 0 |
| Knowledge Layer | 3 | 3 | 0 | 0 | 0 |
| Infraestrutura | 5 | 0 | 0 | 0 | 5 |
| Segurança | 12 | 12 | 0 | 0 | 0 |
| Identidade / Autenticação | 6 | 6 | 0 | 0 | 0 |
| SaaS | 7 | 7 | 0 | 0 | 0 |
| CRM | 8 | 8 | 0 | 0 | 0 |
| Omnichannel | 3 | 0 | 1 | 2 | 0 |
| Analytics | 1 | 0 | 0 | 1 | 0 |
| IA | 1 | 0 | 0 | 1 | 0 |
| **Total** | **49** | **39** | **1** | **4** | **5** |

---

## 5. Regras de Sequenciamento

1. Auth primeiro (gateway OIDC/Keycloak) → feito
2. Tenant/RLS depois de Auth → feito
3. Empresas depois de Tenant → feito
4. Contatos/Leads/Pipeline → feito
5. Workflows → feito
6. WhatsApp (16) → em andamento
7. **Notificações** → para suportar UX/chat real-time
8. **IA/Sugestão de resposta** (20)
9. Campanhas (17) → Automações (18) → Analytics (19)

---

## 6. Histórico de Alterações

| Data | Sprint | Alteração |
|------|--------|-----------|
| 2026-08-17 | — | **Revisão profunda do estado real dos módulos** (backend/frontend/DB). Módulos inexistentes: notifications, analytics, campaign, communication, IA. Frontend sem Campaigns/Reports/Notifications/real-time. Banco sem tabela de notificações. Próximo: Notificações → IA/Sugestão. |
| 2026-08-16 | — | Deploy real em produção via GHCR; fix RLS no createCompany (proxy de tenant GUC); wizard de criação de empresa |
| 2026-08-15 | 16 | Início Sprint 16 — WhatsApp (código + testes + build OK) |

---

*Última atualização: 2026-08-17*
