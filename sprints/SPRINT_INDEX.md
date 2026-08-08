# Índice de Sprints

> **Documento oficial do roadmap e do estado das Sprints deste projeto — fonte de verdade.**
> Código implementado **sem** atualização deste índice **não** contabiliza Sprint concluída.
> A atualização do índice faz parte da própria Sprint (ver `Definition of Done` no fim).

## Convenção de numeração

| Faixa | Área |
|-------|------|
| 0 – 3.3 | Fundação / Knowledge Layer |
| 4.x | Infraestrutura |
| 5 – 6.10 | Segurança (Tenant + Access Gateway / OIDC) |
| **7.x** | **Identidade / Autenticação** |
| 8 | SaaS — Empresas |
| 9 | CRM — Contatos |
| 10 | CRM — Leads |
| 11 | CRM — Pipeline |
| 12 | CRM — Conversas |
| 13 | Omnichannel — WhatsApp |
| 14 | Omnichannel — Campanhas |
| 15 | Omnichannel — Automações |
| 16 | Analytics — Dashboard |
| 17 | IA |

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
| 6 | Access Gateway | ✅ Concluída | 2026-08-02 | AI Agent | 5 |
| 6.1 | Gateway OIDC (authorize/callback) | ✅ Concluída | 2026-08-02 | AI Agent | 6 |
| 6.2 | Gateway OIDC (logout + ciclo de vida da sessão) | ✅ Concluída | 2026-08-02 | AI Agent | 6.1 |
| 6.3 | Gateway OIDC (integração de produção / E2E) | ✅ Concluída | 2026-08-03 | AI Agent | 6.2 |
| 6.4 | Migração do frontend para o Access Gateway (BFF relay) | ✅ Concluída | 2026-08-04 | AI Agent | 6.3 |
| 6.5 | Gateway OIDC (hardening, observabilidade e correções) | ✅ Concluída | 2026-08-04 | AI Agent | 6.4 |
| 6.6 | Gateway OIDC (health/readiness, correlation ID e rate limiting) | ✅ Concluída | 2026-08-04 | AI Agent | 6.5 |
| 6.7 | Gateway OIDC (rate limit do relay `/api/*` por usuário autenticado) | ✅ Concluída | 2026-08-04 | AI Agent | 6.6 |
| 6.8 | Gateway OIDC (hardening, concorrência do rate limiting e fechamento) | ✅ Concluída | 2026-08-05 | AI Agent | 6.7 |
| 6.9 | Gateway OIDC (auditoria final de segurança/arquitetura, correção do login manual e fechamento) | ✅ Concluída | 2026-08-05 | AI Agent | 6.8 |
| 6.10 | Production Infrastructure Hardening & Final Closure (etapa auth/Gateway encerrada) | ✅ Concluída | 2026-08-05 | AI Agent | 6.9 |

## Identidade / Autenticação

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 7.0 | Identity Provider Architecture (catálogo de provedores + `kc_idp_hint` + fundação do login) | ✅ Concluída | 2026-08-05 | AI Agent | 6.10 |
| 7.1 | Login/Cadastro com Google | ✅ Concluída | 2026-08-06 | AI Agent | 7.0 |
| 7.2 | Account Linking | 🚧 Em andamento | — | AI Agent | 7.1 |
| 7.3 | Telefone/OTP | ✅ Concluída | 2026-08-08 | AI Agent | 7.1 |
| 7.4 | Telefone → OTP → senha Keycloak (login completo por telefone na UI, catálogo `phone-enabled`, rota nginx direta) | ✅ Concluída | 2026-08-08 | AI Agent | 7.2, 7.3 |
| 7.5 | Recuperação de conta — forgot/reset-password com reset REAL no Keycloak (service account `crm-keycloak-admin`, rotas nginx diretas, RLS V027/V028) | ✅ Concluída | 2026-08-08 | AI Agent | 7.3, 7.4 |

> **7.2 — débito real registrado (NÃO pode ser marcada ✅ enquanto estiver pendente):**
> - 🚧 **Falta** `sprints/7.2/REPORT.md` (+ REVIEW/RETROSPECTIVE — não existe pasta `sprints/7.2/`);
> - 🚧 **Sem validação E2E em produção** (sprints irmãs 7.1/7.3/7.4/7.5 têm E2E documentado na VPS);
> - 🚧 `PendingLinkStore` é apenas **in-memory** (`InMemoryPendingLinkStore`) enquanto prod usa
>   `AUTH_GATEWAY_SESSION_STORE=redis` — falta alinhamento do armazenamento da pendência com o store externo;
> - 🚧 `/auth/link-status` e `/auth/link` **sem testes unitários** no auth-service (branche
>   `LinkingRequired` de `CurrentUserResolutionService` sem cobertura);
> - 🚧 Reports 7.3/7.4 registram explicitamente: *"Account linking visual (7.2) ainda pendente de
>   fechamento / validação em produção"*.

## SaaS

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 8 | Empresas | ⏳ Pendente | — | — | 7.5 |

> **Planejamento (próxima sprint — não implementar nesta etapa):** a Sprint 8 poderá ser dividida
> internamente em **8.1 Company Foundation · 8.2 Membership · 8.3 Onboarding · 8.4 Company Switcher ·
> 8.5 Invitations · 8.6 SaaS Hardening** (padrão decimal já adotado em 6.x/7.x — detalhar quando iniciar).

## CRM

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 9 | Contatos | ⏳ Pendente | — | — | 8 |
| 10 | Leads | ⏳ Pendente | — | — | 9 |
| 11 | Pipeline | ⏳ Pendente | — | — | 9 |
| 12 | Conversas | ⏳ Pendente | — | — | 9 |

## Omnichannel

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 13 | WhatsApp | ⏳ Pendente | — | — | 12 |
| 14 | Campanhas | ⏳ Pendente | — | — | 12 |
| 15 | Automações | ⏳ Pendente | — | — | 14 |

## Analytics

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 16 | Dashboard | ⏳ Pendente | — | — | 9, 10, 12 |

## IA

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 17 | IA | ⏳ Pendente | — | — | 12 |

---

## Resumo

| Fase | Total | ✅ Concluída | 🚧 Em andamento | ⏳ Pendente |
|------|-------|-------------|-----------------|-------------|
| Planejamento | 3 | 3 | 0 | 0 |
| Knowledge Layer | 3 | 3 | 0 | 0 |
| Infraestrutura | 5 | 0 | 1 | 4 |
| Segurança | 12 | 12 | 0 | 0 |
| Identidade / Autenticação | 6 | 5 | 1 | 0 |
| SaaS | 1 | 0 | 0 | 1 |
| CRM | 4 | 0 | 0 | 4 |
| Omnichannel | 3 | 0 | 0 | 3 |
| Analytics | 1 | 0 | 0 | 1 |
| IA | 1 | 0 | 0 | 1 |
| **Total** | **39** | **23** | **2** | **14** |

---

## Definition of Done (obrigatório para toda Sprint)

Uma Sprint **somente** pode receber **`✅ Concluída`** quando **todos** os itens abaixo
forem atendidos (e, ao concluir, **a atualização do `SPRINT_INDEX.md` faz parte da própria
Sprint** — nunca é tarefa posterior/opcional):

- [ ] Implementação concluída
- [ ] Testes concluídos (suítes verdes)
- [ ] Build validado
- [ ] Integração validada
- [ ] E2E realizado quando aplicável
- [ ] Produção / VPS validada quando aplicável
- [ ] Documentação atualizada (inclui `sprints/[N]/REPORT.md` etc.)
- [ ] Migrações validadas quando aplicável
- [ ] Git commit realizado
- [ ] Working tree limpo/validado
- [ ] Débitos conhecidos registrados
- [ ] `SPRINT_INDEX.md` atualizado (status ✅, data real, responsável, resumo e última atualização)

**Fluxo obrigatório (não iniciar a próxima Sprint antes do fim da anterior):**

```
Implementar → Testar → Validar → Documentar → Commit → Atualizar SPRINT_INDEX.md → Marcar ✅ Concluída → Iniciar próxima Sprint
```

---

*Última atualização: 2026-08-08 — Sprint 7.5 (Recuperação de conta) concluída; numeração alinhada
(7.x = Identidade/Autenticação; 8=Empresas; 9–12 = CRM; 13–15 = Omnichannel; 16=Dashboard; 17=IA);
7.2 (Account Linking) permanece 🚧 Em andamento com débito registrado —
não está concluída até haver `sprints/7.2/REPORT.md`, validação E2E em produção, cobertura de teste
nos endpoints de link e alinhamento do store de pendência com Redis.*