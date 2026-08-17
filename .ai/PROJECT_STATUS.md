# Status do Projeto

## Identificação
- **Nome:** CRM SaaS Omnichannel
- **Versão:** 20.0.0 (roadmap até Sprint 20 — IA)
- **Status Geral:** 🟢 Em desenvolvimento
- **Data da Última Atualização:** 2026-08-17

## Sprint Atual
- **Sprint:** 16 — WhatsApp (base) 🚧 em andamento (código ok; pendente deploy/VPS + IT + E2E).
- **Recém-concluídas:** Módulo **Notificações** e **Sprint 20 — IA / Sugestão de resposta**.

## Estado Real dos Módulos (verificado em 2026-08-17)

### Backend
| Módulo | Nível |
|--------|-------|
| identity, company, membership, invitation, contact, lead, pipeline, activity, task, customer360, audit, storage, workflow, omnichannel, **notification**, **ai** | ✅ completo |
| dashboard, onboarding, me | 🟡 parcial (leitura/orquestração) |
| **analytics, campaign, communication** | ❌ pastas vazias |
| IA/OpenAI | ✅ `integration/openai` com providers real + fake |

### Frontend
| Área | Nível |
|------|-------|
| Auth, Dashboard, Contacts, Leads, Pipeline, Inbox/Channels, Tasks/Activities, Workflows, Tenants, Users/Members/Invitations, Roles/Permissions, Audit/Storage, Profile | ✅ |
| Settings | 🟡 (falta `/settings` raiz) |
| **Notifications** | ✅ sino real + badge + página `/notifications` + sidebar |
| **IA/Sugestão** | ✅ botão ✨ no Inbox |
| **Campaigns** | ❌ rota sem página |
| **Reports** | ❌ rota sem página |
| **Chat real-time** | ⚠️ REST+React Query, sem WebSocket/SSE |

### Banco de Dados
- V001–V049 aplicadas. **Notificações** (V047) e **permissões IA** (V049) presentes.
- Omnichannel (V044) e Workflows (V041/V043) presentes.

## Métricas Gerais
- **Sprints:** 49 total · 40 ✅ · 1 🚧 · 3 ⏳ · 5 ↪️
- **Migrações Flyway:** V001–V049
- **Deploy:** produção na VPS via GHCR (backend, auth-service, frontend), pipelines CI/CD verdes

## Próximos Passos
1. 🔴 **Campanhas (17)** — backend + frontend + rota `/campaigns`
2. 🔴 **Automações Omnichannel (18)**
3. 🟠 **Analytics / Dashboard avançado (19)**
4. 🟡 Fechamento Sprint 16 (deploy/VPS WhatsApp)
5. 🟡 E-mail real nas notificações (hoje console fake) + opcional: conectar frontend ao STOMP

---

*Atualizado em: 2026-08-17*