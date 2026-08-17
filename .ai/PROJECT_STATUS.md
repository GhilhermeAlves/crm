# Status do Projeto

## Identificação
- **Nome:** CRM SaaS Omnichannel
- **Versão:** 20.0.0 (roadmap até Sprint 20 — IA)
- **Status Geral:** 🟢 Em desenvolvimento
- **Data da Última Atualização:** 2026-08-17

## Sprint Atual
- **Sprint:** 16 — WhatsApp (base) 🚧 em andamento + **revisão de roadmap**
- **Status:** Código + testes + build OK; pendente deploy/VPS + IT Testcontainers + E2E

## Estado Real dos Módulos (verificado em 2026-08-17)

### Backend
| Módulo | Nível |
|--------|-------|
| identity, company, membership, invitation, contact, lead, pipeline, activity, task, customer360, audit, storage, workflow, omnichannel | ✅ completo |
| dashboard, onboarding, me | 🟡 parcial (leitura/orquestração) |
| **notification** | **🟡/❌ só `EmailSender` (console fake)** |
| analytics, campaign, communication | ❌ pastas vazias |
| **IA/OpenAI** | ❌ pasta `integration/openai` vazia |

### Frontend
| Área | Nível |
|------|-------|
| Auth, Dashboard, Contacts, Leads, Pipeline, Inbox/Channels, Tasks/Activities, Workflows, Tenants, Users/Members/Invitations, Roles/Permissions, Audit/Storage, Profile | ✅ |
| Settings | 🟡 (falta `/settings` raiz) |
| **Notifications** | ❌ só sino decorativo no `Header.tsx` |
| **Campaigns** | ❌ rota sem página |
| **Reports** | ❌ rota sem página |
| **Chat real-time** | ⚠️ REST+React Query, sem WebSocket/SSE |
| **IA/Sugestão** | ❌ inexistente |

### Banco de Dados
- V001–V046 aplicadas. **Sem tabela de notificações** (só `notification_preferences` em company_settings).
- Omnichannel (V044) e Workflows (V041/V043) presentes.

## Métricas Gerais
- **Sprints:** 49 total · 39 ✅ · 1 🚧 · 4 ⏳ · 5 ↪️
- **Migrações Flyway:** V001–V046
- **Deploy:** produção na VPS via GHCR (backend, auth-service, frontend), pipelines CI/CD verdes

## Próximos Passos
1. 🔴 **Módulo Notificações** — backend (tabela, serviço, controller, DTOs, WebSocket/SSE) + frontend (página, hook, badge no header)
2. 🔴 **Módulo IA / Sugestão de resposta** (Sprint 20)
3. 🟠 Campanhas (17) → Automações (18) → Analytics (19)
4. 🟡 Fechamento Sprint 16 (deploy/VPS WhatsApp)

---

*Atualizado em: 2026-08-17*