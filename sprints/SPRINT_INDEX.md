# Índice de Sprints

## Planejamento

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 0 | Planejamento | ✅ Concluída | 2026-07-15 | Architect | — |
| 1 | Fundação | ✅ Concluída | 2026-07-15 | Architect | Sprint 0 |
| 2 | Correções | ✅ Concluída | 2026-07-15 | Architect | Sprint 1 |

## Knowledge Layer

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 3.1 | Knowledge Layer | ✅ Concluída | 2026-07-15 | Architect | Sprint 2 |
| 3.2 | AI Runtime Layer | ✅ Concluída | 2026-07-15 | Architect | Sprint 3.1 |
| 3.3 | Sprint Management Layer | ✅ Concluída | 2026-07-15 | AI Agent | Sprint 3.2 |

## Infraestrutura

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 4.1 | Infraestrutura Auth | 🚧 Em andamento | 2026-07-15 | AI Agent | Sprint 3.2 |
| 4.2 | Usuários | ⏳ Pendente | — | — | 4.1 |
| 4.3 | Login | ⏳ Pendente | — | — | 4.1, 4.2 |
| 4.4 | Frontend Auth | ⏳ Pendente | — | — | 4.3 |
| 4.5 | Testes Auth | ⏳ Pendente | — | — | 4.3, 4.4 |

## Segurança

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 5 | Tenant | ✅ Concluída | 2026-08-01 | AI Agent | 4.1 |
| 6 | Access Gateway | 🚧 Em andamento | 2026-08-02 | AI Agent | 5 |
| 6.1 | Gateway OIDC (authorize/callback) | ✅ Concluída | 2026-08-02 | AI Agent | 6 |
| 6.2 | Gateway OIDC (logout + ciclo de vida da sessão) | ✅ Concluída | 2026-08-02 | AI Agent | 6.1 |

## SaaS

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 7 | Empresas | ⏳ Pendente | — | — | 6 |

## CRM

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 8 | Contatos | ⏳ Pendente | — | — | 7 |
| 9 | Leads | ⏳ Pendente | — | — | 8 |
| 10 | Pipeline | ⏳ Pendente | — | — | 8 |
| 11 | Conversas | ⏳ Pendente | — | — | 8 |

## Omnichannel

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 12 | WhatsApp | ⏳ Pendente | — | — | 11 |
| 13 | Campanhas | ⏳ Pendente | — | — | 11 |
| 14 | Automações | ⏳ Pendente | — | — | 13 |

## Analytics

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 15 | Dashboard | ⏳ Pendente | — | — | 9, 10, 11 |

## IA

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 16 | IA | ⏳ Pendente | — | — | 11 |

---

## Resumo

| Fase | Total | ✅ Concluída | 🚧 Em andamento | ⏳ Pendente |
|------|-------|-------------|-----------------|-------------|
| Planejamento | 3 | 3 | 0 | 0 |
| Knowledge Layer | 3 | 3 | 0 | 0 |
| Infraestrutura | 5 | 0 | 1 | 4 |
| Segurança | 4 | 3 | 1 | 0 |
| SaaS | 1 | 0 | 0 | 1 |
| CRM | 4 | 0 | 0 | 4 |
| Omnichannel | 3 | 0 | 0 | 3 |
| Analytics | 1 | 0 | 0 | 1 |
| IA | 1 | 0 | 0 | 1 |
| **Total** | **25** | **9** | **2** | **14** |

---

*Última atualização: 2026-08-02*
