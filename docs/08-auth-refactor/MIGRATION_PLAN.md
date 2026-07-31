# MIGRATION_PLAN — Plano de Migração por Sprints

## Objetivo

Definir o plano completo de migração da arquitetura atual (coexistência Keycloak + JWT próprio do backend) para a arquitetura alvo, na qual o **Keycloak é o IdP e Authorization Server exclusivo** (único emissor de JWT) e o **`crm-auth-service` atua como camada de identidade da aplicação** (provisionamento, sincronização, RBAC e `CurrentUser`) — sem introduzir um novo emissor de tokens. Inclui estratégia de rollback, critérios de aceite por sprint, análise de riscos e a regra permanente de encerramento de sprint.

## Índice

- [1. Princípios da Migração](#1-princípios-da-migração)
- [2. Regra Permanente de Encerramento de Sprint](#2-regra-permanente-de-encerramento-de-sprint)
- [3. Visão Geral das Sprints](#3-visão-geral-das-sprints)
- [4. Sprint 1 — Provisionamento Automático e Eliminação do 500](#4-sprint-1--provisionamento-automático-e-eliminação-do-500)
- [5. Sprint 2 — Estrutura do crm-auth-service](#5-sprint-2--estrutura-do-crm-auth-service)
- [6. Sprint 3 — Migração do Frontend](#6-sprint-3--migração-do-frontend)
- [7. Sprint 4 — Microsserviços consumindo o Auth Service](#7-sprint-4--microsserviços-consumindo-o-auth-service)
- [8. Sprint 5 — Eventos, Auditoria e Observabilidade](#8-sprint-5--eventos-auditoria-e-observabilidade)
- [9. Sprint 6 — Hardening, Limpeza e Dependências Desnecessárias](#9-sprint-6--hardening-limpeza-e-dependências-desnecessárias)
- [10. Estratégia de Rollback](#10-estratégia-de-rollback)
- [11. Critérios de Aceite Consolidados](#11-critérios-de-aceite-consolidados)
- [12. Riscos e Impactos](#12-riscos-e-impactos)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## 1. Princípios da Migração

1. **Coexistência controlada**: em nenhum momento a aplicação fica sem autenticação — legacy e novo operam em paralelo até o fim.
2. **Feature flags**: cada etapa possui flag para alternar comportamento (`AUTH_PROVISIONING_ENABLED`, `AUTH_IDENTITY_LAYER_ENABLED`, `AUTH_CURRENT_USER_ENABLED`), permitindo rollback por serviço.
3. **Nenhuma mudança destrutiva**: banco e schema não são alterados de forma irreversível; colunas obsoletas são removidas apenas no fim.
4. **Usuários nunca perdem acesso**: o primeiro login provisiona automaticamente (o 500 atual é eliminado).
5. **Validação contínua**: cada sprint termina com critérios de aceite testáveis (incluindo o cenário de rollback).
6. **Keycloak permanece o único emissor de JWT**: nenhuma sprint introduz emissão de tokens pelo `crm-auth-service`.

---

## 2. Regra Permanente de Encerramento de Sprint

Ao encerrar **qualquer** sprint, de forma obrigatória:

1. **Atualizar a documentação** afetada pela sprint (esta seção e docs correlatas).
2. **Executar validações/testes** e corrigir todas as falhas encontradas.
3. **Confirmar os critérios de aceite** da sprint — nenhuma pendência conhecida pode permanecer.
4. **Criar um único commit atômico por sprint**, com mensagem em *Conventional Commits*.
5. **Não fazer push** e **não iniciar a próxima sprint automaticamente**.
6. **Entregar relatório obrigatório** contendo: resumo técnico, documentação atualizada, arquivos alterados, testes executados, critérios de aceite atendidos, hash do commit e confirmação de que nada foi enviado ao remoto.

O próximo sprint só inicia após **aprovação explícita** deste relatório.

---

## 3. Visão Geral das Sprints

| Sprint | Foco | Entregável principal | Dependência |
|---|---|---|---|
| **1** | Provisionamento automático + eliminação do 500 | Primeiro login provisiona usuário; `GET /auth/me` → 200 | Atual crm-backend |
| **2** | Estrutura do `crm-auth-service` | Serviço (provisionamento + sync + RBAC + `CurrentUser`) + shared starter + resolução via gateway | Sprint 1 |
| **3** | Migração do Frontend | Fluxo OIDC + PKCE 100% com o JWT do Keycloak; fim do TokenManager dual | Sprint 2 |
| **4** | Microsserviços consumindo o Auth Service | Backend consome identidade de aplicação (CurrentUser); fim da emissão própria de tokens | Sprint 2/3 |
| **5** | Eventos + Auditoria + Observabilidade | RabbitMQ, audit completo, métricas, correlação de sessões | Sprint 4 |
| **6** | Hardening + Limpeza | Remoção do legacy, reconciliação, carga, rollback validado, docs de síntese | Sprint 5 |

---

## 4. Sprint 1 — Provisionamento Automático e Eliminação do 500

### Objetivo

Eliminar o erro `500` de `GET /api/v1/auth/me` quando o usuário autenticado ainda **não existe** no banco CRM, implementando o provisionamento automático no caminho atual (crm-backend).

### Escopo

- Auto-provisionamento no primeiro login: vínculo por `sub`/e-mail, criação de usuário, empresa/tenant default e role default (PROVISIONING.md).
- Ajuste da cadeia `KeycloakJwtAuthenticationConverter` → `CrmPrincipal` para provisionar antes de resolver o usuário.
- Correção do fluxo `UserService.getUserById`/`UserRepositoryImpl.findById` para não falhar com `userId` nulo.
- Flag `AUTH_PROVISIONING_ENABLED` (default: ligada) para rollback imediato.
- `GET /api/v1/auth/me` retorna `CurrentUser` completo para usuários recém-provisionados.

### Status — Sprint 1 implementado (2026-07-31)

- **Provisionamento**: `AuthUseCase.provisionKeycloakUser` implementado em `AuthService` (vínculo por `sub` → e-mail → criação; sync apenas de campos vazios; conflito de e-mail vincula o `sub`, nunca duplica).
- **Concorrência**: criação em transação `REQUIRES_NEW` via self-proxy; corrida de `UNIQUE (email)` reabre o vencedor e o vincula (`DataIntegrityViolationException` capturada fora da transação principal — sem `UnexpectedRollbackException`).
- **Erros**: falhas previsíveis → `UserProvisioningException` → 401 (converter re-emite como `AuthenticationServiceException` no resource server; `GlobalExceptionHandler` também mapeia 401). Sem `InvalidDataAccessApiUsageException`/`userId = null`.
- **Rollback**: `AUTH_PROVISIONING_ENABLED=false` → usuários existentes seguem autenticando; usuários desconhecidos recebem 401 com mensagem clara.
- **Testes**: `AuthServiceProvisioningTest` (10 casos) e `KeycloakJwtAuthenticationConverterTest` (2 casos) — primeiro login, reuso, sync, corrida, sem `sub`, flag off, usuário desativado (com/sem flag), e tradução para 401.

### Critérios de Aceite

- [x] Primeiro login de usuário inexistente → provisionado e `/auth/me` retorna **200** (o 500 desaparece). *(coberto por testes + validação real: 500 → 200)*
- [x] Login de usuário existente com `keycloak_sub` diferente → vínculo por e-mail sem duplicar registro. *(coberto por teste)*
- [x] RBAC (roles/permissions) resolvido para o usuário novo no primeiro acesso. *(role default `AGENT` atribuída; RBAC resolvido no converter; confirmado em produção)*
- [x] Usuário desativado → `/auth/me` negado (401). *(novo: `rejectIfInactive` no provisionamento — `UserProvisioningException` → 401; validado em produção: INACTIVE → 401, reativado → 200)*
- [x] Rollback: `AUTH_PROVISIONING_ENABLED=false` restaura o comportamento anterior. *(usuários existentes OK; novos → 401)*

---

## 5. Sprint 2 — Estrutura do crm-auth-service

### Objetivo

Criar o `crm-auth-service` como camada de identidade da aplicação e o shared starter, **sem emissão de tokens nem JWKS próprio**.

### Escopo

- Repositório/módulo `crm-auth-service` (Clean Architecture, mesmo padrão do backend).
- Serviço no `docker/docker-compose.yml` (porta dedicada, network `crm-network`, Redis/RabbitMQ).
- Validação do JWT do Keycloak (JWKS do Keycloak) e extração de claims.
- Módulos: provisioning, sync, resolução de usuário/empresa, RBAC resolver e `CurrentUser` service.
- `GET /auth/me` e `POST /auth/current-user` (resolve `CurrentUser`; não emite token).
- Biblioteca `crm-security-spring-boot-starter`: validação via JWKS do Keycloak + mapeamento do `CurrentUser` recebido do gateway.
- Gateway passa a resolver e propagar o `CurrentUser` (flag `AUTH_CURRENT_USER_ENABLED`).

### Critérios de Aceite

- [ ] `crm-auth-service` sobe via Docker Compose e responde `/auth/health` (200).
- [ ] `POST /auth/current-user` retorna `CurrentUser` válido a partir de um JWT real do Keycloak.
- [ ] Gateway enriquece a requisição com `CurrentUser` (flag on) e o starter o mapeia corretamente.
- [ ] `GET /auth/me` no auth-service retorna 200 (usuário existente e recém-provisionado).
- [ ] **Nenhum endpoint de emissão de token/JWKS existe no auth-service** (grep ausente).
- [ ] Rollback: `AUTH_CURRENT_USER_ENABLED=false` mantém o backend validando Keycloak normalmente.

---

## 6. Sprint 3 — Migração do Frontend

### Objetivo

Migrar o frontend para operar **exclusivamente com o JWT do Keycloak** (OIDC + PKCE, client público), eliminando o TokenManager dual e a dependência do JWT próprio do backend.

### Escopo

- Cliente público `crm-frontend` no Keycloak (realm CRM) com PKCE S256.
- Login: fluxo OIDC + PKCE direto com o Keycloak (mantém `keycloak-js` como biblioteca OIDC).
- Remoção do TokenManager dual: apenas o JWT do Keycloak é armazenado/distribuído.
- Interceptor 401: renovação via refresh/SSO do Keycloak (em vez de token próprio).
- Logout: `end_session_endpoint` do Keycloak.
- Middleware/guards: leitura do token/cookie permanece (padrão atual).

### Critérios de Aceite

- [ ] Login via Keycloak funciona ponta a ponta com JWT do Keycloak apenas.
- [ ] `/auth/me` carregado no dashboard retorna dados reais (200) para usuário novo e existente.
- [ ] Refresh/SSO sem logout forçado (sessão > TTL do token).
- [ ] Logout limpa tokens e encerra sessão SSO.
- [ ] Nenhuma referência ao token próprio do backend no frontend (grep ausente).
- [ ] Rollback: reverter build/flags volta o frontend ao fluxo dual atual.

---

## 7. Sprint 4 — Microsserviços consumindo o Auth Service

### Objetivo

Fazer o `crm-backend` (e serviços futuros) consumirem o `crm-auth-service` como **camada de identidade da aplicação**, eliminando a emissão própria de tokens e reduzindo o acoplamento direto com o Keycloak.

### Escopo

- Remoção da emissão de tokens no `AuthService` (login/refresh/logout legacy) e do `JwtAuthenticationFilter`/`JwtTokenProvider`.
- Substituição do `KeycloakJwtAuthenticationConverter` pela resolução via starter + `CurrentUser` (vindo do gateway).
- Substituição de `CrmPrincipal` por `CurrentUser` nos controllers (compatibilidade de métodos).
- Manutenção da validação do JWT do Keycloak (JWKS do Keycloak) nos serviços.
- Manutenção dos endpoints de administração (`/users`, `/roles`, `/permissions`) e perfil, consumindo `CurrentUser`.
- Flag `AUTH_IDENTITY_LAYER_ENABLED` por serviço.

### Critérios de Aceite

- [ ] Todos os endpoints autenticados funcionam com JWT do Keycloak + `CurrentUser` do gateway.
- [ ] Nenhuma emissão de token própria permanece no backend (grep: `JwtTokenProvider`/`JwtAuthenticationFilter` ausentes).
- [ ] `/auth/me`, `/auth/refresh`, `/auth/logout` deixam de existir no backend (delegados ao Keycloak/auth-service).
- [ ] Auditoria identifica o usuário corretamente a partir do `CurrentUser`.
- [ ] Rollback: `AUTH_IDENTITY_LAYER_ENABLED=false` restaura o comportamento anterior (código legacy removido apenas na Sprint 6).

---

## 8. Sprint 5 — Eventos, Auditoria e Observabilidade

### Objetivo

Ampliar a integração de eventos e a observabilidade da autenticação.

### Escopo

- Produtor RabbitMQ no auth-service; consumidores (Audit, Notification, Analytics).
- Auditoria completa de eventos de autenticação (substitui `AuditEventListener` in-process).
- Métricas: taxa de login, falhas, latência de resolução de `CurrentUser`, renovações.
- Correlação de sessões multi-dispositivo e histórico.

### Critérios de Aceite

- [ ] Eventos `auth.*` publicados no RabbitMQ com payloads conforme EVENTS.md.
- [ ] `audit_logs` registra login/logout/provisionamento/role_changed.
- [ ] Métricas de autenticação disponíveis em `/actuator/prometheus`.
- [ ] Rollback: DLQ e reprocessamento funcionam; auditoria não perde eventos.

---

## 9. Sprint 6 — Hardening, Limpeza e Dependências Desnecessárias

### Objetivo

Remover o caminho legacy, eliminar dependências diretas desnecessárias do Keycloak, validar carga e encerrar a migração.

### Escopo

- Remoção do fluxo legacy (login direto email/senha, tokens HS256, coluna/rotas obsoletas).
- Remoção de acoplamentos diretos ao Keycloak já substituídos pela camada de identidade (converter, config duplicada nos serviços).
- Job de reconciliação periódica Keycloak ↔ CRM (não-destrutivo).
- Testes de carga (login, resolução de `CurrentUser`, renovação).
- Validação final do runbook de rollback; atualização de documentação de síntese (SUMMARY, SECURITY_MAP, ARCHITECTURE_DECISIONS).
- (Opcional) cookies httpOnly para hardening do armazenamento do JWT no frontend.

### Critérios de Aceite

- [ ] Nenhuma referência a `JwtAuthenticationFilter`, `JwtTokenProvider`, `KeycloakJwtAuthenticationConverter` ou TokenManager dual no código.
- [ ] Benchmark de login/resolução dentro do SLO (ex.: p95 < 500ms).
- [ ] Rollback testado e documentado (restauração de deploy da última versão com suporte legacy).
- [ ] Documentação de síntese atualizada.

---

## 10. Estratégia de Rollback

### Por serviço (feature flags)

| Serviço | Flag | Valor legacy | Valor alvo |
|---|---|---|---|
| `crm-backend` | `AUTH_PROVISIONING_ENABLED` | `false` (500 atual) | `true` (auto-provisioning) |
| `crm-backend` | `AUTH_IDENTITY_LAYER_ENABLED` | `false` (resolve no próprio serviço) | `true` (consome `CurrentUser` do gateway) |
| Gateway | `AUTH_CURRENT_USER_ENABLED` | `false` | `true` (resolvem/propagam `CurrentUser`) |
| Frontend | `NEXT_PUBLIC_AUTH_FLOW` | `dual` | `oidc` (Keycloak exclusivo) |

### Cenários de rollback

| Cenário | Ação | Impacto |
|---|---|---|
| Provisionamento com bug | `AUTH_PROVISIONING_ENABLED=false`; usuários existentes seguem autenticando | Usuários novos aguardam correção |
| Auth-service instável | Desligar `AUTH_CURRENT_USER_ENABLED`; backend volta a resolver no próprio serviço | Segundos |
| Regressão no frontend | Reverter build do frontend (fluxo dual) | Imediato |
| Corrupção de dados | Restore do backup (BACKUP_RECOVERY.md) | Horas |

### Garantias

- O código legacy (JWT HS256, converter Keycloak) é **removido apenas na Sprint 6**; até lá, qualquer deploy pode ser revertido.
- Nenhuma migração de banco remove colunas antes da Sprint 6.
- Feature flags versionadas e observáveis.
- O Keycloak permanece o único emissor em todos os cenários; rollback nunca introduz um emissor paralelo.

---

## 11. Critérios de Aceite Consolidados

| # | Critério (macro) | Sprint |
|---|---|---|
| CA-1 | Primeiro login provisiona usuário; `/auth/me` retorna 200 (elimina o 500 atual) | 1 |
| CA-2 | Auth-service operacional (provisionamento/sync/RBAC/CurrentUser); starter + gateway resolvem `CurrentUser`; sem emissão de tokens | 2 |
| CA-3 | Frontend opera 100% com JWT do Keycloak (OIDC + PKCE); TokenManager dual removido | 3 |
| CA-4 | Backend consome a camada de identidade; sem emissão própria de tokens; acoplamento Keycloak reduzido | 4 |
| CA-5 | Eventos `auth.*` no RabbitMQ; auditoria completa | 5 |
| CA-6 | Legacy removido; carga OK; rollback documentado e testado | 6 |

---

## 12. Riscos e Impactos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Quebra de compatibilidade de `CurrentUser` (claims/contexto) | Média | Alto | Starter versionado; migração por serviço com flag |
| Regressão no fluxo de login (indisponibilidade) | Média | Alto | Coexistência até a Sprint 6; rollback por serviço |
| Provisionamento incorreto (duplicação/conflito de e-mail) | Média | Médio | Regras de vínculo (PROVISIONING.md §3); idempotência; evento de auditoria |
| Defasagem de RBAC (JWT stateless × banco CRM) | Média | Médio | TTL curto; re-resolução no refresh; mapeamento opcional de roles no JWT via mapper |
| Keycloak indisponível | Baixa | Alto | Alerta; SLO de disponibilidade; reconexão; sessões locais continuam até expiração |
| Latência de resolução de `CurrentUser` (RBAC por request) | Baixa | Médio | Cache com TTL curto e invalidação por evento; resolução só no gateway |
| `GET /auth/me` voltar a falhar se provisionamento desativado | Média | Alto | Flag de provisionamento independente; monitoramento de erros 5xx no `/auth/me` |
| Impacto em auditoria (mudança de publisher) | Média | Médio | Nomes/routing keys preservados; testes de contrato |
| Esforço/estimativa subestimada | Média | Médio | Sprints pequenas e aceites testáveis; backlog contínuo |

### Impactos por área

| Área | Impacto |
|---|---|
| Backend | Fim da emissão de tokens próprios; `CrmPrincipal` → `CurrentUser`; `AuthService` reduzido; conversor substituído pelo starter |
| Frontend | Fim do TokenManager dual; fluxo OIDC + PKCE exclusivo com Keycloak |
| Banco | Nenhuma mudança destrutiva; possível nova tabela de sessões/auditoria do auth-service |
| Keycloak | Cliente público `crm-frontend` (PKCE); roles mantidas; (opcional) role mapper de RBAC |
| DevOps | Novo serviço no compose; variáveis de flag; nginx para `auth.` |
| Segurança | Menor superfície (um único emissor — Keycloak); RBAC centralizado na camada de identidade |

## Referências

| Documento | Relação |
|---|---|
| [README.md](./README.md) | Índice da seção |
| [OVERVIEW.md](./OVERVIEW.md) | Arquitetura alvo |
| [PROVISIONING.md](./PROVISIONING.md) | Provisionamento (Sprints 1-2) |
| [AUTHORIZATION.md](./AUTHORIZATION.md) | RBAC (Sprints 2/4) |
| [AUTH_SERVICE_API.md](./AUTH_SERVICE_API.md) | APIs (Sprints 2-4) |
| [EVENTS.md](./EVENTS.md) | Eventos (Sprint 5) |
| [04-integrations/KEYCLOAK_INTEGRATION.md](../04-integrations/KEYCLOAK_INTEGRATION.md) | Estado atual e rollback do fluxo Keycloak |
| [ARCHITECTURE_DECISIONS.md](../ARCHITECTURE_DECISIONS.md) | ADR-007 (JWT) — a ser sucedido pelo ADR de nova arquitetura |

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-31 | Architect | Sprint 0 — plano de migração por sprints, rollback, critérios de aceite e riscos |
| 1.1.0 | 2026-07-31 | Architect | Ajuste: reordenadas as sprints (1 = auto-provisionamento/500, 2 = estrutura do auth-service, 3 = frontend, 4 = serviços, 5 = eventos, 6 = hardening); Keycloak único emissor (sem JWKS/emissão no auth-service); adicionada regra permanente de encerramento de sprint |
| 1.2.0 | 2026-07-31 | Architect | Sprint 1 — implementado (provisionamento no crm-backend, REQUIRES_NEW, flag de rollback, falhas 401, rejeição de usuário desativado); critérios de aceite atualizados e validados em produção (500 → 200; INACTIVE → 401) |
