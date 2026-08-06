# Identity Provider Roadmap (7.0 → 7.7)

> Roteiro da evolução de identidade do CRM sobre o Access Gateway (auth-service) + Keycloak
> Identity Brokering. Sprint 7.0 (concluída) entregou a **fundação**: catálogo de provedores,
> `kc_idp_hint` e a nova tela de login. A **7.1** habilitou o **Google** em produção (IdP real
> no Keycloak + código 7.0 sincronizado na VPS + deploy controlado); **Microsoft permanece
> bloqueado** por falta de credenciais Entra. As sprints seguintes habilitam Apple, Telefone
> e o account linking visual, sempre com deploy na VPS.

## Princípios invariantes

- **Não-regressão:** `/login` → Keycloak → `/auth/callback` → `/dashboard` nunca pode quebrar.
- **Sem autenticação paralela:** todo fluxo passa pelo Access Gateway; o browser nunca detém
  token de provedor.
- **Sem credencial inventada:** provedor só é habilitado com credencial real configurada no
  Keycloak (configuração = `AUTH_GATEWAY_ENABLED_PROVIDERS` + IdP no realm CRM).
- **Sem log de secret/OTP/senha.**
- **Meta/Facebook fora de escopo.**

## Matriz de provedores

| Provedor | Alias | Sprint | Status 7.1 | Prerequisito externo |
|----------|-------|--------|-------------|----------------------|
| Google | `google` | 7.1 | ✅ **Habilitado em produção** (E2E final pendente) | OAuth Client ID/Secret (Google Cloud Console) — ✅ consumido |
| Microsoft/Outlook | `microsoft` | 7.1 | ⛔ **Bloqueado** (sem credenciais Entra) — adiado | App registration (Microsoft Entra) — pendente |
| Apple/iCloud | `apple` | 7.2 | Preparado (não habilitado) | Apple Developer Program (Sign in with Apple) |
| Telefone/OTP | `phone` | 7.3 | No catálogo (registro) | Provedor SMS + abstração de envio |

---

## 7.1 — Login & Cadastro com Google (Microsoft bloqueado)

- **Objetivo:** habilitar o primeiro Identity Provider real (Google).
- **Entregue (Google):**
  - OAuth app Google com authorized redirect
    `/realms/CRM/broker/google/endpoint` (registrado no Cloud Console).
  - IdP `google` configurado no Keycloak realm CRM (client id/secret, `useJwksUrl`,
    `syncMode=IMPORT`, `trustEmail`, scopes `openid profile email`) + 4 mappers
    (username/email/first name/last name).
  - `AUTH_GATEWAY_ENABLED_PROVIDERS=google` habilitado apenas no serviço auth-service
    do compose de produção.
  - **Deploy controlado:** backup → sincronização do código 7.0 (a VPS estava ~24 commits
    atrás) → rebuild auth-service/frontend → `up -d` → regressão 6.6–6.9 + cadeia curl
    até a página de sign-in do Google (Google aceita Client ID/redirect URI).
  - **Cadastro via IdP:** `firstBrokerLoginFlow` padrão do Keycloak (cria usuário novo no
    primeiro login; e-mail duplicado é auto-linked — `duplicateEmailsAllowed=false`).
- **Pendências da 7.1:**
  - **E2E interativo do usuário** (Google bloqueia automação): login real em
    `https://srv1348261.hstgr.cloud/login` até `/dashboard` com `/auth/me` 200.
  - **Microsoft/Outlook ⛔ bloqueado** — sem app registration no Microsoft Entra; o
    `enabled-providers` contém apenas `google` e `authorize?provider=microsoft` responde
    `400 PROVIDER_NOT_AVAILABLE`. Reabrir quando as credenciais Entra existirem.
  - Account linking visual (7.4).
- **Critérios de aceite:** login com conta Google termina em `/dashboard` com `/auth/me`
  200; e-mail+senha continua funcional; botões da tela de login habilitados apenas para
  provedores ativos.
- **Saída:** `sprints/7.1/REPORT.md` (status: em validação final — E2E pendente).

## 7.2 — Apple (Sign in with Apple)

- **Objetivo:** habilitar Apple/iCloud como IdP.
- **Tarefas:**
  - Adquirir Apple Developer Program (conta paga, pré-requisito) e criar Services ID + Key +
    Private Key para "Sign in with Apple".
  - Configurar IdP `apple` no Keycloak (`apple` já está no catálogo e no registro).
  - Lidar com a peculiaridade da Apple: e-mail privado (Relay) e autorização apenas em
    dispositivos/WebView — definir política de `email` privado × e-mail real do usuário.
  - Deploy controlado + regressão.
- **Critérios de aceite:** login com Apple (Safari/desktop) termina em `/dashboard`; e-mail
  de relay não duplica contas (link por `sub`).
- **Saída:** `sprints/7.2/REPORT.md`.

## 7.3 — Telefone / OTP

- **Objetivo:** adicionar autenticação por telefone com código OTP via Identity Broker.
- **Tarefas:**
  - Definir a **abstração de envio de SMS** (porta `OtpSender` + adapters: primeiro provedor
    real, depois mock local) — hoje `phone` existe apenas no catálogo.
  - Fluxo no Keycloak: IdP/flow custom para telefone+OTP ou fluxo de autenticação dedicado
    (SPI), sempre atrás do Access Gateway.
  - Políticas de segurança OTP: expiração, tentativas, cooldown, rate limit por telefone/IP
    (reutilizar o rate limiting 6.6–6.8).
  - Nenhum OTP em log; nunca logar o código.
- **Critérios de aceite:** login por telefone com OTP válido termina em `/dashboard`; OTP
  inválido/vencido rejeitado; rate limit ativo.
- **Saída:** `sprints/7.3/REPORT.md`.

## 7.4 — Account Linking

- **Objetivo:** resolver contas duplicadas e vincular identidades de provedores diferentes.
- **Tarefas:**
  - Fluxo de "já tenho conta" (pedir login por senha para vincular à conta existente com o
    mesmo e-mail) no Keycloak broker flow.
  - Política de e-mail duplicado com IdP (link automático seguro vs. confirmação manual).
  - Perfil do usuário consolidado no crm-backend: `external_id`/`idp` por usuário, mapeamento
    de e-mail privado (Apple relay).
  - Auditoria de link/deslink (audit log 6.x).
- **Critérios de aceite:** um usuário pode entrar por qualquer provedor vinculado sem criar
  duplicidade; deslink seguro.
- **Saída:** `sprints/7.4/REPORT.md`.

## 7.5 — Recuperação de conta e segurança da identidade

- **Objetivo:** recuperação de acesso e endurecimento da conta.
- **Tarefas:**
  - Política de recuperação quando o provedor externo é perdido (e-mail/senha como fallback).
  - Revogação de sessões por admin, logout global (aproveitar `end_session_endpoint`).
  - Revisar `bruteForceProtected`, email verification e políticas de senha do realm CRM.
  - MFA nativo do Keycloak (TOTP) como camada opcional — decisão de produto.
- **Critérios de aceite:** recuperação funcional sem criar backdoor; sessões revogáveis.
- **Saída:** `sprints/7.5/REPORT.md`.

## 7.6 — UX / Perfil de identidade

- **Objetivo:** polir a experiência de identidade.
- **Tarefas:**
  - Logo definitivo no `LoginBrand.logoSrc` (SVG/PNG oficial) e identidade visual da tela de
    login.
  - Página de perfil mostrando provedores vinculados e permissão de vincular/desvincular.
  - Estados de erro UX para provedor indisponível/falha (botões `error`, tela de retorno do
    Keycloak).
  - Testes E2E de todos os caminhos de login.
- **Critérios de aceite:** tela de login com identidade final; perfil gerencia provedores.
- **Saída:** `sprints/7.6/REPORT.md`.

## 7.7 — Final Security Review

- **Objetivo:** auditoria final de identidade antes de seguir para o SaaS (Sprint 7+).
- **Tarefas:**
  - Revisão de segurança de todos os IdPs habilitados, scopes mínimos, consentimento.
  - Rotação de secrets de provedores e Keycloak client; Docker secrets (pendência 6.10).
  - Regressão completa (6.1–6.10 + 7.x) na VPS + teste de performance/rate limit sob carga.
  - Fechamento: atualização do `SPRINT_INDEX.md` e marcação da etapa Identidade como concluída.
- **Critérios de aceite:** nenhuma exposição nova; suítes verdes; documentação final.
- **Saída:** `sprints/7.7/REPORT.md`.

---

## Dependências externas (necessárias para 7.1–7.3)

- Google Cloud Console (OAuth 2.0 Client ID) — **7.1** — ✅ consumido (Google habilitado).
- Microsoft Entra (app registration) — **7.1** — ⏳ pendente (Microsoft bloqueado).
- Apple Developer Program (conta paga + Services ID/Key) — **7.2**.
- Provedor de SMS — **7.3**.
- Nenhuma credencial será inventada; a sprint 7.0 não consome nenhuma delas.
