# Sprint 7.1 — Login & Cadastro com Google (Keycloak Identity Brokering)

**Data:** 2026-08-05/06 · **Ambiente:** local + VPS (produção `srv1348261.hstgr.cloud`) · **Status:** ⏳ Em validação final (aguardando E2E interativo do usuário)

## Identificação

- **Sprint:** 7.1
- **Nome:** Login & Cadastro com Google — IdP `google` real no Keycloak (Identity Brokering),
  catálogo + `kc_idp_hint` em produção, sincronização do código 7.0 na VPS e deploy controlado
- **Responsável:** AI Agent + usuário (E2E interativo)
- **Fase:** Segurança — Access Gateway / Identidade
- **Dependência:** Sprint 7.0 (arquitetura de provedores) + Sprints 6.6–6.10 (Gateway/OIDC/sessão)

## Objetivo

Habilitar **login e cadastro com Google** no CRM em produção, preservando o login por
e-mail+senha e o fluxo Gateway/OIDC/sessão das Sprints 6.x. Isso exigiu: (a) configurar o
IdP `google` no Keycloak (realm CRM) com credenciais reais, (b) sincronizar o código 7.0
(que a VPS ainda não tinha — rodava código da era `bdbd593`, 24 commits atrás do local)
e (c) habilitar `AUTH_GATEWAY_ENABLED_PROVIDERS=google` apenas no serviço auth-service do
compose de produção. Microsoft/Outlook permanece **bloqueado** (sem credenciais Entra),
Apple/Telefone permanecem preparados e desabilitados.

## Escopo

- **Keycloak (realm CRM, produção):** IdP `google` via Identity Brokering (client_id/
  client_secret, `useJwksUrl=true`, `syncMode=IMPORT`, `trustEmail=true`) + 4 mappers
  (username, email, first name, last name) — tudo via admin API `localhost:8080`.
- **Código na VPS (sincronização 7.0):** 9 arquivos do auth-service (catálogo de
  providers, `kc_idp_hint`, `/auth/providers` permitAll, properties) + 6 testes + 23
  arquivos do frontend (tokens `crm-*`, `ProviderList`, `IdentityProviderButton`,
  `LoginBrand`, `useIdentityProviders`, página de login, `gateway-auth` + testes).
- **Compose (produção):** `AUTH_GATEWAY_ENABLED_PROVIDERS=google` no serviço
  auth-service; rebuild controlado de auth-service e frontend; regressão de endpoints.
- **Deploy controlado:** backups pré-alteração, restarts com verificação de saúde,
  validação HTTP ponta-a-ponta até a página de sign-in do Google.
- **Fora de escopo:** account linking visual completo (7.4), Apple/Telefone (7.2/7.3),
  Microsoft (bloqueado sem credenciais). O linking de e-mail duplicado funciona via
  comportamento padrão do Keycloak (ver "Cadastro / account linking").

## Decisões de escopo (confirmadas pelo usuário)

1. **Credenciais Google reais autorizadas** — Client ID/Secret do Cloud Console usados
   apenas no Keycloak (admin API) e em arquivo temporário do VPS (apagado em seguida).
   Nunca no Git, no frontend ou em logs.
2. **Redirect URI autorizado** — `https://srv1348261.hstgr.cloud/realms/CRM/broker/google/endpoint`
   confirmado como registrado no Google Cloud Console.
3. **Microsoft bloqueado nesta sprint** — sem credenciais Entra; valida-se a resposta
   correta de negação (`400 PROVIDER_NOT_AVAILABLE`).
4. **`enabled-providers` contém apenas `google`** — Apple/Telefone seguem "preparados".
5. **Alterações em produção autorizadas** — Keycloak realm CRM (IdP + mappers) e VPS
   (código, compose, rebuild) com backups e validações.

## Estado inicial (auditoria — descobertas críticas)

- **A VPS estava atrás do código local:** auth-service e frontend rodavam código da era
  `bdbd593` (Sprints 6.x, ~24 commits atrás do local `2f86f4b`). O frontend tinha
  `gateway-auth.ts` mas **não tinha** os componentes UI da 7.0 (`ProviderList`,
  `IdentityProviderButton`, `LoginBrand`, `useIdentityProviders`, página de login com
  providers). O auth-service **não tinha** `IdentityProviderCatalog`/`/auth/providers`.
  Deploy é cópia manual (worktree VPS com centenas de arquivos modificados/untracked; sem
  git push/pull) — a sincronização foi feita por tar+scp arquivo a arquivo.
- Realm CRM (pré-configuração): `registrationAllowed=False`, `verifyEmail=False`,
  `duplicateEmailsAllowed=False`, `bruteForceProtected=True`, **0 identity providers**.
- Envs: `/opt/crm/.env` e `/opt/crm/docker/.env`; o compose de aplicação lê
  `/opt/crm/docker/.env` (a variável `AUTH_GATEWAY_ENABLED_PROVIDERS` entrou **no
  compose**, não no `/opt/crm/.env`, que não é lido pelos services de aplicação).
- Nginx `location /auth/` → `127.0.0.1:8082` (auth-service) — `/auth/providers` e
  `/auth/authorize` já alcançáveis sem mudança de infra.

## Keycloak — IdP Google (produção, via admin API)

- Admin API interna `http://localhost:8080/admin/realms/CRM` (token `admin-cli` do
  `/opt/crm/.env`).
- **IdP `google` criado (HTTP 201)** e verificado: `alias=google`, `providerId=google`,
  `enabled=true`, `useJwksUrl=true`, `syncMode=IMPORT`, `trustEmail=true`,
  `defaultScope=openid profile email`, `clientId`/`clientSecret` reais.
- **Mappers (estado final — após correção do NPE):**
  - Criados (HTTP 201): **email** ← claim `email`, **first name** ← `given_name`,
    **last name** ← `family_name` (todos `oidc-user-attribute-idp-mapper`).
  - **`username` REMOVIDO (HTTP 204)**: criado via API como `oidc-username-idp-mapper`
    com config `{claim: email, target: LOCAL}` **sem a chave `template`** — em Keycloak
    26.3.5 essa classe (`UsernameTemplateMapper`) exige `template` e quebrava o login com
    NPE (ver "Problemas"). Sem o mapper, o broker usa o **fallback padrão**: username =
    e-mail do ID token do Google (o Google não envia `preferred_username`).
- Realm (verificado pós-config): `registrationAllowed=false`, `verifyEmail=false`,
  `duplicateEmailsAllowed=false`, `bruteForceProtected=true` — nenhuma política de
  registro aberta; o "cadastro" via Google acontece pela primeira entrada no broker
  (`firstBrokerLoginFlow`), não pela página de registro.

## Cadastro / account linking (comportamento Keycloak documentado)

- **Caso A — e-mail já existe como usuário local no Keycloak:** `duplicateEmailsAllowed=false`
  + primeiro login via broker → Keycloak **vincula automaticamente** a identidade Google ao
  usuário existente (auto-link). Login direto.
- **Caso B — usuário novo (nunca acessou o CRM):** o `firstBrokerLoginFlow` padrão cria o
  usuário Keycloak a partir dos dados do Google (create user if unique). O backend então
  resolve o usuário CRM por `sub` → não existe → exige provisão
  (`PROVISIONING_REQUIRED`/`CRM_ACCESS_DENIED`) até um usuário CRM ser vinculado — o mesmo
  fluxo do e-mail+senha. (Validação final pendente do E2E.)
- **Caso C — e-mail duplicado entre duas identidades:** protegido por
  `duplicateEmailsAllowed=false`; o broker não cria conta duplicada.
- **Caso D — e-mail Google pertence a um usuário CRM que não tem conta Keycloak:**
  precisa de provisionamento (fora de escopo; roadmap 7.4).
- UI dedicada de linking (escolha de qual conta vincular) não foi implementada — fica para
  7.4. Nesta sprint o linking é o automático/seguro do Keycloak.

## Backend na VPS (sincronização 7.0 + validação)

- Sincronizados (tar+scp, após backup): `IdentityProviderCatalog.java`,
  `ConfiguredIdentityProviderCatalog.java`, `GatewayOidcService.java`,
  `GatewayOidcUseCase.java`, `GatewayConfig.java`, `SecurityConfig.java`,
  `OidcGatewayProperties.java`, `OidcGatewayController.java`, `.env.example` + testes
  `ConfiguredIdentityProviderCatalogTest`, `GatewayOidcServiceTest`,
  `OidcGatewayControllerTest`, `GatewayOidcLogoutTest`, `GatewayOidcRefreshTest`,
  `GatewayRateLimitConcurrencyTest`.
- **Endpoint público `GET /auth/providers`** (permitAll) → catálogo google/microsoft/
  apple/phone com `available` = habilitado no properties.
- **`GET /auth/authorize?provider=...`** → `kc_idp_hint` ao Keycloak; alias desconhecido
  → `400 UNKNOWN_PROVIDER`; conhecido mas não habilitado → `400 PROVIDER_NOT_AVAILABLE`;
  sem provider → fluxo 6.x idêntico.

## Frontend na VPS (sincronização 7.0)

- 23 arquivos: `login/page.tsx` (provedores + e-mail/senha), `AuthLayout`, `LoginForm`,
  `LoginBrand` + teste, `ProviderList` + teste, `IdentityProviderButton` + teste,
  `identity-provider-icons.tsx`, `useIdentityProviders.ts`, tipos `identity-provider`,
  `gateway-auth.ts` + teste, `globals.css`, `tailwind.config.ts`, `vitest.config.ts`,
  `components.json`, `useAuth.test.tsx`, `ProtectedRoute.test.tsx`.
- Não sobrescritos (arquivos locais do VPS inexistentes no git): `.env.local`,
  `.env.production`, `next-env.d.ts`, `tsconfig.tsbuildinfo`, `useAuth.tsx.bak-6.9-pre`.

## Deploy (produção — controlado)

- **Backups:** `/opt/crm/backups/sprint-7.1-pre-20260806-015907/` (compose + envs root/docker)
  e `/opt/crm/backups/sprint-7.1-pre-20260806-020737/` (`auth-service-files/`,
  `frontend-files/`).
- **Compose:** linha `AUTH_GATEWAY_ENABLED_PROVIDERS=google` adicionada ao serviço
  auth-service (`/opt/crm/docker/docker-compose.yml`).
- **Build/restart:** `docker compose build` + `up -d` de `crm-auth-service` (rebuilt;
  `HEALTH=healthy`) e `crm-frontend` (rebuilt; "Compiled successfully"). Demais serviços
  intocados.
- **Limpeza:** arquivos temporários do VPS com o client secret (`/tmp/google-idp.json`
  etc.) e manifests removidos — o secret permanece apenas no Keycloak.

## Validação em produção

- `GET /auth/providers` (público) → google `available=true`; microsoft/apple/phone
  `available=false`. ✔
- `GET /auth/authorize` sem provider → 302 login Keycloak **sem** `kc_idp_hint`
  (fluxo 6.x preservado). ✔
- `GET /auth/authorize?provider=google` → 302 com `kc_idp_hint=google`. ✔
- `GET /auth/authorize?provider=microsoft` → **400 `PROVIDER_NOT_AVAILABLE`**. ✔
- Cadeia curl completa (authorize → Keycloak broker → Google) até
  `accounts.google.com/v3/signin/...` (HTTP 200) com `client_id` e
  `redirect_uri=https://.../realms/CRM/broker/google/endpoint` — **o Google aceita o
  Client ID e o redirect URI**. ✔
- `/login` VPS HTTP 200; bundle do frontend contém `auth/providers` (código 7.0 servido). ✔

### E2E interativo (pendente — ação do usuário)

O passo final (clicar em "Continuar com Google" → conta
`paulo.alves@praiaclube.org.br` → voltar ao CRM logado em `/dashboard`) **requer sessão
interativa**: o Google bloqueia automação de login. O usuário deve executar em
`https://srv1348261.hstgr.cloud/login`.

## Problemas / observações

- **NPE no callback do Google (RESOLVIDO — causa raiz encontrada):**
  `Cannot invoke "java.lang.CharSequence.length()" because "this.text" is null` em
  `UsernameTemplateMapper.setUserNameFromTemplate` (linha 162). A stack trace mostrou o
  `Pattern.matcher(null)` → **o mapper "username" criado via admin API (`oidc-username-idp-mapper`)
  foi configurado com `{claim: email, target: LOCAL}` mas sem a chave `template`** — e em
  Keycloak 26.3.5 esse providerId instancia a classe `UsernameTemplateMapper`, que executa
  `SUBSTITUTION.matcher(config["template"])` e NPE quando `template` é null (a antiga classe
  `UsernameMapper`, que usava `claim`/`target`, **não existe mais** nessa versão — foi
  unificada na `UsernameTemplateMapper`).
  **Correção:** mapper "username" removido (`DELETE .../mappers/ea0612c3...` → HTTP 204);
  restaram 3 mappers (email/first name/last name). Sem o mapper, o broker usa o fallback
  padrão username = e-mail do Google (comportamento consistente com os usuários existentes,
  que logam por e-mail). Sem restart necessário (config lida do DB por request).
  Re-testar o login Google interativo.
- **A VPS rodava código 7.0-parcial antigo** — sem a sincronização, `/auth/providers` e a
  UI de providers simplesmente não existiam em produção. Corrigido com cópia manual
  arquivo a arquivo (sem git push/pull, preservando os arquivos locais do VPS).
- **Erros auxiliares em logs:** `Redirection URL does not contain a state parameter`
  (02:16/02:17, `127.0.0.1`) — foram os curls de teste batendo direto no endpoint do broker
  sem `state`; inofensivos e esperados.
- **Flake pré-existente** `GatewayOidcRefreshTest.shouldUpdateLastAccessedAtOnRefresh`
  (sensitive a timing; passa isolado) — sem relação com esta sprint.
- Warn obsoleto do compose (`version:`) — pré-existente, inofensivo.

## Testes locais (7.1)

- **Backend:** `mvn clean verify` → **245/245 testes PASS** (BUILD SUCCESS).
- Novos cenários 7.1:
  - `ConfiguredIdentityProviderCatalogTest`: shape de produção com `enabled-providers`
    só-google (google `available=true`, demais false) e microsoft sem credenciais
    indisponível.
  - `GatewayOidcServiceTest`: `400` para apple desabilitado, google não habilitado e
    microsoft sem credenciais; `kc_idp_hint` quando microsoft habilitado via properties.
  - `OidcGatewayControllerTest`: `GET /auth/authorize?provider=microsoft` → `400
    PROVIDER_NOT_AVAILABLE`.
- **Frontend:** lint PASS · typecheck PASS · tests **49/49 PASS** · `next build` PASS.

## Pendências / recomendações

- **E2E interativo (usuário):** login Google real até `/dashboard`; reportar sucesso ou
  erro (se NPE, coletar logs do Keycloak). O NPE do mapper foi corrigido — re-testar.
- **Credenciais Entra (Microsoft)** para habilitar o segundo provedor.
- **Apple Developer Program** para Sign in with Apple (7.2).
- **Provedor SMS** para Telefone/OTP (7.3).
- **Account linking visual** para Caso D (7.4).
- **Logo definitivo** para `LoginBrand.logoSrc`.

## Resultado

STATUS: **EM VALIDAÇÃO FINAL** (todas as etapas concluídas exceto o E2E interativo).

- IdP `google` real configurado no Keycloak (produção) com 3 mappers (email/first name/
  last name) + username via fallback padrão (e-mail); credenciais nunca expostas fora do
  Keycloak. NPE do mapper corrigido (username mapper removido).
- Código 7.0 sincronizado na VPS (backend catálogo+hint+providers público; frontend UI
  de providers) e `AUTH_GATEWAY_ENABLED_PROVIDERS=google` aplicado ao auth-service.
- Deploy controlado com backups; auth-service e frontend healthy; regressão de endpoints
  OK (google → `kc_idp_hint`; microsoft → 400; sem provider → fluxo 6.x).
- Google aceita Client ID/redirect URI na cadeia curl (até a página de sign-in).
- Suítes verdes: backend 245/245, frontend 49/49 + lint/typecheck/build PASS.

## Próxima sprint (roadmap)

- **7.2 — Apple / iCloud:** Apple Developer Program + `kc_idp_hint` apple + E2E.
- **7.4 — Account linking:** UI de escolha de conta vinculada e provisão para Caso D.
- **7.3 — Telefone/OTP** conforme roadmap. Ver `sprints/7.0/IDENTITY_ROADMAP.md`.
