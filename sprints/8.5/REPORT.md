# Sprint 8.5 - Invitations

**Data:** 2026-08-11 · **Status:** ✅ Concluída · **Responsável:** AI Agent · **Dependência:** 8.2, 8.3

## Resumo

Sprint **Invitations** implementada em duas partes — **(1) Governança** (versionamento e
padronização de migrações/grants) e **(2) Convites** (fluxo de convite por e-mail com token
secreto, isolado por RLS multi-tenant) — e deployada em produção (backend e frontend).

---

## Parte 1 — Governança

- **Versionamento de migrações reconciliado**: `V032–V035` agora versionados no Git; `V034`
  virou DML (seed de papéis) e `V035` aplica o **lockdown de grants** (padronização).
- **Grants padronizados**: `companies` recebe DML completo para `crm_app`; `permissions` e
  `flyway_schema_history` ficam **SELECT-only** (uso restrito a scripts internos).
- **`users.company_id` nullable**: permite usuário company-less (seed de onboarding).
- **Seed de papéis no `createCompany`**: transacional, via `RoleSeedService` + membership
  `OWNER/ACTIVE` + papel `ADMIN` + grant de acesso CRM.
- **`SPRING_FLYWAY_OUT_OF_ORDER=true`** adicionado ao compose (permite aplicar migrações que
  ficaram de fora do histórico da VPS sem quebrar o Flyway).
- **Bug `V032` corrigido**: ambigüidade de `is_nullable` e checagem de nullable invertida.

## Parte 2 — Backend (Invitations)

- **`V036__invitations.sql`**: tabela `invitations` + **4 policies RLS** (admin_select,
  admin_write, token_select, token_update) + funções
  `app.set_invitation_token_context` / `app.current_invitation_token_hash` (GUC
  `app.invitation_token_hash`) + grants `crm_app` (DELETE/INSERT/SELECT/UPDATE).
- **Token**: 32B base64url (somente exposto no e-mail); no banco armazena-se apenas o
  **SHA-256 hex** (`invitations.token_hash`, `varchar(64)`), com índice único.
- **Regras de negócio**: única `PENDING` por `(company_id, email)` (índice parcial único);
  status `PENDING/ACCEPTED/REVOKED/EXPIRED`; **decline → REVOKED**; roles elegíveis
  `ADMIN/MANAGER/AGENT/VIEWER`; `SUPER_ADMIN`/`OWNER` bloqueados (OWNER só via onboarding).
- **API**: `GET/POST /companies/{id}/invitations` (+ query `status`) e
  `DELETE /companies/{id}/invitations/{invitationId}` com `membership:view`/`membership:manage`.
- **Acesso por token**: `InvitationTokenContextHolder` + policies token permitem leitura/aceite
  sem depender do contexto de empresa do chamador (aceite pré/provisório).
- **Casos de uso**: `InvitationService` (criar/aceitar/revogar/expirar) e `InvitationTokenService`
  (geração/hash).
- **ConsoleEmailSender**: envio de e-mail por console (perfil-gate removido p/ não quebrar o
  startup em produção), já preparado para troca por provider real.

## Frontend (2.17)

- Página `/invitations`: lista de convites com badges de status, papel e expiração.
- `CreateInvitationDialog`: e-mail + papel (`ADMIN/MANAGER/AGENT/VIEWER`) com react-hook-form+zod.
- `useInvitations` / `useCreateInvitation` / `useRevokeInvitation` (React Query + toast).
- `InvitationService`: `GET/POST/DELETE` via cliente axios `@/lib/api`.
- Sidebar: entrada **Convites** (`membership:view`) + `ROUTES.INVITATIONS`.
- `typecheck` OK, `lint` sem erros novos (warnings da Sidebar são pré-existentes).

## RLS provado em produção

Smoke via `psql`/`docker exec`:
- Sem contexto (admin sem GUC): **0 linhas** na tabela `invitations`.
- Com GUC `app.invitation_token_hash` do token: **1 linha** visível.
> O mecanismo real via `InvitationTokenContextHolder`/`JdbcTenantLinkHolder` é o mesmo usado por
> `memberships`; o teste inicial só falhou por chamar `set_tenant_context(text)` com argumento
> inválido, sem relação com o fluxo produtivo.

## Testes

- **Backend: 179 testes verdes** (inclui `InvitationServiceTest` 10 e `InvitationControllerTest` 5).
- **Frontend**: typecheck OK; lint sem erros novos.
- Legacy (audit 2.18): fluxo antigo `POST /users/invite` continua como está (grava token na
  row do usuário, role `AGENT` hardcoded, e-mail nunca enviado — TODO) — **sem mudança breaking**,
  apenas documentado.

## Deploy (produção / VPS `crm-vps`)

- Push `d46a54e..4a55639` / etapas de Parte 1 em `main`.
- Flyway: rephas aplicado com `SPRING_FLYWAY_OUT_OF_ORDER=true`; histórico **`031..036`** ok.
- Backend e frontend rebuild :redeploy via `docker compose` (containers `crm-backend`,
  `crm-frontend`). `/invitations` respondendo (307 → login quando não autenticado).

## Reconciliação (segunda passada) — `cbee5ce`

Fechamento dos gaps do escopo sobre a base já existente:

- **Frontend de aceite/recusa**: `InvitationService.accept/decline`; página `/invitations/accept`
  (rota `(invitation)` com gate **somente de autenticação** — aceita usuário com/sem empresa,
  essencial p/ ingressar numa segunda empresa). Não-autenticado → login preservando a URL+token.
- **Rate limit**: `InvitationRateLimiter` (janela móvel em memória, sem dependência) aplicado em
  `create` (por empresa, 20/h) e `accept` (por usuário, 10/h). @Mock adicionado ao
  `InvitationServiceTest`. Em multi-instância deve virar Redis/DB (débito anotado).
- **Link absoluto no e-mail**: `app.invitations.base-url` (`INVITATION_BASE_URL`) — quando
  configurado, o e-mail leva a URL absoluta `/invitations/accept?token=`; vazio mantém relativo
  (sem inventar domínio).
- **Validação no VPS**: pages `/invitations/accept` viva (307→login); endpoints backend
  `/api/v1/invitations/accept|decline` registrados (401 sem sessão, via nginx).
- **Status dos testes**: backend **179 verdes** (invitation 15); frontend typecheck + lint sem erros.
- Pendência: E2E de aceite **autenticado** (sessão + token real) fica para validação manual no browser.

## Pendências / débitos conhecidos

- Envio de e-mail real: `ConsoleEmailSender` é placeholder; trocar por provider (SMTP/Resend/SES)
  quando disponível — abstração `EmailSender` pronta.
- E2E de aceite autenticado (fluxo completo convite → aceite → membership): validação manual no browser.
- `InvitationRateLimiter` em memória: substituir por implementação distribuída se multi-instância.
- Legado `users.invite_token`/`/users/invite` (Sprint antiga): sem mudança breaking; mapeado como
  débito para depreciação isolada.

## Artefatos

- `backend/src/main/resources/db/migration/V036__invitations.sql`
- `backend/.../domain/invitation/{Invitation,InvitationStatus}.java`
- `backend/.../application/invitation/{service,port,dto}/...`
- `backend/.../infrastructure/invitation/persistence/{InvitationTokenContextHolder,JpaInvitation,InvitationJpaRepository,InvitationRepositoryImpl}.java`
- `backend/.../presentation/rest/invitation/InvitationController.java`
- `backend/.../presentation/rest/handler/GlobalExceptionHandler.java`
- `frontend/src/features/invitations/**`
- `frontend/src/app/(dashboard)/invitations/page.tsx`
- `frontend/src/components/layout/Sidebar.tsx`