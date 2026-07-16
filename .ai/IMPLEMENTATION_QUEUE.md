# Gerenciador Mestre de Execução do Projeto

**Projeto:** CRM SaaS Omnichannel
**Versão:** 1.1.0
**Última Atualização:** 2026-07-15

---

## 1. Visão Geral das Sprints

| Sprint | Fase | Módulo | Status | Dependências | Playbook | Contexto |
|--------|------|--------|--------|--------------|----------|----------|
| 0 | Planejamento | Documentação | ✅ Concluída | — | — | — |
| 1 | Planejamento | Fundação | ✅ Concluída | — | — | — |
| 2 | Planejamento | Correções | ✅ Concluída | — | — | — |
| 3.1 | Knowledge Layer | Navegação IA | ✅ Concluída | — | — | — |
| 3.2 | AI Runtime Layer | Memória Projeto | ✅ Concluída | — | — | — |
| 3.3 | Sprint Management | Sprint Layer | ✅ Concluída | — | — | — |
| 4.1 | Infraestrutura | Auth Backend | ✅ Concluída | Nenhuma | implement-auth.md | auth.context.md |
| 4.2 | Infraestrutura | Usuários | ⏳ Pendente | 4.1 | implement-users.md | user.context.md |
| 4.3 | Infraestrutura | Login | ✅ Concluída | 4.1, 4.2 | implement-auth.md | auth.context.md |
| 4.4 | Infraestrutura | Frontend Auth | ⏳ Pronta para iniciar | 4.3 | implement-auth.md | auth.context.md |
| 4.5 | Infraestrutura | Testes Auth | ⏳ Pendente | 4.3, 4.4 | implement-auth.md | auth.context.md |
| 5 | Segurança | Tenant | ⏳ Pendente | 4.1 | implement-company.md | tenant.context.md |
| 6 | SaaS | Empresas | ⏳ Pendente | 5 | implement-company.md | tenant.context.md |
| 7 | CRM | Contatos | ⏳ Pendente | 6 | implement-contact.md | contact.context.md |
| 8 | CRM | Leads | ⏳ Pendente | 7 | implement-lead.md | lead.context.md |
| 9 | CRM | Pipeline | ⏳ Pendente | 7 | implement-pipeline.md | pipeline.context.md |
| 10 | CRM | Conversas | ⏳ Pendente | 7 | implement-chat.md | conversation.context.md |
| 11 | Omnichannel | WhatsApp | ⏳ Pendente | 10 | implement-whatsapp.md | whatsapp.context.md |
| 12 | Omnichannel | Campanhas | ⏳ Pendente | 10 | implement-automation.md | campaign.context.md |
| 13 | Omnichannel | Automações | ⏳ Pendente | 12 | implement-automation.md | automation.context.md |
| 14 | Analytics | Dashboard | ⏳ Pendente | 8, 9, 10 | implement-dashboard.md | dashboard.context.md |
| 15 | IA | IA Features | ⏳ Pendente | 10 | implement-ai.md | ai.context.md |

---

## 2. Agrupamento por Fases

### Fase 0: Planejamento ✅
| Sprint | Módulo | Status |
|--------|--------|--------|
| 0 | Documentação oficial (43+ arquivos) | ✅ Concluída |
| 1 | Fundação (backend, frontend, docker, CI/CD) | ✅ Concluída |
| 2 | Correções e melhorias (55 fixes) | ✅ Concluída |

### Fase 1: Knowledge Layer ✅
| Sprint | Módulo | Status |
|--------|--------|--------|
| 3.1 | Camada de Navegação IA (61 arquivos) | ✅ Concluída |
| 3.2 | AI Runtime Layer (18 arquivos) | ✅ Concluída |
| 3.3 | Sprint Management Layer (13 arquivos) | ✅ Concluída |

### Fase 2: Infraestrutura ⏳
| Sprint | Módulo | Status |
|--------|--------|--------|
| 4.1 | Auth Backend (domain, infrastructure, JWT, Spring Security) | ✅ Concluída |
| 4.2 | Usuários (CRUD, convites, roles) | ✅ Concluída |
| 4.3 | Login (refresh token, logout) | ✅ Concluída |
| 4.4 | Frontend Auth (login, provider, routes) | ⏳ Pronta para iniciar |
| 4.5 | Testes Auth (unit, integration) | ⏳ Pendente |

### Fase 3: Segurança ⏳
| Sprint | Módulo | Status |
|--------|--------|--------|
| 5 | Tenant (multi-tenancy, schema-per-tenant) | ⏳ Pendente |

### Fase 4: SaaS ⏳
| Sprint | Módulo | Status |
|--------|--------|--------|
| 6 | Empresas (CRUD, settings, subscriptions) | ⏳ Pendente |

### Fase 5: CRM ⏳
| Sprint | Módulo | Status |
|--------|--------|--------|
| 7 | Contatos (CRUD, endereços, custom fields, tags) | ⏳ Pendente |
| 8 | Leads (CRUD, scoring, lifecycle) | ⏳ Pendente |
| 9 | Pipeline (stages, kanban, opportunities) | ⏳ Pendente |
| 10 | Conversas (chat, mensagens, WebSocket) | ⏳ Pendente |

### Fase 6: Omnichannel ⏳
| Sprint | Módulo | Status |
|--------|--------|--------|
| 11 | WhatsApp (Evolution API, Meta API, templates) | ⏳ Pendente |
| 12 | Campanhas (campaigns, steps, sending) | ⏳ Pendente |
| 13 | Automações (triggers, actions, RabbitMQ) | ⏳ Pendente |

### Fase 7: Analytics ⏳
| Sprint | Módulo | Status |
|--------|--------|--------|
| 14 | Dashboard (KPIs, charts, real-time, cache) | ⏳ Pendente |

### Fase 8: IA ⏳
| Sprint | Módulo | Status |
|--------|--------|--------|
| 15 | IA Features (OpenAI, suggest-reply, scoring) | ⏳ Pendente |

---

## 3. Matriz de Dependências

```
4.1 Auth Backend ─────────────────────────────────────────────┐
    │                                                        │
    ├─→ 4.2 Usuários ─→ 4.3 Login ─→ 4.4 Frontend Auth ─→ 4.5 Testes
    │                        │
    ├─→ 5 Tenant ─→ 6 Empresas ─→ 7 Contatos ─┬→ 8 Leads ─────┐
    │                                          │               │
    │                                          ├→ 9 Pipeline ──┤
    │                                          │               │
    │                                          └→ 10 Conversas ─┬→ 11 WhatsApp
    │                                                          ├→ 12 Campanhas ─→ 13 Automações
    │                                                          │
    │                                                          └→ 14 Dashboard (depende de 8, 9, 10)
    │
    └─────────────────────────────────────────────────────────→ 15 IA (depende de 10)
```

### Legenda
- `─→` dependência direta
- `┬→` ramificação (depende do anterior)
- `└→` último na cadeia

---

## 4. Critérios de Conclusão

### Para considerar uma Sprint CONCLUÍDA:

#### Backend
- [ ] Domain entities criadas com value objects
- [ ] Application services com casos de uso
- [ ] Infrastructure implementations (repositories, mappers)
- [ ] REST controllers com DTOs
- [ ] Migration Flyway criada e testada
- [ ] Testes unitários (≥80% coverage)
- [ ] Testes de integração (≥60% coverage)
- [ ] Lint sem erros (`mvn compile`)
- [ ] Swagger documentado

#### Frontend
- [ ] Components criados (≤200 linhas cada)
- [ ] Hooks criados (useQuery/useMutation)
- [ ] Pages criadas com layouts
- [ ] Types definidos
- [ ] Validações Zod implementadas
- [ ] Lint sem erros (`npm run lint`)
- [ ] TypeScript sem erros (`npm run typecheck`)

#### Documentação
- [ ] `docs/CHANGELOG.md` atualizado
- [ ] `IMPLEMENTATION_REPORT.md` atualizado
- [ ] `.ai/LAST_SESSION.md` atualizado
- [ ] `.ai/WORKLOG.md` atualizado
- [ ] `.ai/CURRENT_TASK.md` atualizado

#### Qualidade
- [ ] Código segue Clean Architecture
- [ ] Sem duplicação de lógica
- [ ] Sem secrets no código
- [ ] Naming conventions seguidas
- [ ] Soft delete implementado (deleted_at)
- [ ] UUID v4 como PK
- [ ] created_at/updated_at presentes

---

## 5. Próxima Sprint Automática

### 🎯 Sprint 4.1 — Infraestrutura Auth

| Campo | Valor |
|-------|-------|
| **Sprint** | 4.1 |
| **Fase** | Infraestrutura |
| **Módulo** | Auth Backend |
| **Playbook** | `playbooks/implement-auth.md` |
| **Contexto** | `contexts/auth.context.md` |
| **Documentação Oficial** | `docs/01-backend/Auth.md`, `docs/05-business-rules/Permissions.md` |
| **Status** | ⏳ Pendente |
| **Dependências** | Nenhuma |
| **Prioridade** | 🔴 Alta |

#### Objetivo
Criar domain entities, infrastructure, JWT, RBAC para o módulo de autenticação.

#### Arquivos que serão criados

##### Backend
- `backend/src/main/java/com/becommerce/crm/domain/identity/` — Domain entities
  - `User.java` — Entidade usuário
  - `RefreshToken.java` — Entidade refresh token
  - `Role.java` — Entidade role
  - `UserRole.java` — Entidade user_role
  - `valueobject/` — Value objects (Email, Password, etc.)
  - `event/` — Domain events
  - `exception/` — Domain exceptions

- `backend/src/main/java/com/becommerce/crm/application/identity/` — Application services
  - `port/` — Input/Output ports
  - `service/` — Application services
  - `dto/` — Request/Response DTOs

- `backend/src/main/java/com/becommerce/crm/infrastructure/identity/` — Infrastructure
  - `persistence/` — JPA repositories
  - `security/` — JWT provider, filters
  - `mapper/` — MapStruct mappers

- `backend/src/main/java/com/becommerce/crm/presentation/rest/identity/` — REST
  - `AuthController.java` — Login, refresh, logout
  - `UserController.java` — CRUD usuários

- `backend/src/main/resources/db/migration/V002__auth_tables.sql` — Migration

##### Frontend
- Nenhum (Sprint 4.4)

#### Arquivos proibidos
- `docs/**` — Documentação oficial
- `docs-ai/**` — Knowledge Layer
- `contexts/**` — Contextos
- `playbooks/**` — Playbooks
- `prompts/**` — Prompts
- `frontend/**` — Código frontend (Sprint 4.4)

---

## 6. Status Padronizados

| Ícone | Status | Significado |
|-------|--------|-------------|
| ⏳ | Pendente | Sprint ainda não iniciada |
| 🚧 | Em andamento | Sprint em execução |
| ✅ | Concluída | Sprint completa e validada |
| ⛔ | Bloqueada | Sprint bloqueada por dependência |
| ❌ | Cancelada | Sprint cancelada |

---

## 7. Regras de Sequenciamento

1. **Auth primeiro** — Tudo depende de autenticação
2. **Tenant depois de Auth** — Multi-tenancy requer JWT
3. **Empresas depois de Tenant** — Companies precisam de schema
4. **Contatos depois de Empresas** — Contacts são por empresa
5. **Leads/Conversas depois de Contatos** — Referenciam contacts
6. **Pipeline depois de Leads** — Opportunities são por lead
7. **WhatsApp depois de Conversas** — Integra com mensagens
8. **Campanhas depois de WhatsApp** — Envia mensagens
9. **Automações depois de Campanhas** — Dispara campanhas
10. **Dashboard/IA no final** — Consomem dados de todos

---

## 8. Métricas de Progresso

| Fase | Total | Concluídas | Em Andamento | Pendentes |
|------|-------|------------|--------------|-----------|
| Planejamento | 3 | 3 | 0 | 0 |
| Knowledge Layer | 3 | 3 | 0 | 0 |
| Infraestrutura | 5 | 3 | 0 | 2 |
| Segurança | 1 | 0 | 0 | 1 |
| SaaS | 1 | 0 | 0 | 1 |
| CRM | 4 | 0 | 0 | 4 |
| Omnichannel | 3 | 0 | 0 | 3 |
| Analytics | 1 | 0 | 0 | 1 |
| IA | 1 | 0 | 0 | 1 |
| **Total** | **22** | **9** | **0** | **13** |

**Progresso Geral:** 41% (9/22)

---

## 9. Histórico de Alterações

| Data | Sprint | Alteração |
|------|--------|-----------|
| 2026-07-15 | 4.3D | Encerramento da Sprint 4.3 — Login (nota 93/100) |
| 2026-07-15 | 4.2 | Sprint 4.2 — User Identity Foundation |
| 2026-07-15 | 4.1D | Encerramento da Sprint 4.1 |
| 2026-07-15 | 4.1C | Revisão aprovada (93/100) |
| 2026-07-15 | 4.1B | Infraestrutura Spring Security + JWT + Exception Handling |
| 2026-07-15 | 4.1A | Planejamento formal da Sprint 4.1 |
| 2026-07-15 | 3.3 | Criação da Sprint Management Layer |
| 2026-07-15 | 3.2 | Criação do Gerenciador Mestre |
| 2026-07-15 | 3.1 | Criação da Knowledge Layer |
| 2026-07-15 | 1 | Fundação do Projeto |

---

*Última atualização: 2026-07-15*
