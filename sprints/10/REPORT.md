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

- **Backend: 228 testes PASS** (antes 215). Novos:
  - `LeadServiceTest` (4): cria lead com contato próprio+único; rejeita contato de
    outra empresa; rejeita lead duplicado; lead não encontrado.
  - `LeadControllerTest` (9): list/403 cross-company; create 201/400 (sem origem)/403;
    update/403; delete/403 — cobrindo autorização por permission e isolamento.
- **Frontend: 82 testes PASS** (antes 74). Novos:
  - `lead.schema.test.ts` (5): validação de UUID, origem, score 0–100, classificação.
  - `useLeads.test.ts` (3): busca por empresa ativa, não busca sem empresa, create
    mutation posta na empresa ativa.
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

- **NÃO validado em produção nesta etapa.** A implementação está pronta e testada
  localmente; o deploy real na VPS (imagem GHCR, rebuild backend+frontend, Flyway
  inalterado pois não há nova migration) e a validação dos endpoints
  `/api/v1/companies/{companyId}/leads*` na VPS ainda **pendentes** — push para `main`
  não implica deploy real (pipeline atual publica imagens no GHCR sem deploy). Ver
  débitos.

## Débitos

- **Deploy + validação VPS pendente** (Sprint 10 não 100% fechada no ambiente de
  produção): rebuild backend/frontend na VPS e smoke test dos endpoints de leads.
- **E2E autenticado manual** (herdado): fluxo real de leads no browser sem credenciais.
- **Envio de e-mail real** (herdado): `ConsoleEmailSender` placeholder.
- **Scoring/distribuição/conversão** (Lead.md L-020/030/040) — regras avançadas de
  qualificação e scoring ficam para Sprints 11 (Pipeline) e 17 (IA); esta sprint
  entregou CRUD + status/classificação/score validados.

## Artefatos

- Backend: `domain/lead/*`, `application/lead/{dto,port,service}/*`,
  `presentation/rest/lead/LeadController.java`,
  `infrastructure/lead/persistence/*`, `GlobalExceptionHandler.java`.
- Testes backend: `LeadServiceTest.java`, `LeadControllerTest.java`.
- Frontend: `features/leads/{types,schemas,services,hooks,components}/*`,
  `app/(dashboard)/leads/{page,new,{[id]},{[id]/edit}}`,
  `components/layout/Sidebar.tsx`, `lib/constants.ts`.
