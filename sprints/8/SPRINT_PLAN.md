# Sprint 8 — Empresas (SaaS · Multi-tenant)

> Roteiro da evolução **multi-empresa** do CRM: fundação da empresa como entidade de primeiro
> plano, adesão de usuários, onboarding self-service, troca de empresa (company switcher),
> convites e endurecimento SaaS. **Decisão de escopo (2026-08-09):** a Sprint 8 será executada
> em 6 sub-sprints (8.1 → 8.6), seguindo o padrão decimal adotado em 6.x/7.x. Este documento é
> o **plano**; cada sub-sprint produz seu `REPORT.md` e atualiza o `SPRINT_INDEX.md`.

## Contexto da arquitetura atual (levantado em 2026-08-09)

O projeto já possui uma base **multi-tenant real** (Sprints 5–7), mas o modelo ainda é
**um usuário → uma empresa**, e a empresa é chamada de **tenant** na API/UI:

- **Isolamento:** schema compartilhado + **RLS** (GUC `app.current_company_id` via
  `TenantContext`/`TenantAwareDataSource`). `companies`, `permissions` e `role_permissions`
  são **globais** (sem RLS); as demais tabelas (users, roles, user_roles, contacts, leads,
  pipelines, ...) são isoladas por `company_id`.
- **Empresa:** tabela `companies` (V005) já tem CRUD completo em `CompanyController`
  (`/api/v1/tenants`) + UI admin em `/tenants` (restrita a SUPER_ADMIN). Campos de plano,
  status, limite de usuários/armazenamento já existem.
- **Usuário:** `users.company_id NOT NULL` — **um usuário pertence a uma única empresa**
  (decisão estrutural a ser relaxada em 8.2). Papéis por empresa em `roles`/`user_roles`
  (`(name, company_id)` único), seed por empresa via `RoleDataSeeder`
  (SUPER_ADMIN/ADMIN/MANAGER/AGENT/VIEWER).
- **Provisionamento:** identidade Keycloak/OIDC cria o usuário na empresa padrão
  (`AUTH_DEFAULT_COMPANY_ID`, papel `AUTH_DEFAULT_ROLE`=AGENT). Não há fluxo de "crie sua empresa".
- **Convites:** base já existe — `POST /api/v1/users/invite` + `POST /api/v1/users/accept-invite`,
  colunas `invite_token/invited_at/invited_by` em `users`. **Sem e-mail, sem tabela dedicada,
  sem UI.**
- **Assinatura:** apenas tabela `subscriptions` (plano/status/trial) + campos em `companies`;
  **sem cobrança/Stripe.**
- **Terminologia divergente:** a API/UI usa `/tenants`/`Empresas`; o domínio/banco usa
  `Company`/`companies`. A Sprint 8 precisa **reconciliar** a nomenclatura (decisão abaixo).

## Decisões de escopo (2026-08-09)

| # | Decisão | Decisão tomada |
|---|---------|----------------|
| D1 | **Terminologia** | Adotar **Company/Empresa** como nome canônico (domínio já é `Company`). `Tenant` vira sinônimo histórico/aliasing; rotas antigas `/api/v1/tenants` mantidas com **alias** de compatibilidade durante a sprint, com depreciação documentada. A UI passa a exibir "Empresas". |
| D2 | **Modelo de adesão** | Nova tabela **`memberships`** (user_id, company_id, role, joined_at, invited_by, status) como fonte de verdade da relação usuário↔empresa; `users.company_id` vira **empresa "ativa/padrão"** (denormalizada para compatibilidade + RLS). Relaxamento de `NOT NULL` feito com migração controlada. |
| D3 | **Company switcher** | Troca de empresa **recarrega o contexto** (novo `company_id` no JWT/contexto), sem navegação por URL de tenant. A empresa ativa persiste por sessão (Redis) e é atualizada no `CurrentUser`. |
| D4 | **Onboarding** | Fluxo self-service: usuário sem empresa → tela "Criar empresa" (novo tenant) OU "Entrar com convite". `AUTH_DEFAULT_COMPANY_ID` deixa de ser obrigatório para o fluxo de onboarding (fica como fallback apenas quando habilitado). |
| D5 | **Billing** | **FORA DE ESCOPO da 8.x.** Cobrança/Stripe fica para backlog pós-8.6 (bloqueio por gateway de pagamento externo). A 8.x usa apenas plano/status/trial do modelo existente. |
| D6 | **Convites** | Convites passam a ser **por empresa** (usuário já autenticado convida para a empresa em que é ADMIN/OWNER), com tabela `invitations` dedicada, e-mail via provedor transacional e aceite que cria a `membership`. |

## Estrutura das sub-sprints

| Sub-sprint | Nome | Objetivo principal | Dependência |
|-----------|------|--------------------|-------------|
| 8.1 | **Company Foundation** | Consolidação da entidade Company como produto: API/UI "Empresas", plano/status/limites, settings, unificação de nomenclatura, RLS auditado | 7.5 |
| 8.2 | **Membership** | Modelo de adesão multi-empresa: tabela `memberships`, relaxar `users.company_id`, RLS estendido, API de membresias e papéis por empresa | 8.1 |
| 8.3 | **Onboarding** | Fluxo self-service: criação de empresa na primeira entrada, convite como alternativa, perfil/workspace inicial | 8.2 |
| 8.4 | **Company Switcher** | Troca de empresa ativa (UI + backend), contexto por sessão, atualização de `CurrentUser`/permissões | 8.2 |
| 8.5 | **Invitations** | Tabela `invitations`, envio de e-mail (provedor transacional), convite por empresa, aceite → membership, expiração/revogação | 8.2, 8.3 |
| 8.6 | **SaaS Hardening** | Limites por plano (usuários/contatos/armazenamento), quotas, auditoria de tenant, revisão de segurança RLS/gateway, fechamento | 8.4, 8.5 |

---

## 8.1 — Company Foundation

- **Status:** ⏳ Planejada (não iniciada).
- **Objetivo:** transformar a empresa em entidade de primeiro plano e unificar nomenclatura.
- **Entregas planejadas:**
  - Renomear exposição "Tenants" → "Empresas" na UI; manter alias `/api/v1/tenants` → `/api/v1/companies` (compatibilidade + depreciação).
  - Endpoints de empresa completos: listagem por membro (não só SUPER_ADMIN), detalhe, atualização de perfil (nome, logo, dados de endereço), status/plano.
  - `company_settings` na API (timezone, locale, moeda, horário comercial, preferências).
  - Limites de plano no modelo (max_users, max_contacts, max_storage_mb) — leitura/exposição; **enforcement na 8.6**.
  - RLS auditado para as consultas de empresa por membro (novas policies para leitura de empresa pelo membro via `memberships` — depende de 8.2, então na 8.1 manter leitura via SUPER_ADMIN/company_id atual).
- **Critérios de aceite:**
  - UI "Empresas" lista empresas do usuário (hoje só admin global).
  - Nomenclatura unificada em código/docs novos; alias de compatibilidade documentado.
  - Backend: controllers/usecases `Company*` completos + testes; suítes verdes; E2E na VPS.
- **Saída:** `sprints/8.1/REPORT.md` + atualização do `SPRINT_INDEX.md`.

## 8.2 — Membership

- **Status:** ⏳ Planejada.
- **Objetivo:** habilitar **um usuário ↔ N empresas** com papel por empresa.
- **Entregas planejadas:**
  - Migração: tabela `memberships` (`id`, `user_id FK`, `company_id FK`, `role`, `status` [ACTIVE/PENDING/REMOVED], `invited_by`, `joined_at`, `created_at`); índice único `(user_id, company_id)`; backfill a partir de `users.company_id` + `user_roles` (o usuário atual vira membro da sua empresa com papel vigente).
  - Relaxar `users.company_id` (tornar nullable ou manter NOT NULL como "empresa ativa" denormalizada — decisão técnica na sprint; recomenda-se manter NOT NULL + denormalização com consistência via trigger).
  - RLS: novas policies para `memberships` (membro enxerga apenas suas memberships; empresa enxerga membros apenas se for membro ativo); ajustar policies existentes (users/roles) para considerar membership ativa.
  - API: `GET /api/v1/companies/{id}/members`, `GET /api/v1/me/memberships`, `PUT/DELETE` de membro por ADMIN/OWNER (desligar/rebaixar).
  - `CurrentUser` ganha `membershipRole` por empresa e a lista de memberships consultável.
- **Critérios de aceite:**
  - Backfill correto em staging/VPS (usuários existentes viram membros).
  - RLS FORCE mantido: sem vazamento entre empresas; membro desligado perde acesso.
  - Testes de integração de RLS para memberships.
- **Saída:** `sprints/8.2/REPORT.md` + índice.

## 8.3 — Onboarding

- **Status:** ⏳ Planejada.
- **Objetivo:** primeira experiência self-service (usuário sem empresa).
- **Entregas planejadas:**
  - Fluxo "Criar empresa": formulário (nome, CNPJ/identificação, plano inicial STARTER, dados opcionais) → cria `companies` + `memberships` (OWNER) + define como empresa ativa + seed de papéis.
  - Fluxo alternativo "Já tenho convite": redireciona para aceite de convite (aceite completo na 8.5).
  - Detecção de usuário sem empresa após login (provisionado sem `AUTH_DEFAULT_COMPANY_ID`) → tela de onboarding.
  - Página de perfil/workspace inicial (settings básicos: nome do usuário, empresa recém-criada).
  - Gate: usuário sem empresa não acessa módulos CRM (redirect para onboarding).
- **Critérios de aceite:**
  - Novo usuário Google/OTP sem empresa padrão cai no onboarding e consegue criar empresa ponta-a-ponta.
  - Empresa criada já nasce com papéis seed e membership OWNER.
  - E2E em produção.
- **Saída:** `sprints/8.3/REPORT.md` + índice.

## 8.4 — Company Switcher

- **Status:** ⏳ Planejada.
- **Objetivo:** trocar a empresa ativa do usuário sem relogar.
- **Entregas planejadas:**
  - Backend: endpoint `POST /api/v1/me/switch-company {companyId}` que valida membership ativa e atualiza a empresa ativa (JWT/contexto/Redis); `GET /api/v1/me/companies` para o seletor.
  - Auth-service/gateway: contexto de empresa ativa por sessão (estender store de sessão Redis com `activeCompanyId`).
  - `CurrentUserResolution` e `TenantFilter` usam a empresa ativa da sessão quando o usuário alterna.
  - UI: seletor de empresa no `Header`/`UserMenu` (nome + logo), troca re-carrega permissões/módulos.
  - Cuidado de regressão: fluxo de login existente (`/dashboard`) nunca quebra.
- **Critérios de aceite:**
  - Troca de empresa atualiza contexto + permissões sem relogar; logout mantém estado correto.
  - RLS respeita a empresa ativa em todos os módulos.
- **Saída:** `sprints/8.4/REPORT.md` + índice.

## 8.5 — Invitations

- **Status:** ⏳ Planejada.
- **Objetivo:** convidar usuários para uma empresa de forma rastreável e por e-mail.
- **Entregas planejadas:**
  - Tabela `invitations` (`id`, `company_id`, `email`, `role`, `token` (hash), `invited_by`, `status` [PENDING/ACCEPTED/REVOKED/EXPIRED], `expires_at`, `created_at`); RLS FORCE por `company_id`.
  - API: `POST /api/v1/companies/{id}/invitations`, `POST /api/v1/invitations/{token}/accept`, `POST /api/v1/invitations/{token}/decline`, revogação, listagem (pendentes/aceitos).
  - Envio de e-mail via **provedor transacional** (abstração `EmailSender` — ex.: SMTP/Resend/SES; em dev `ConsoleEmailSender`; nunca loga token em prod).
  - Aceite: usuário logado (sem empresa ou outra empresa) → cria `memberships` ativa; link de convite na UI.
  - Migrar/reusar `users.invite_token` legado (deprecar ou manter compatibilidade).
- **Critérios de aceite:**
  - Convite ponta-a-ponta (criar → e-mail → aceitar → membro ativo) validado na VPS.
  - Token de uso único + expiração; convite revogável; sem log de token.
- **Saída:** `sprints/8.5/REPORT.md` + índice.

## 8.6 — SaaS Hardening

- **Status:** ⏳ Planejada.
- **Objetivo:** endurecimento multi-tenant e limites por plano.
- **Entregas planejadas:**
  - Enforcement de limites: max_users (bloqueia convite/membership além do limite), max_contacts (bloqueia criação de contato), max_storage_mb (quota de arquivos/MinIO) — com mensagens claras e upgrade sugerido.
  - Quotas e uso corrente expostos (`GET /api/v1/companies/{id}/usage`).
  - Auditoria de tenant: eventos de criação/troca/remoção de empresa e membership em `audit_logs`; trilha por `company_id`.
  - Revisão de segurança: policies RLS de todas as novas tabelas (memberships, invitations), fluxos de troca de empresa, CSRF, rate limit nos endpoints de convite/troca.
  - Documentação final de multi-tenancy (corrigir `docs/MULTI_TENANCY.md`, `DATABASE_MAP.md`, `BACKEND_MAP.md` — hoje descrevem o design pré-RLS) e fechamento da fase SaaS.
- **Critérios de aceite:**
  - Limites efetivamente aplicados (teste de limite de usuários/contatos).
  - Auditoria completa das ações de tenant.
  - Docs de multi-tenancy corrigidos para a arquitetura RLS real.
- **Saída:** `sprints/8.6/REPORT.md` + índice (fase SaaS encerrada).

---

## Fora do escopo da Sprint 8

- **Billing / Stripe / cobrança real** — plano/modelo de dados existente apenas; cobrança fica em backlog pós-8.6 (D5).
- **Whitelabel / temas por empresa** — backlog.
- **Microsoft/Apple IdP** — já fora de escopo na fase Identidade (7.x), permanece.
- **IA / automações / omnichannel** — sprints 13–17.

## Dependências externas

- **Provedor de e-mail transacional** (SMTP/Resend/SES) — **8.5** — pendente de decisão/cadastro (em dev/test usa `ConsoleEmailSender`).
- **Nenhuma credencial será inventada** — provedor de e-mail só é integrado com credencial real quando a 8.5 iniciar.

## DoD (aplicável a cada sub-sprint 8.x)

Implementação → Testes → Validação → Documentação → Commit → `SPRINT_INDEX.md` atualizado
(✅, data, responsável, resumo) → marcar concluída → iniciar a próxima.
