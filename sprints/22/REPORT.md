# Sprint 22 — Follow-up + Scheduler + UAZAPI

> **Status: ✅ Concluída (2026-09-08).**
> Auditoria → implementação dos GAPS → CI/GREEN → CD + deploy VPS com UAZAPI ativado.

## Escopo

Sprint de Omnichannel/Follow-up: auditar o que já existia de **FollowUp** (domain, scheduler,
processador, permissões, testes) e implementar os **gaps reais** encontrados, sem reescrever o
que já estava robusto:

1. **UazapiWhatsAppProvider** — novo adapter para o provedor de WhatsApp da UAZAPI V2 (uazapiGO);
2. **FollowUpSequence** — grupo nomeado e reutilizável de follow-ups (entidade + migration V076 +
   CRUD HTTP completo restrito a ADMIN/MANAGER);
3. **ChannelProvider.UAZAPI** — enum do domínio;
4. **Deploy VPS** com **UAZAPI ativado** (credenciais de teste fornecidas) + CI/CD verde.
5. **RabbitMQ** fica para a próxima etapa (registrado como decisão de escopo).

## Auditoria (passo 1 do fluxo)

| Componente | Situação na auditoria |
|---|---|
| `FollowUp` (domain, status PENDING/PROCESSING/SENT/CANCELLED/FAILED, MAX_ATTEMPTS=3) | ✅ já existia |
| `FollowUpScheduler` (`@Scheduled`, candidates SECURITY DEFINER) | ✅ já existia |
| `FollowUpProcessingService` (claim atômico, HUMAN_MODE, SUPERSEDED_BY_NEW_MESSAGE, retry) | ✅ já existia |
| `FollowUpController` + permissões `omnichannel:followup{,:read}` + RLS FORCE | ✅ já existia |
| Cancelamento por resposta do cliente (staleness determinística) | ✅ já existia |
| Porta `WhatsAppProvider` + `FakeWhatsAppProvider` + `WhatsAppCloudApiProvider` | ✅ já existia |
| Testes: `FollowUpTest`, `FollowUpServiceTest`, `FollowUpProcessingServiceTest`, `FollowUpControllerTest`, `FollowUpIsolationIT` | ✅ já existiam |
| **`UazapiWhatsAppProvider`** | ❌ **GAP** → implementado |
| **`FollowUpSequence`** | ❌ **GAP** → implementado |
| **`ChannelProvider.UAZAPI`** | ❌ **GAP** → implementado |
| **Frontend de follow-up** | ❌ não fazia parte desta etapa → débito |
| **RabbitMQ** | ⏸ próxima etapa |

## Migração

### V076__followup_sequence.sql

- **`followup_sequences`** — `id`, `company_id`, `name varchar(120)` (único por empresa),
  `description`, `status ACTIVE|INACTIVE` (default ACTIVE), `created_at`/`updated_at`;
  RLS **ENABLE + FORCE** + `tenant_isolation_policy` (padrão V044/V073); grants CRUD via loop
  dinâmico (padrão V062).
- **`followups.sequence_id`** — coluna nullable + FK `REFERENCES followup_sequences(id)
  ON DELETE SET NULL` (follow-ups antigos preservados) + índice `idx_followups_sequence`.
- **Permissões** (`omnichannel:followup:sequence` manage / `omnichannel:followup:sequence:read`
  read), grants por empresa existente via DO block com `app.current_company_id` (ADMIN/MANAGER).
  Empresas novas são cobertas no startup pelo `RoleSeedService`.

## Backend

### UazapiWhatsAppProvider

`infrastructure/omnichannel/whatsapp/UazapiWhatsAppProvider` implementa `WhatsAppProvider`
(porta existente, sem tocar no domínio/controllers):

- **Ativo somente** quando `omnichannel.whatsapp.provider=uazapi`
  (`@ConditionalOnProperty`) — os três providers são mutuamente exclusivos por valor da mesma
  propriedade (`fake`|`cloud-api`|`uazapi`).
- **Contrato UAZAPI V2 (uazapiGO)** confirmado (sem inventar API):
  `POST {base}/send/text`, **header customizado `token: <token>`** (não Bearer),
  body `{"number":"<to>","text":"<body>"}`.
- **Base URL**: `omnichannel.whatsapp.uazapi.base-url` (`${UAZAPI_BASE_URL:}`) — ex. `https://<subdomain>.uazapi.com`.
- **Token**: `secretsRef` do canal → env de mesmo nome → fallback global `UAZAPI_TOKEN`
  (nunca logado/persistido).
- **External id**: `data.key.id` / `data.id` / top-level `id`; gera `UAZAPI_<uuid>` quando
  `success=true` sem id (idempotência local); lança `OmnichannelProviderException` em 4xx/5xx
  sem id e em `success=false`.

`application.yml` ganhou o bloco `omnichannel.whatsapp.uazapi.{base-url,token}`.

### FollowUpSequence (CRUD completo)

- **Domínio** (`domain/followup`): `FollowUpSequence` (POJO, factories `create`/`reconstitute`,
  `update`, `activate`, `deactivate`, `isActive`), `FollowUpSequenceStatus`, exceções
  `FollowUpSequenceNotFound/Validation`.
- **Aplicação** (`application/followup`): portas `FollowUpSequenceUseCase`/`FollowUpSequenceRepository`,
  DTOs `FollowUpSequenceRequest`/`Response`, `FollowUpSequenceService` (TenantContext por empresa +
  auditoria `AuditModule.FOLLOWUPS`).
- **Infra** (`infrastructure/followup/persistence`): `FollowUpSequenceJpaEntity`,
  `FollowUpSequenceJpaRepository` (paginado por company, delete filtrado), `FollowUpSequenceRepositoryImpl`.
- **REST** `presentation/rest/followup/FollowUpSequenceController` em
  `/api/v1/omnichannel/follow-up-sequences`: POST/GET (list)/GET/{id}/PUT/DELETE/
  POST/{id}/activate|deactivate, `@PreAuthorize` com `omnichannel:followup:sequence{,:read}`.
- **`FollowUp`** ganhou `sequenceId` nullable (domínio, JPA entity, mapper, `FollowUpResponse`,
  `FollowUpRequest`) — associção opcional do seguimento ao follow-up.

## Testes

- **Backend**: **724 testes / 0 falhas** (`mvnw test`, suíte completa offline).
  Novos: `UazapiWhatsAppProviderTest` (7: MockRestServiceServer — sucesso data.key.id/data.id/
  success-only, 4xx/5xx, token via secretsRef, fallback global, token ausente, base URL ausente,
  success=false), `FollowUpSequenceTest`, `FollowUpSequenceServiceTest`, `FollowUpSequenceControllerTest`
  (CRUD + activate/deactivate + 404/ownership). Ajustados: `FollowUp*` (novo arg `sequenceId`).
- **Frontend**: sem alterações de código (nenhum arquivo frontend tocado); `npm run lint` e
  `npm run typecheck` verdes (warnings preexistentes apenas).

## CI/CD + Deploy

- Commit `1a53a53` (features) + `9f49c3c` (compose com `UAZAPI_BASE_URL`/`UAZAPI_TOKEN`).
- **CI verde**: Backend CI (unit + ITs Testcontainers), Auth-service CI, Frontend CI (lint/
  typecheck/format/build) e Docker Build — **todos success**.
- **CD**: imagens backend/auth-service/frontend build+push `ghcr.io/ghilhermealves/crm/*:latest` OK;
  o job **Deploy to Staging falhou por rede** (`ssh: Network is unreachable` — transitório do runner
  GitHub). Deploy concluído **manualmente via `crm-vps`** com o mesmo procedimento do fluxo CD.
- **VPS (produção)**:
  - `docker compose pull` + `up -d`: backend/auth-service/frontend recriados;
  - **V076 aplicada** (Flyway `v076`) — "granted 8 followup-sequence permissions";
  - `WHATSAPP_PROVIDER=uazapi` + `UAZAPI_BASE_URL=https://free.uazapi.com` +
    `UAZAPI_TOKEN=<credencial de teste>` no `.env` (com backup `.env.bak-20260908-uazapi`);
  - `.env` do container confirmado (`WHATSAPP_PROVIDER=uazapi`, `UAZAPI_BASE_URL`, `UAZAPI_TOKEN`);
  - Smoke: `/actuator/health` 200, `/auth/health` 200, frontend `/` e `/login` 200,
    `/api/v1/omnichannel/follow-up-sequences` 401 sem sessão (rota registrada e protegida).

## Débitos conhecidos

- **Frontend de follow-up / follow-up-sequences** — sem UI nesta Sprint (backend + permissions
  prontas; rota 401 sem sessão comprova proteção).
- **E2E autenticado manual** (browser) — herdado; requer credenciais de teste.
- **RabbitMQ** — etapa seguinte (produção de mensagens para fila, fila→webhook UAZAPI).
- **Envio real via UAZAPI** ainda não exercitado ponta-a-ponta com um canal conectado na VPS
  (credenciais de teste configuradas; exige instância UAZAPI com número ativo para E2E).

## Arquivos Principais

- Migração: `backend/src/main/resources/db/migration/V076__followup_sequence.sql`
- Provider: `backend/src/main/java/com/becommerce/crm/infrastructure/omnichannel/whatsapp/UazapiWhatsAppProvider.java`
- FollowUpSequence: `domain/followup/{FollowUpSequence,FollowUpSequenceStatus}`,
  `application/followup/{dto,port/input/FollowUpSequenceUseCase,port/output/FollowUpSequenceRepository,service/FollowUpSequenceService}`,
  `infrastructure/followup/persistence/FollowUpSequence*`,
  `presentation/rest/followup/FollowUpSequenceController`
- FollowUp (sequenceId): `domain/followup/FollowUp`, `application/followup/{dto,service/FollowUpService}`,
  `infrastructure/followup/persistence/FollowUp{JpaEntity,RepositoryImpl}`
- Config: `backend/src/main/resources/application.yml` (+ `docker/docker-compose.yml` env UAZAPI)
- Testes: `UazapiWhatsAppProviderTest`, `FollowUpSequenceTest`, `FollowUpSequenceServiceTest`,
  `FollowUpSequenceControllerTest` (+ ajustes em `FollowUp*Test`)