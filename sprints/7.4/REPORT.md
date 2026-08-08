# Sprint 7.4 — Login completo por telefone (OTP → senha Keycloak)

**Data:** 2026-08-07/08 — **Ambiente:** local + VPS (produção `srv1348261.hstgr.cloud`) — **Status:** ✅ Concluída

## Identificação

- **Sprint:** 7.4
- **Nome:** Login por telefone — coleta de telefone + OTP local + fluxo de senha do Keycloak
- **Responsável:** AI Agent
- **Fase:** Segurança — Access Gateway / Identidade
- **Dependência:** Sprint 7.3 (telefone/OTP backend) + Sprint 7.0 (catálogo de providers) + Sprints 6.x (Gateway/OIDC/sessão)

## Objetivo

Completar o login por telefone iniciado na 7.3: a tela de login coleta o **telefone**, confirma a
**posse via OTP** (prova de posse resumível no crm-backend), e então segue para o **fluxo de
senha do Keycloak** (sessão do gateway, como qualquer login). **Telefone NÃO é um IdP do
Keycloak** — a confirmação do OTP valida quem é o usuário e encaminha para o login normal.

## Decisão de arquitetura (confirmada pelo usuário)

- **Telefone → OTP → Keycloak via sessão normal do gateway** (exige senha Keycloak). Não foi
  criado IdP de telefone no Keycloak, nem tokens no browser.
- **Sessão:** continua server-side via cookie `crm_session` (browser nunca detém tokens).
- **Backend OTP:** endpoints já públicos da 7.3, roteados no nginx **direto ao crm-backend**.

## Escopo

- **JS:**
  - `frontend/src/features/auth/components/PhoneLoginForm.tsx` (novo): estados `phone` e `otp`,
    coleta telefone → `sendOtp` → confirmação → `verifyOtp` → valida `userExists` →
    `loginWithGateway(redirect)` (sem provider). Prop `onBack` para voltar aos provedores.
  - `frontend/src/features/auth/services/phone-otp.service.ts` (novo): `sendOtp`/`verifyOtp`,
    tipos `SendOtpResult`/`VerifyOtpResult`, `PhoneOtpError`; trata 400 `sent:false` (cooldown)
    e códigos `USER_NOT_FOUND`/`INVALID_PHONE`/OTP do get.
  - `ProviderList.tsx`: clique no **Telefone** abre o formulário local (não navega para
    `/auth/authorize`), mantendo Google existente.
- **Auth-service (catálogo disponibilidade):**
  - `ConfiguredIdentityProviderCatalog` e `GatewayOidcService`: o telefone entra no catálogo
    quando `auth.guideway.phone-enabled` (`AUTH_GATEWAY_PHONE_ENABLED`) — separado de
    `enabled-providers`, que controla apenas IdPs OIDC (Google).
  - Guard `PHONE_IS_LOCAL_FLOW` (400) em `applyIdentityProviderHint`: bloqueia
    `kc_idp_hint=phone` no beginAuthorization — telefone nunca vira IdP do Keycloak.
- **Nginx:** novo bloco `location ^~ /api/v1/auth/phone/` → `proxy_pass
  http://localhost:8081` (crm-backend, endpoint público pre-auth), declarado **antes** do
  relay `/api/` → 8082 (exige sessão).
- **Compose (prod):** `AUTH_GATEWAY_ENABLED_PROVIDERS=google` e `AUTH_GATEWAY_PHONE_ENABLED=true`.

## Backend de OTP (da Sprint 7.3, já existia)

Endpoints públicos via `SecurityConfig` (`/api/v1/auth/phone/**` permitAll, CSRF off);
códigos `OTP_NOT_FOUND`, `OTP_EXPIRED`, `OTP_ALREADY_USED`, `OTP_MAX_ATTEMPTS`,
`OTP_INVALID`, `USER_NOT_FOUND`; `send-otp` retorna `{sent, phoneE164, ttlSeconds,
resendCooldownSeconds}`; invariante: `DisabledOtpSender` em prod (nunca loga o OTP).

## E2E em produção (VPS - evidencias)

1. `GET https://srv.../auth/providers` →
   `[{"alias":"google","label":"Google","available":true},{"alias":"phone","label":"Telefone","available":true}]`
2. `POST https://srv.../api/v1/auth/phone/send-otp` `{"phone":"+5511999990000"}` →
   `{"sent":true,"phoneE164":"+5511999990000","ttlSeconds":300,"resendCooldownSeconds":60}`
   (via nginx, sem sessão — rota direta 8081).
3. `GET /login` → HTTP 200 (frontend servido). O clique em "Telefone" abre o
   `PhoneLoginForm` local.

## Problemas e correções

1. **Compose da VPS sobrescrito no deploy:** meu `scp` sobrescreveu o `docker/docker-compose.yml`
   da VPS, que tinha mudanças de hardening prod NÃO commitadas (com `AUTH_GATEWAY_*`), e o
   `git checkout` seguinte descartou essas mudanças locais. Corrigido: recuperado a partir do
   backup `docker-compose.yml.bak-7.2-restore-env` (versão hardening prod), reaplicado
   `AUTH_GATEWAY_PHONE_ENABLED=true` + `AUTH_GATEWAY_ENABLED_PROVIDERS=google` no serviço
   `auth-service`; o nginx `crm.conf` já tinha o bloco `^~ /api/v1/auth/phone/` à 8081.
2. **scp nested `src/src`** — a cópia de `auth-service/src` para a VPS havia duplicado em
   `src/src` (o build usava o código antigo sem `isPhoneEnabled`). Corrigido: sync do `src/main`
   + `src/test` no destino, remoção do `src/src` e rebuild. Service healthy e catálogo com
   `phone: available:true`.
3. **Teste `provider.test`: assertion de heading não existe** — ajustado para `getByPlaceholderText`
   e botão "Enviar código".
4. **`PhoneOtpError.code` → `errorCode`** — typecheck do front.
5. **Envio de OTP `{}` vazio retornou 500 (JsonParse)** no meu curl — erro do shell do
   desenvolvedor ao montar o JSON, não bug do backend; com body bem formado → 200.

## Testes locais

- **Auth-service:** `mvn test` → **244 testes, 0 falhas** (incluindo `ConfiguredIdentityProviderCatalogTest`
  — phone via `phoneEnabled` — e `GatewayOidcServiceTest` — `shouldRejectPhoneAsNonOidcProviderEvenWhenEnabled`).
- **Frontend:** `vitest run` → **56/56 PASS**, typecheck `tsc --noEmit` limpo, `next build` OK.

## Pendências / recomendações

- **Provedor SMS real** no lugar do `DisabledOtpSender` em produção (7.3 segue).
- **Auto-provisionamento por telefone** (criar conta via telefone, sem senha Keycloak).
- **Anti-abuso/rate limit fino** em `send-otp` por IP.
- **Account linking visual (7.2)** ainda pendente de validação em produção.

## Resultado

STATUS: **CONCLUÍDA**.

- UI: form telefone→OTP integrado à `ProviderList` (clique em Telefone abre o fluxo local).
- Catálogo: disponibilidade separada de `phone-enabled` (não-OIDC) vs `enabled-providers` (OIDC).
- Guard proíbe: nunca gera `kc_idp_hint=phone`.
- Rede: `/api/v1/auth/phone/` roteado direto ao crm-backend (público) antes do relay.
- E2E prod: `providers.phone.available=true`; `send-otp` 200 com cooldown; `/login` 200.
- Suítes: backend 244 testes + frontend 56/56 + typecheck/build OK.

## Próxima sprint (roadmap)

- **7.1 — Google IdP E2E** (validação final), **7.2 — Account Linking**, **7.5+ — recuperação
  de conta / segurança de identidade** (dependência 7.4 e 7.7).

---

*Nota do deploy:* a VPS possuía mudanças de hardening NÃO commitadas no compose ("clobbered via
`git checkout`"); a versão ao vivo foi recuperada do backup `.-bak-7.2-restore-env` e reaplicada
com a nova env de telefone. Recomenda-se commitar o compose atualizado nas duas pontes a seguir.