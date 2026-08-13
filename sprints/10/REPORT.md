# Sprint 10 — Leads

**Data:** 2026-08-13 · **Status:** ✅ Concluída · **Responsável:** AI Agent · **Dependência:** 9

## Resumo

Sprint de **gestão de leads por empresa** dentro do modelo SaaS/RLS já estabelecido.
A tabela `leads` e suas policies RLS (V016/V021) e as permissões `lead:create/read/
update/delete` (V007 + `RoleSeedService`) já existiam das sprints 8.x — esta sprint
construiu a **camada de aplicação (backend)** e a **feature de UI (frontend)** sobre
essa base, sem duplicar estruturas de banco, RBAC, `CurrentUser`, RLS ou Company
Switcher. Reutilizou integralmente o padrão do módulo `contact` (Sprint 8.6/9) e o
padrão de listagem/paginação do módulo `users`.

## Banco de dados

> **Nenhuma nova migration foi necessária.**

- Tabela `leads` (V016), índices únicos `(contact_id, company_id)` e CHECK de
  `status`/`source`/`score` já existentes.
- RLS FORCE + `tenant_isolation_policy` para `leads` (V021) já existentes —
  isolamento multi-tenant garantido pelo banco.
- Permissões `lead:*` já registradas (V007) e associadas aos roles (ADMIN/MANAGER
  completas, AGENT create/read/update, VIEWER read) via `RoleSeedService`.

## Backend

Novo módulo `com.becommerce.crm.<lead>` espelhando `contact`:

- **Domínio**:
  - `domain/lead/Lead.java` — POJO com factories `create`/`reconstitute`, `touch()`,
    transição de estado `transitionTo` (L-010/L-012: LOST→NEW reabre; CONVERTED não
    pode ser alterado — L-023), `updateScore` (0–100) e `updateClassification`.
  - `domain/lead/LeadStatus.java` (NEW/CONTACTED/QUALIFIED/UNQUALIFIED/CONVERTED/LOST),
    `LeadSource.java` (WHATSAPP/FORM/API/IMPORT/MANUAL), `LeadClassification.java`
    (HOT/WARM/COLD/DISQUALIFIED).
  - `exception/LeadNotFoundException.java`, `exception/DuplicateLeadException.java`
    (violação da unicidade por contato/empresa → 409).
- **Aplicação**:
  - `application/lead/port/input/LeadUseCase.java` — `create/getById/update/delete/list`;
  - `application/lead/port/output/LeadRepository.java` — `save/findById/delete/
    existsByContactIdAndCompanyId/findByCompanyWithFilters` (porta `PageResult`);
  - `application/lead/dto/CreateLeadRequest.java`, `UpdateLeadRequest.java`,
    `LeadResponse.java` (validações Jakarta: contato e origem obrigatórios, score 0–100);
  - `application/lead/service/LeadService.java` — isola cada operação pelo
    `TenantContext` (finally `clear()`), valida que o **contato associado pertence e
    está ativo na mesma empresa** (`ContactRepository` — defense-in-depth além do RLS),
    garante unicidade por `(contact_id, company_id)`, e audita
    (`AuditModule.LEADS` + `AuditAction.CREATE/UPDATE/DELETE` via `TenantAuditRecorder`).
- **Apresentação**:
  - `presentation/rest/lead/LeadController.java` — `/api/v1/companies/{companyId}/leads`
    com `@PreAuthorize('lead:read/create/update/delete')` + `requireCompanyAccess`
    (SUPER_ADMIN cross-tenant preservado; demais restritos à empresa ativa).
- **Infraestrutura**:
  - `infrastructure/lead/persistence/LeadJpaEntity.java`, `LeadJpaRepository.java`
    (query derivada com filtros status/source/classification + `Pageable`),
    `LeadRepositoryImpl.java`.
- **Handlers**: `GlobalExceptionHandler` recebe handlers para `LeadNotFoundException`
  (404) e `DuplicateLeadException` (409).

## Frontend

Nova feature `features/leads` + páginas sob `app/(dashboard)/leads`:

- **Tipos** `types/lead.types.ts` — `Lead`, enums (`LeadStatus/Source/Classification`),
  DTOs e `PageResponse`/`ListLeadsParams`.
- **Schema** `schemas/lead.schema.ts` — validação Zod (UUID do contato, origem/status
  restritos, score 0–100, classificação opcional) + rótulos pt-BR.
- **Serviço** `services/lead.service.ts` — `list/findById/create/update/delete`
  (scoped por companyId, base `/companies/{companyId}/leads`).
- **Hooks** `hooks/useLeads.ts` — React Query (`useLeads/useLead/useCreateLead/
  useUpdateLead/useDeleteLead`), invalidação por `["leads", companyId]` (re-deriva a
  cada Company Switcher).
- **Componentes** `components/` — `LeadBadges` (status/source/classificação),
  `LeadTable`, `LeadFilters` (status/origem/classificação), `LeadForm`,
  `DeleteLeadDialog`.
- **Páginas** — `/leads` (lista com filtros + paginação), `/leads/new`,
  `/leads/[id]` (detalhe), `/leads/[id]/edit`.
- **Permissões** — `Sidebar` gating do item Leads por `lead:read`; botões Novo/Excluir
  gated por `lead:create`/`lead:delete` (`useAuthorization`). O backend permanece a
  autoridade final (403).

## Testes

> **Validado em 2026-08-13.** Contagens refletem execução real, incluindo os testes
> adicionados posteriormente (IT RLS + componentes de UI).

- **Backend: 242 testes PASS** (antes 215). Novos:
  - `LeadServiceTest` (4): cria lead com contato próprio+único; rejeita contato de
    outra empresa; rejeita lead duplicado; lead não encontrado.
  - `LeadControllerTest` (9): list/403 cross-company; create 201/400 (sem origem)/403;
    update/403; delete/403 — cobrindo autorização por permission e isolamento.
  - `LeadIsolationIT` (8, Testcontainers PostgreSQL 17 + RLS, **executado 8/8 PASS**):
    isolamento cross-tenant real na tabela `leads` — cada tenant vê apenas os seus leads,
    cross-tenant SELECT/UPDATE/DELETE afetam 0, INSERT cross-tenant bloqueado por RLS, e
    insert+read na mesma empresa funciona.
  - `InvitationRateLimiterTest` (6, **6/6 PASS**): após migração do rate limiter de
    convites para Redis — permitido dentro do limite (create 20/h, accept 10/h), bloqueio
    ao exceder, contadores independentes por chave e fail-open quando o Redis está
    indisponível (timeout/conexão).
- **Frontend: 96 testes PASS** (antes 74, **suíte completa executada 96/96**). Novos:
  - `lead.schema.test.ts` (5): validação de UUID, origem, score 0–100, classificação.
  - `useLeads.test.ts` (3): busca por empresa ativa, não busca sem empresa, create
    mutation posta na empresa ativa.
  - `LeadBadges.test.tsx` (5): rótulos pt-BR de status/classificação, fallback e dash.
  - `LeadTable.test.tsx` (4): skeleton de loading, empty state, renderização de linhas
    (status/origem/score) e disparo do `onDelete` (abre o dropdown por `pointerdown`).
  - `DeleteLeadDialog.test.tsx` (5): sem lead → nada; título/descrição; confirmar;
    cancelar; estado `isLoading` (botão travado).
- Typecheck OK; lint sem erros novos (warnings pré-existentes mantidos); build
  production OK (rotas `/leads*` geradas).

## Build e integração

- Backend: `mvnw test` BUILD SUCCESS (228 testes), compile OK.
- Frontend: `tsc --noEmit` OK, `next lint` OK, `next build` OK.
- Integração: endpoints `/api/v1/companies/{companyId}/leads*` com RLS + `@PreAuthorize`
  + `requireCompanyAccess` + `TenantContext`; UI refletindo `lead:*` permissions.

## E2E

- **E2E autenticado manual não realizado** (limitação herdada das sprints 8.x/9): fluxo
  real de criação/edição de lead no browser exige credenciais de teste não
  automatizáveis. Testes unitários de controller/serviço cobrem os fluxos de
  autorização e validação; não inventado resultado de E2E.

## Produção / VPS

- **Validado em produção nesta etapa.** Deploy real executado na VPS `crm-vps`
  (`docker compose build` + `up -d`: rebuild backend+frontend, sem nova migration —
  Flyway inalterado). Smoke tests: `/actuator/health` 200, `/api/v1/leads` e
  `/api/v1/companies/{companyId}/leads` 401 sem sessão (endpoints registrados),
  `/leads` 307 → `/login?redirect=%2Fleads`, 0 ERROR nos logs do backend.

## Reconciliação de débito — rate limiter de convites → Redis (2026-08-13)

Fechamento do débito anotado nas Sprints 8.5/8.6/9 ("`InvitationRateLimiter` em memória →
Redis/DB em multi-instância"):

- **`InvitationRateLimiter` migrado de janela deslizante em memória (`ConcurrentHashMap`)**
  para **janela fixa distribuída em Redis** (`StringRedisTemplate`), espelhando o padrão do
  `GatewayRateLimiter` do auth-service (Lua `INCR` + `EXPIRE` no primeiro incremento ⇒
  atômico e compartilhado entre instâncias).
- **Contrato preservado**: `tryCreate(companyId)` / `tryAccept(key)` booleanos — o
  `InvitationService` e o `InvitationServiceTest` (mock) não mudaram. Limites mantidos
  (create 20/h por empresa, accept 10/h por usuário; janela 60 min).
- **`prune()` removido**: chaves distribuem-se com TTL nativo do Redis (não há mais
  limpeza manual em memória).
- **Política de falha (fail-open controlado)**: Redis indisponível ⇒ requisição permitida
  com warning (igual ao gateway) — o limiter nunca derruba o backend.
- **Infra**: o backend já recebia `CRM_REDIS_HOST/PORT/PASSWORD` no compose e tem
  `spring-boot-starter-data-redis` + health indicator de Redis — só a implementação de
  in-memory → Redis mudou (nenhuma configuração nova de deploy).
- **Teste**: `InvitationRateLimiterTest` (6) validando limites, independência por chave e
  fail-open; suíte `InvitationServiceTest` (19) permanece 19/19.

## Débitos

- **E2E autenticado manual** (herdado): fluxo real de leads no browser sem credenciais.
- **Envio de e-mail real** (herdado): `ConsoleEmailSender` placeholder.
- **Scoring/distribuição/conversão** (Lead.md L-020/030/040) — regras avançadas de
  qualificação e scoring ficam para Sprints 11 (Pipeline) e 17 (IA); esta sprint
  entregou CRUD + status/classificação/score validados.

## Artefatos

- Backend: `domain/lead/*`, `application/lead/{dto,port,service}/*`,
  `presentation/rest/lead/LeadController.java`,
  `infrastructure/lead/persistence/*`, `GlobalExceptionHandler.java`.
- Testes backend: `LeadServiceTest.java`, `LeadControllerTest.java`,
  `LeadIsolationIT.java`, `InvitationRateLimiterTest.java`.
- Frontend: `features/leads/{types,schemas,services,hooks,components}/*`,
  `app/(dashboard)/leads/{page,new,{[id]},{[id]/edit}}`,
  `components/layout/Sidebar.tsx`, `lib/constants.ts`.
