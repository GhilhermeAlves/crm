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
| 4.1 | Infraestrutura Auth | ↪️ Absorvida | 2026-08-09 | AI Agent | Sprint 3.2 |
| 4.2 | Usuários | ↪️ Absorvida | 2026-08-09 | AI Agent | 4.1 |
| 4.3 | Login | ↪️ Absorvida | 2026-08-09 | AI Agent | 4.1, 4.2 |
| 4.4 | Frontend Auth | ↪️ Absorvida | 2026-08-09 | AI Agent | 4.3 |
| 4.5 | Testes Auth | ↪️ Absorvida | 2026-08-09 | AI Agent | 4.3, 4.4 |

> **Fase 4.x (Infraestrutura Auth) — absorvida pela arquitetura Keycloak/OIDC (Sprints 5–7).**
> O plano original da fase 4 (auth monolítico em Spring Security/JWT: 4.1 infra, 4.2 usuários,
> 4.3 login, 4.4 frontend auth, 4.5 testes auth) foi **substituído pelo Access Gateway OIDC** com
> Keycloak. A fase Segurança (5, 6.0–6.10) entregou tenant/RLS, gateway de autenticação e sessão
> Redis; a fase Identidade (7.x) entregou login Google, account linking, telefone/OTP e recuperação
> de conta. 4.1 foi encerrada em 2026-07-15 (REPORT próprio, 93/100) e 4.3 teve Review aprovado,
> mas suas entregas (JWT próprio, CORS aberto, `permitAll`) foram substituídas/descontinuadas pelo
> gateway. **4.2/4.4/4.5 não serão executadas como fase própria** — seu escopo está coberto por
> 5–7.x. Consolidação registrada em 2026-08-09.

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
| 7.2 | Account Linking | ✅ Concluída | 2026-08-08 | AI Agent | 7.1 |
| 7.3 | Telefone/OTP | ✅ Concluída | 2026-08-08 | AI Agent | 7.1 |
| 7.4 | Telefone → OTP → senha Keycloak (login completo por telefone na UI, catálogo `phone-enabled`, rota nginx direta) | ✅ Concluída | 2026-08-08 | AI Agent | 7.2, 7.3 |
| 7.5 | Recuperação de conta — forgot/reset-password com reset REAL no Keycloak (service account `crm-keycloak-admin`, rotas nginx diretas, RLS V027/V028) | ✅ Concluída | 2026-08-08 | AI Agent | 7.3, 7.4 |

> **7.2 — Account Linking ✅ Concluída (2026-08-08).** Débito registrado em 2026-08-08 resolvido:
> - ✅ `sprints/7.2/REPORT.md` criado (este fechamento);
> - ✅ E2E em produção validado na VPS (Redis ativo, sobrevivência a reinício, expiração lógica,
>   CSRF 403, senha incorreta 401 sem consumo);
> - ✅ `RedisPendingLinkStore` (chave `gateway:pending-link:<token>`, TTL nativo, uso único)
>   alinhado a `AUTH_GATEWAY_SESSION_STORE=redis`; `InMemoryPendingLinkStore` condicional;
> - ✅ `/auth/link-status` e `/auth/link` com testes unitários (`GatewayOidcLinkingTest`,
>   `OidcGatewayControllerTest`, `RedisPendingLinkStoreTest`);
> - ✅ Bug CSRF crítico corrigido: `GatewayCsrfFilter` agora registrado também para `/auth/link`
>   (commit `8851595`).

## SaaS

| Sprint | Nome | Status | Data | Responsável | Dependência |
|--------|------|--------|------|-------------|-------------|
| 8 | Empresas | ✅ Concluída | 2026-08-09 | AI Agent | 7.5 |
| 8.1 | Company Foundation | ✅ Concluída | 2026-08-09 | AI Agent | 8 (plano) |
| 8.2 | Membership | ⏳ Pendente | — | — | 8.1 |
| 8.3 | Onboarding | ⏳ Pendente | — | — | 8.2 |
| 8.4 | Company Switcher | ⏳ Pendente | — | — | 8.2 |
| 8.5 | Invitations | ⏳ Pendente | — | — | 8.2, 8.3 |
| 8.6 | SaaS Hardening | ⏳ Pendente | — | — | 8.4, 8.5 |

> **Planejamento (próxima sprint — não implementar nesta etapa):** a Sprint 8 será dividida
> internamente em **8.1 Company Foundation · 8.2 Membership · 8.3 Onboarding ·
> 8.4 Company Switcher · 8.5 Invitations · 8.6 SaaS Hardening** (padrão decimal já adotado em
> 6.x/7.x). Plano detalhado (entregas, critérios de aceite, decisões de escopo D1–D6 e
> dependências externas) em **`sprints/8/SPRINT_PLAN.md`**.

> **8.1 — Company Foundation ✅ Concluída (2026-08-09).**
> - ✅ Empresa como entidade de primeiro plano: `CompanyController` passa a mapear
>   `/api/v1/companies` com alias de compatibilidade `/api/v1/tenants`;
> - ✅ Leitura por membro (`GET /`, `/me`, `/{id}` com `isAuthenticated()` + escopo no serviço),
>   leitura cross-tenant restrita a SUPER_ADMIN;
> - ✅ API de settings da empresa (`GET/PUT /companies/{id}/settings`, `settings:view/update`,
>   escopo restrito à própria empresa);
> - ✅ `companies.max_contacts` (V029, default 500) exposto no modelo;
> - ✅ Bug corrigido: upsert de settings gerava `StaleObjectStateException` (500) —
>   `Persistable` + `existsById` para decidir `persist` vs `merge`;
> - ✅ Suíte backend verde (125 testes) + E2E em produção **33/33 PASS**;
> - 📄 `sprints/8.1/REPORT.md`.

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

| Fase | Total | ✅ Concluída | 🚧 Em andamento | ⏳ Pendente | ↪️ Absorvida |
|------|-------|-------------|-----------------|-------------|--------------|
| Planejamento | 3 | 3 | 0 | 0 | 0 |
| Knowledge Layer | 3 | 3 | 0 | 0 | 0 |
| Infraestrutura | 5 | 0 | 0 | 0 | 5 |
| Segurança | 12 | 12 | 0 | 0 | 0 |
| Identidade / Autenticação | 6 | 6 | 0 | 0 | 0 |
| SaaS | 7 | 2 | 0 | 5 | 0 |
| CRM | 4 | 0 | 0 | 4 | 0 |
| Omnichannel | 3 | 0 | 0 | 3 | 0 |
| Analytics | 1 | 0 | 0 | 1 | 0 |
| IA | 1 | 0 | 0 | 1 | 0 |
| **Total** | **45** | **26** | **0** | **14** | **5** |

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

*Última atualização: 2026-08-09 — Sprint **8.1 (Company Foundation)** concluída com E2E em produção
33/33 PASS; resumo agora 45 sprints: 26 ✅, 0 🚧, 14 ⏳, 5 ↪️. Próxima sprint:
**8.2 — Membership** (SaaS), detalhada em `sprints/8/SPRINT_PLAN.md`.*