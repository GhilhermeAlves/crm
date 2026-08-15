# Sprint 8.6 — SaaS Hardening

**Data:** 2026-08-12 · **Status:** ✅ Concluída · **Responsável:** AI Agent · **Dependência:** 8.4, 8.5

## Resumo

Sprint de **endurecimento multi-tenant**: enforcement de limites por plano
(`max_users`, `max_contacts`, `max_storage_mb`), exposição de quotas/uso, auditoria de
tenant, revisão de segurança RLS/gateway e documentação final da arquitetura RLS real.
Esta é a **última sprint da fase SaaS (8.x)**.

## Enforcement de limites por plano

- **`QuotaExceededException`** + handler no `GlobalExceptionHandler` → HTTP **422**
  (`code = QUOTA_EXCEEDED`), com mensagem clara e upgrade sugerido.
- **`CompanyQuotaService`** (ports & adapters): `assertCanAddContact`,
  `assertCanAddSpace`, `usage`, contadores (`countActiveUsers`, `countPendingInvitations`,
  `countContacts`, `storageBytes`), `requireCompany`; seta/restaura o tenant
  (`app.current_company_id`) no escopo.
- **`max_users`**: `InvitationService.create` conta `activeMembers + convites PENDING` e
  bloqueia criação; `accept` bloqueia quando `active >= maxUsers`; também bloqueia aceite
  quando o e-mail **já é membro** (`inviteeAlreadyMember`).
- **`max_contacts`**: módulo mínimo `Contact` reutilizando a tabela `contacts` (V015);
  `ContactService` chama `assertCanAddContact` na criação.
- **`max_storage_mb`**: módulo mínimo `Storage` em nova tabela `storage_objects` (V037,
  blob `bytea`); `StorageService` chama `assertCanAddSpace` (bytes = `bigint`, conversão
  `1024L*1024L` por MB).
- Defaults (`CompanyService`): `maxUsers=5`, `maxContacts=500`, `maxStorageMb=1024`.

## Quotas e uso expostos

- `GET /api/v1/companies/{id}/usage` em `CompanyController`/`CompanyUseCase`/
  `CompanyService` (via `CompanyQuotaService`) → `CompanyUsageResponse`
  (`users`/`contacts`/`storage`) + limites do plano (`CompanyResponse`).
- **Frontend**: feature `usage` (`types`, `service`, hook `useCompanyUsage` + teste);
  mensagens de quota com upgrade sugerido.

## Auditoria de tenant

- `AuditModule` ganhou `MEMBERSHIPS` e `INVITATIONS` (além de `TENANTS`).
- **`TenantAuditRecorder`**: lê o `AuditContext` (ator/IP/User-Agent), com fallback de
  `actorUserId`; seta/restaura o tenant no escopo para gravar `audit_logs`.
- Eventos auditados: convite **criado/aceito/revogado**, membership **removida**
  (DELETE), empresa ativa **trocada** (switch, `TENANTS UPDATE`).

## Revisão de segurança RLS/gateway

- Revisadas todas as policies das novas tabelas: `memberships` (V030), `invitations`
  (V036, 4 policies + GUC de token), `storage_objects` (V037).
- **Correção real encontrada**: contextos de tenant precisavam ser setados nos novos
  serviços `Contact`/`Storage` para permitir operação de SUPER_ADMIN cross-tenant.
- Nenhuma mudança em CSRF/rate-limit além do existente (invitations já têm
  `InvitationRateLimiter`).

## Documentação (arquitetura RLS real)

- **`docs/MULTI_TENANCY.md`**: reescrita 1.0→2.0 — shared schema + RLS FORCE (GUC
  `app.current_company_id`), `app.current_tenant_id()`, memberships (own+tenant policies),
  switcher, invitations por token, quotas/uso e auditoria de tenant; design antigo
  schema-per-tenant marcado como superado.
- **`docs/DATABASE_MAP.md`**: reescrita 1.0→2.0 — entidades reais (companies, users,
  memberships, invitations, contacts, storage_objects, audit_logs), tabelas tenant-scoped
  vs globais, quotas por plano.
- **`docs/BACKEND_MAP.md`**: 1.1.0 — nova seção "Implementação Vigente (Sprint 8)"
  (CompanyQuotaService, Contact, Storage, Invitation, TenantAuditRecorder, TenantContext/
  TenantAwareDataSource).

## Migração

- **`V037__company_quota_storage.sql`**: `storage_objects` (RLS FORCE + policy
  `tenant_isolation_policy` + grants `crm_app` + verificação estrutural via `DO`), padrão
  V031/V034/V036.

## Testes

- **Backend: 210 testes PASS** (antes 185; novos para quota/invite/contact/storage/usage:
  `InvitationServiceTest`, `CompanyQuotaServiceTest`, `ContactServiceTest`,
  `StorageServiceTest`, `ContactControllerTest`, `CompanyControllerTest` usage/403,
  `GlobalExceptionHandlerTest`), typecheck OK, lint OK (warnings pré-existentes).
- **Frontend: 68 testes PASS** (antes 66), typecheck OK, lint sem erros novos.

## Pendências / débitos técnicos

- **E2E autenticado manual** (herdado da 8.5): fluxo real de invitations/quota no browser —
  sem credenciais de teste automatizáveis.
- **Envio de e-mail real** (herdado): `ConsoleEmailSender` é placeholder; trocar por
  provider quando credenciais disponíveis.
- **`InvitationRateLimiter` em memória** (herdado): Redis/DB em multi-instância.
- **Storage**: `storage_objects` guarda blob no banco (infraestrutura mínima para aplicar/
  testar a quota); o port permite trocar por object-store externo (MinIO) sem mexer nos
  casos de uso.
- **Auditoria geral** de eventos não-tenant além do escopo desta sprint — não realizada
  (fora de escopo).

## Artefatos

- `backend/src/main/resources/db/migration/V037__company_quota_storage.sql`
- `backend/.../application/company/{service/{CompanyQuotaService,CompanyService},port/input/CompanyUseCase,dto/*}.java`
- `backend/.../presentation/rest/company/CompanyController.java`
- `backend/.../application/audit/service/TenantAuditRecorder.java`, `domain/audit/AuditModule.java`
- `backend/.../application/invitation/service/InvitationService.java`
- `backend/.../application/membership/service/MembershipService.java`, `application/me/service/MeService.java`
- `backend/.../application/contact/**`, `backend/.../application/storage/**` (módulos mínimos)
- `backend/.../presentation/rest/handler/GlobalExceptionHandler.java`
- `frontend/src/features/usage/**`
- `docs/MULTI_TENANCY.md`, `docs/DATABASE_MAP.md`, `docs/BACKEND_MAP.md`

---

## Addendum (2026-08-15) — Storage: CRUD completo + tela de Arquivos

Extensão do módulo mínimo `Storage` (8.6), que originalmente só gravava via upload
para aplicar a quota. Esta entrega completa os casos de uso e adiciona a UI.

### Backend
- `StorageUseCase`/`StorageService`/`StorageRepository`(+impl/`StorageJpaRepository`):
  novos casos `list`, `download` e `delete` — todos escopados por tenant
  (`TenantContext` + RLS FORCE) e auditados via `TenantAuditRecorder`.
- `StorageDownload` (DTO: id, fileName, contentType, sizeBytes, data).
- `StorageObjectNotFoundException` (`domain/storage/exception/`) → HTTP **404** no
  `GlobalExceptionHandler` quando o objeto não existe ou não pertence à empresa.
- Endpoints em `StorageController`:
  `GET /api/v1/companies/{companyId}/storage` (listar),
  `GET /{objectId}` (download com `Content-Type` + `Content-Disposition: attachment`),
  `DELETE /{objectId}` (204); acesso restrito à própria empresa (SUPER_ADMIN cross-tenant).

### Frontend
- Nova página `/storage` ("Arquivos") em `frontend/src/app/(dashboard)/storage/`:
  upload, listagem em tabela (nome/tipo/tamanho), download e exclusão com `ConfirmDialog`.
- Feature `features/storage/`: `types`, `service`, hook `useStorage`
  (`useStorageObjects`/`useUploadFile`/`useDownloadFile`/`useDeleteFile`) + `formatBytes`.
- Sidebar (grupo Administração) e `ROUTES.STORAGE` atualizados.

### Qualidade
- Backend: **366 testes** PASS (BUILD SUCCESS) — `StorageServiceTest` cobrindo
  list/download/delete e 404 cross-tenant.
- Frontend: **133 testes** (26 arquivos) PASS; typecheck/lint OK; build prod OK
  (rota `/storage` gerada).

### Débito técnico resolvido
- `KNOWN_ISSUES L-005` ("Sem file storage implementado") → **resolvida**: storage
  com upload/download/list/delete agora disponível (blob em `storage_objects`, port
  permite trocar por object-store externo).

### Deploy / validação (VPS)
- Código sincronizado em `/opt/crm` e imagens `backend`/`frontend` reconstruídas
  (`docker compose build/up -d`) na VPS `crm-vps` (76.13.237.238).
- Backend sobe limpo (sem nova migração — mudança apenas de código), `/actuator/health` → **200**.
- Smoke test: `GET .../storage` → **401** (rota registrada e protegida por
  `@PreAuthorize` + `requireCompanyAccess`); frontend `/storage` → **307** para
  `/login?redirect=/storage` (rota existente e guardada pelo middleware de auth).

### Artefatos (addendum)
- `backend/.../application/storage/**`, `domain/storage/exception/**`,
  `presentation/rest/storage/StorageController.java`, `GlobalExceptionHandler.java`
- `backend/.../test/.../storage/service/StorageServiceTest.java`
- `frontend/src/app/(dashboard)/storage/page.tsx`, `frontend/src/features/storage/**`
- `frontend/src/components/layout/Sidebar.tsx`, `frontend/src/lib/constants.ts`