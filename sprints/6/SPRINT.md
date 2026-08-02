# Sprint 6 — Access Gateway

## Identificação
- **Sprint:** 6
- **Nome:** Access Gateway
- **Data Início:** 2026-08-02
- **Data Fim:** —
- **Status:** 🚧 Em andamento
- **Responsável:** AI Agent
- **Fase:** Segurança

## Objetivo
Evoluir a arquitetura de autenticação para um **CRM Access Gateway**: o Frontend passa a
entrar pela camada do Auth Service (Authorization Code + PKCE, orquestração de identidade,
sessão e **CRM Access**), o Keycloak continua como IdP/Authorization Server **exclusivo**
(nunca substituído) e o acesso ao CRM passa a ser uma decisão **explícita e independente**
da autenticação no Keycloak. Nenhum fluxo atual é quebrado antes de o novo ser validado.

## Escopo
- **Auditoria** do fluxo de autenticação atual (frontend → Keycloak OIDC + PKCE direto;
  Auth Service stateless; backend resource server; modelo de dados) — concluída na fase 1.
- **Documentação** da Sprint 6 (arquitetura, fluxos, CRM Access, segurança, relatório) —
  concluída na fase 1.
- **Modelo de dados**: flag explícita de CRM access (`users.crm_enabled`) + gate de
  `companies.status = ACTIVE`. **Não** criar tabela `user_application_access`.
- **Provisionamento separado de concessão de acesso**: identidade provisionada **não**
  concede acesso automaticamente; `crm_enabled` é decisão explícita.
- **Auth Service como gateway OIDC** (client Authorization Code + PKCE, sessão de browser,
  callback, logout OIDC coerente) mantendo a resolução de `CurrentUser` atual.
- **Frontend**: login/callback/logout passam pelo Auth Service; endurecimento de storage
  (tokens para cookie HttpOnly).
- **Testes** unitários, de integração e **E2E na VPS** (login, CRM access negado, usuário
  inativo, empresa suspensa, tenants A/B).

## Não-Objetivos (proibido nesta sprint)
- Substituir o Keycloak como IdP/Authorization Server ou criar emissor de tokens próprio.
- Usar Password Grant / Resource Owner Password Credentials como fluxo principal.
- Armazenar senhas no CRM (senhas continuam exclusivas do Keycloak).
- Criar tabela `user_application_access`.
- Duplicar infra local; montar outro compose; infra sempre na VPS (`ssh crm-vps`).
- Confiar em `companyId`/tokens arbitrários vindos do frontend.
- Conceder CRM access automaticamente por existir no Keycloak.

## Sub-arquivos

| Arquivo | Finalidade | Status |
|---------|-----------|--------|
| SPRINT.md | Este arquivo | ✅ |
| ARCHITECTURE.md | Arquitetura anterior vs nova; componentes alterados vs preservados | ✅ |
| AUTH_FLOW.md | Fluxos de autenticação (login OIDC+PKCE via gateway, callback, sessão, logout) | ✅ |
| CRM_ACCESS.md | Modelo e decisão de CRM Access (flag + company ACTIVE; gates; testes) | ✅ |
| SECURITY.md | Matriz de segurança (CSRF, XSS, cookies, CORS, open redirect, replay, logout) | ✅ |
| REPORT.md | Relatório final com evidências (será preenchido ao longo da implementação) | 📝 |

## Dependências
- Sprint 5 — Tenant (RLS FORCE, `crm_app` NOBYPASSRLS, provisioning `AUTH_DEFAULT_COMPANY_ID`)
- Sprint 4.1 — Infraestrutura Auth

---

*Data: 2026-08-02*
