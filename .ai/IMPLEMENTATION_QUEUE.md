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
| 20 | IA | IA Features / Sugestão de resposta | ✅ Concluída |
| — | Notificações | Notificações in-app + WebSocket/STOMP | ✅ Concluída (fora do índice canônico) |

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
| **notification** | **✅** | **Tabela, domínio, serviço, controller REST, push WebSocket/STOMP, auditoria + permissões (V047/V048).** |
| **ai** | **✅** | **Sugestão de resposta: port + providers (OpenAI real / fake) + service + controller `/ai/suggestions/{id}` + permissão `ai:suggest` (V049).** |
| **analytics** | **❌** | Pastas vazias (`application/analytics`, `domain/analytics`) |
| **campaign** | **❌** | Pastas vazias (`application/campaign`, `domain/campaign`) |
| **communication** | **❌** | Pastas vazias |
| shared | ❌ | Pastas vazias (compartilhado é por módulo) |
| IA/OpenAI | ✅ | `infrastructure/integration/openai` com `OpenAiSuggestionProvider` + `FakeAiSuggestionProvider` |

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
| **Notifications** | **✅** | **Sino real com badge de não-lidas + dropdown, página `/notifications`, hook `useNotifications` (polling 15s), sidebar. Consome REST; STOMP pronto no backend (frontend usa polling — auth via cookie HttpOnly não expõe JWT pro STOMP).** |
| **Campaigns** | **❌** | Rota `/campaigns` na Sidebar **sem page.tsx** |
| **Reports** | **❌** | Rota `/reports` na Sidebar **sem page.tsx** |
| **Chat real-time** | **⚠️** | REST + React Query; **sem WebSocket/SSE** — novas mensagens exigem refetch manual |
| **IA/Sugestão** | **✅** | **Botão ✨ "Sugerir resposta" no `ChatThread.tsx` (perm `ai:suggest`) preenche o campo do composer.** |

### Banco de Dados (`backend/src/main/resources/db/migration/V001–V049`)

- **Notificações:** `notifications` (V047) com RLS FORCE + `notification:*` permissões (V048) ✅.
- **IA:** permissão `ai:suggest` (V049) ✅.
- **Sem tabela de campanhas/relatórios.**
- WhatsApp/Omnichannel: `omnichannel_channels`, `omnichannel_conversations`, `omnichannel_messages` (V044) ✅.
- Workflows: 5 tabelas (V041/V043) ✅.

---

## 3. Sprints Pendentes (próximas)

| Prioridade | Sprint | Módulo | Por quê |
|-----------|--------|--------|---------|
| 🔴 1 | **Notifications** | Notificações in-app + WebSocket/STOMP + e-mail real | ✅ **Implementado** (backend + WS/STOMP + frontend). Resta e-mail real (hoje console fake) e, opcionalmente, conectar frontend ao STOMP (hoje polling). |
| 🔴 2 | 20 | IA / Sugestão de resposta | ✅ **Implementado** (backend + providers + frontend). Resta chave OpenAI real (hoje `fake`). |
| 🟠 3 | 17 | Campanhas | Backend vazio + rota sem página |
| 🟠 4 | 18 | Automações (Omnichannel) | Depende de 17 |
| 🟠 5 | 19 | Analytics / Dashboard avançado | Depende de 9, 10, 12 |
| 🟡 6 | 16 | WhatsApp (fechamento) | Código + testes + build OK; falta deploy/VPS + IT Testcontainers + E2E |

> **Nota de sequência:** Notificações e IA (Sprint 20) **concluídos**. Próximos módulos a implementar:
> **Campanhas (17) → Automações (18) → Analytics (19)**, além do fechamento da Sprint 16 (WhatsApp).

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
| IA | 1 | 1 | 0 | 0 | 0 |
| **Total** | **49** | **40** | **1** | **3** | **5** |

---

## 5. Regras de Sequenciamento

1. Auth primeiro (gateway OIDC/Keycloak) → feito
2. Tenant/RLS depois de Auth → feito
3. Empresas depois de Tenant → feito
4. Contatos/Leads/Pipeline → feito
5. Workflows → feito
6. WhatsApp (16) → em andamento
7. **Notificações** → ✅ concluído (backend + WebSocket/STOMP + frontend)
8. **IA/Sugestão de resposta** (20) → ✅ concluído
9. Campanhas (17) → Automações (18) → Analytics (19)

---

## 6. Histórico de Alterações

| Data | Sprint | Alteração |
|------|--------|-----------|
| 2026-08-17 | Notificações + IA | **Módulo de Notificações concluído** (backend `notifications` + RLS V047, permissões V048, controller REST, push WebSocket/STOMP, auditoria; frontend sino real com badge + página `/notifications` + sidebar + polling 15s). **Módulo IA concluído** (Sprint 20: `AiSuggestionProvider` + `OpenAiSuggestionProvider`/`FakeAiSuggestionProvider`, `AiSuggestionService`, controller `/ai/suggestions/{id}`, permissão `ai:suggest` V049; frontend botão ✨ no Inbox). Backend 379 testes ✅, frontend typecheck/lint/build ✅. |
| 2026-08-16 | — | Deploy real em produção via GHCR; fix RLS no createCompany (proxy de tenant GUC); wizard de criação de empresa |
| 2026-08-15 | 16 | Início Sprint 16 — WhatsApp (código + testes + build OK) |

---

*Última atualização: 2026-08-17*
