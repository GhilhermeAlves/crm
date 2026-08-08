# Sprint 7.0 — Identity Provider Architecture (fundação de identidade)

**Data:** 2026-08-05 · **Ambiente:** local (código 100% local; sem deploy nesta sprint) · **Status:** ✅ Concluída

> **Nota (2026-08-06):** decisão de escopo — a Sprint 7 tem como IdP externo **somente o
> Google**. Microsoft/Entra e Apple/Sign in with Apple estão **FORA DO ESCOPO ATUAL**
> (permanecem apenas como registro "preparado" no catálogo de código, não habilitados). A
> estrutura oficial da Sprint 7 é 7.0 → 7.4 (ver `sprints/7.0/IDENTITY_ROADMAP.md`).

## Identificação

- **Sprint:** 7.0
- **Nome:** Identity Provider Architecture — catálogo de provedores + `kc_idp_hint` (backend) e fundação da tela de login com providers (frontend)
- **Responsável:** AI Agent
- **Fase:** Segurança — Access Gateway / Identidade
- **Dependência:** Sprint 6.10

## Objetivo

Preparar a arquitetura de identidade para Google, Microsoft/Outlook, Apple/iCloud e
Telefone/OTP sobre o Keycloak Identity Brokering, **mantendo o login por e-mail+senha e
preservando integralmente o fluxo Gateway/OIDC/sessão das Sprints 6.x** (`/login` →
Keycloak → `/auth/callback` → `/dashboard`). Nesta sprint o código é 100% local: catálogo
de provedores + hint `kc_idp_hint` no backend e renovação da tela de login (tokens de
design, slot de logo, botões de provedor) no frontend. Nenhum provedor externo é habilitado
sem credenciais reais.

## Escopo

- **Backend (auth-service):** catálogo de Identity Providers (`IdentityProviderCatalog` +
  `ConfiguredIdentityProviderCatalog`), endpoint público `GET /auth/providers`, hint
  `kc_idp_hint` no `/auth/authorize` via parâmetro `provider`, configuração
  `auth.gateway.enabled-providers`.
- **Frontend:** tokens de design `--crm-*` + mapeamento Tailwind (`crm-*`), `LoginBrand`
  (slot de logo + fallback textual), `IdentityProviderButton` + `ProviderList`, hook
  `useIdentityProviders` (catálogo), nova página `(auth)/login` com provedores + acesso
  clássico e-mail/senha, `loginWithGateway(redirect?, provider?)`.
- **Regra de não-regressão:** fluxo 6.x inalterado quando `provider` não é informado.
- **Documentação:** `sprints/7.0/REPORT.md`, `sprints/7.0/IDENTITY_ROADMAP.md`,
  `sprints/SPRINT_INDEX.md`.

## Decisões de escopo (confirmadas pelo usuário)

1. **Não implementar Meta/Facebook** — registrado como fora de escopo no catálogo.
2. **Não fixar logo definitivo** — apenas slot/área de logo (`LoginBrand`) com fallback
   textual; a troca futura é fornecer um `logoSrc` (SVG/PNG).
3. **Nenhuma credencial de provedor no frontend** — o browser não conhece client
   secret/token; a sessão permanece server-side (cookie HttpOnly).
4. **Nenhum log de code/OTP/senha** — apenas o alias do provedor é logado.
5. **Nenhuma autenticação paralela ao Keycloak** — tudo passa pelo Access Gateway.
6. **Provedores apenas "preparados", nenhum habilitado** — `enabled-providers` vazio por
   padrão; Google/Microsoft/Apple/Telefone só ficam disponíveis após configurar o IdP no
   Keycloak com credenciais reais (sprints seguintes).
7. **Código 100% local nesta sprint** — sem deploy na VPS; validação de produção fica para
   a sprint que habilitar o primeiro provedor real.

## Auditoria pré-execução (estado da VPS — sem alterações)

- `ssh crm-vps` → `https://srv1348261.hstgr.cloud` (`76.13.237.238`).
- `crm-keycloak` healthy; Keycloak admin API interna `http://localhost:8080` no host VPS
  funcional (realm `master`, token OK).
- Realm **CRM**: `registrationAllowed=False`, `verifyEmail=False`,
  `duplicateEmailsAllowed=False`, `bruteForceProtected=True`, **0 identity providers**.
- Clients OIDC: `crm-gateway` (confidencial), `crm-frontend` (público), `broker`,
  `account`/`account-console`/`admin-cli`/`realm-management`.
- Envs reais: `/opt/crm/.env` (linha `EOF` avulsa que quebrava o `source` foi removida com
  `sed -i "/^EOF$/d"`), `/opt/crm/docker/.env`.
- Nginx: `location /auth/` → `127.0.0.1:8082` (auth-service) — o endpoint
  `/auth/providers` já é alcançável sem mudança de infra.

## Backend — catálogo de Identity Providers

- `IdentityProviderCatalog` (novo, `application/gateway/port/input`):
  - `record IdentityProviderInfo(String alias, String label, boolean available)`.
  - `List<IdentityProviderInfo> list()` e `Optional<IdentityProviderInfo> find(String)`.
- `ConfiguredIdentityProviderCatalog` (novo, `application/gateway/service`):
  - Registro fixo em ordem estável: `google` (Google), `microsoft` (Microsoft),
    `apple` (Apple), `phone` (Telefone). Meta **ausente**.
  - `available = alias ∈ auth.gateway.enabled-providers` (vazio por padrão).
- `OidcGatewayProperties`: campo `Set<String> enabledProviders` (+getter/setter).
- `GatewayConfig`: bean `identityProviderCatalog(...)` alimentado pelo properties.
- `SecurityConfig`: `/auth/providers` adicionado aos paths públicos (`permitAll`).

## Backend — hint `kc_idp_hint` no authorize

- `GatewayOidcUseCase`:
  - `BeginAuthorization beginAuthorization(String redirect, String provider)` — o `provider`
    é opcional e repassado ao gateway.
  - Overload default `beginAuthorization(String redirect)` — chama com `null`, preservando o
    fluxo 6.x (nenhum call-site antigo quebrado).
- `GatewayOidcService`:
  - Ctor ganhou `IdentityProviderCatalog` (4 call-sites atualizados em testes).
  - `applyIdentityProviderHint(builder, provider)`: sem provider → fluxo idêntico ao 6.x;
    alias desconhecido → `400 UNKNOWN_PROVIDER`; conhecido mas não habilitado →
    `400 PROVIDER_NOT_AVAILABLE`; habilitado → adiciona `kc_idp_hint={alias}` à URL de
    autorização do Keycloak (Identity Brokering).
  - Log apenas do alias — nunca de code/OTP/senha/secret.
- `OidcGatewayController`:
  - Novo `GET /auth/providers` (público) → `List<IdentityProviderInfo>`.
  - `GET /auth/authorize` aceita `provider` (não-required) e repassa ao use case.

## Frontend — tokens de design (Sprint 7.0)

- `src/styles/globals.css`: tokens `--crm-*` em `:root` e `.dark` (HSL):
  `--crm-primary/hover/active/foreground`, `--crm-secondary`, `--crm-text/text-secondary`,
  `--crm-border`, `--crm-background`, `--crm-surface`, `--crm-danger`, `--crm-success`.
  Paleta base: fundo branco, primário azul, detalhes cinza. Trocar a marca futura =
  alterar estas variáveis, sem refatorar componentes.
- `frontend/tailwind.config.ts`: namespace `crm-*` mapeado às variáveis.
- `ui/button.tsx`: nova variant `crm` (primário azul com hover/active/focus próprios) —
  evita conflito de ordem de classes entre `bg-primary` e `bg-crm-primary`.

## Frontend — componentes

- `LoginBrand` (`components/brand/LoginBrand.tsx`): slot de logo (size sm/md/lg, variant
  desktop/mobile) com fallback textual temporário (iniciais do wordmark). Suporte futuro a
  SVG/PNG via `logoSrc`.
- `identity-provider-icons.tsx`: ícones Google (cores oficiais), Microsoft, Apple e
  Telefone (feather phone).
- `IdentityProviderButton` (`features/auth/components/IdentityProviderButton.tsx`):
  botão único de provedor abstraindo ícone, label, loading (spinner), disabled, badge
  "Em breve" e erro — nenhuma lógica duplicada por provedor.
- `ProviderList` (`features/auth/components/ProviderList.tsx`): catálogo fixo em ordem
  estável (Google, Microsoft, Apple, Telefone), disponibilidade vinda do servidor; enquanto
  o catálogo não chega (ou falha) **todos ficam desabilitados** (seguro). Clique → `loginWithGateway(redirect, alias)` preservando o `redirect` da URL.
- `useIdentityProviders` (`features/auth/hooks/useIdentityProviders.ts`): react-query sobre
  `GET /auth/providers` (`credentials: include`, staleTime 5min, retry false).
- `gateway-auth.ts`: `loginWithGateway(redirect?, provider?)` monta
  `/auth/authorize?redirect=...&provider=...` (URLSearchParams, encoding seguro).

## Frontend — página de login

- Nova `(auth)/login/page.tsx`: `LoginBrand` + título + `ProviderList` + divisor "ou" +
  `LoginForm` (e-mail e senha via Keycloak) + links "Criar conta"/"Esqueci minha senha".
- `AuthLayout` estilizado com tokens `crm-*` (fundo branco, card surface, bordas cinza).
- `LoginForm` (acesso clássico) inalterado em comportamento: usa a variant `crm` e rótulo
  "Entrar com e-mail e senha"; navegação segue via `useAuth.loginKeycloak` →
  `/auth/authorize` (fluxo 6.x intacto).
- `/register`, `/forgot-password`, `/reset-password`, `/auth/callback`, `/dashboard`
  inalterados. `middleware-auth.ts` inalterado (`/auth/callback` segue público).

## Segurança (decisões e preservações)

- **Preservado das Sprints 6.x:** rate limiting por IP/sessão, PKCE S256, cookie
  `crm_session` HttpOnly+Secure, CSRF cookie-to-header, correlation ID — nenhum filtro
  alterado.
- **Allowlist server-side:** o `provider` é validado contra o catálogo no servidor antes de
  virar `kc_idp_hint`; o browser não escolhe aliases arbitrários.
- **Estado desabilitado por padrão:** com `enabled-providers` vazio, todos os botões exibem
  "Em breve" e permanecem desabilitados; o guard client-side (`available`) é reforço, a
  decisão real é do servidor.
- **Sem exposição de credenciais:** nenhum client secret/token no frontend; log apenas do
  alias; `/auth/providers` devolve apenas alias/label/available (público, sem dados
  sensíveis).
- **Identidade única / account linking:** quando um IdP for habilitado, a criação de
  usuários continua via Keycloak (`verifyEmail=False`, `registrationAllowed=False`); o
  mapeamento e o account linking de e-mail duplicado serão decididos no roadmap 7.1/7.2 —
  não implementados nesta sprint.

## Testes locais

- **Backend (auth-service):** `mvn clean verify` → **239/239 testes PASS** (BUILD SUCCESS).
  Flake pré-existente isolado no `GatewayOidcRefreshTest` (14/14 na re-execução, sem relação
  com esta sprint).
- Novos testes backend: `ConfiguredIdentityProviderCatalogTest` (ordem, labels, Meta
  ausente, `available` por config, `find` vazio), `GatewayOidcServiceTest` (sem hint por
  padrão, `kc_idp_hint` com google habilitado, `UNKNOWN_PROVIDER` facebook,
  `PROVIDER_NOT_AVAILABLE` apple, provider em branco), `OidcGatewayControllerTest`
  (catálogo, forward `provider=google`, `/auth/providers` público).
- **Frontend:** lint PASS (0 erros; warnings pré-existentes de outros módulos) · typecheck
  PASS · tests **49/49 PASS** · `next build` PASS (login em 6.52 kB).
- Novos testes frontend: `gateway-auth.test.ts` (+provider, encoding), `ProviderList.test.tsx`
  (ordem, disponibilidade, redirect preservado, guard desabilitado, loading/error),
  `IdentityProviderButton.test.tsx` (label/ícone, click, "Em breve"+disabled, loading,
  erro), `LoginBrand.test.tsx` (fallback, logoSrc, wordmark, sizes).
- `vitest.config.ts`: `globals: true` habilitado para o auto-cleanup do Testing Library
  (testes multi-caso com render em jsdom).

## Deploy

**Não realizado (intencional).** Sprint 100% local. O endpoint `/auth/providers` e o hint
`kc_idp_hint` entram em produção junto com o primeiro provedor real (7.1+), quando o deploy
será controlado (backup → build → `up -d` → regressão 6.6–6.9 + login real).

## Pendências / recomendações

- **Credenciais reais** de Google OAuth (Cloud Console) para configurar o IdP no Keycloak e
  habilitar `AUTH_GATEWAY_ENABLED_PROVIDERS` (7.1 — consumidas; Google habilitado).
- **Microsoft Entra e Apple Developer Program:** **fora do escopo da Sprint 7** (decisão
  2026-08-06) — não são dependências ativas; nenhuma credencial será requisitada.
- **Provedor SMS** para Telefone/OTP (abstração de envio — roadmap 7.3).
- **Validação na VPS** (`GET /auth/providers` e login real) apenas quando houver deploy.
- **Account linking** de e-mails duplicados entre IdP e conta local (7.2).
- **Logo definitivo** — fornecer SVG/PNG para `LoginBrand.logoSrc`.

## Resultado

STATUS: **CONCLUÍDA**.

- Arquitetura de identidade preparada (catálogo + `kc_idp_hint`) sem quebrar o fluxo 6.x
  (backup default `beginAuthorization(redirect)`).
- Frontend de login renovado (tokens, logo slot, providers) com estado seguro por padrão
  (todos os provedores desabilitados até serem habilitados no servidor).
- Suítes verdes: backend 239/239, frontend 49/49 + lint/typecheck/build PASS.
- Nenhuma credencial de provedor inventada ou exposta; Meta fora de escopo.

## Próxima sprint (roadmap)

- **7.1 — Login/Cadastro com Google** (✅ concluída): IdP `google` no Keycloak com
  credenciais reais, `enabled-providers=google`, deploy controlado + regressão e E2E. Ver
  `sprints/7.1/REPORT.md`.
- **7.2 — Account Linking**, **7.3 — Telefone/OTP** e **7.4 — Recuperação de conta e
  segurança da identidade**: próximas. Apple/Microsoft fora do escopo da Sprint 7. Ver
  `sprints/7.0/IDENTITY_ROADMAP.md`.
