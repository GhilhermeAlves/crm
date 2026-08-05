# Identity Provider Roadmap (7.0 → 7.7)

> Roteiro da evolução de identidade do CRM sobre o Access Gateway (auth-service) + Keycloak
> Identity Brokering. Sprint 7.0 (concluída) entregou a **fundação**: catálogo de provedores,
> `kc_idp_hint` e a nova tela de login. As sprints seguintes **habilitam** provedores reais,
> exigem credenciais externas e, a partir da 7.1, passam a ter deploy na VPS.

## Princípios invariantes

- **Não-regressão:** `/login` → Keycloak → `/auth/callback` → `/dashboard` nunca pode quebrar.
- **Sem autenticação paralela:** todo fluxo passa pelo Access Gateway; o browser nunca detém
  token de provedor.
- **Sem credencial inventada:** provedor só é habilitado com credencial real configurada no
  Keycloak (configuração = `AUTH_GATEWAY_ENABLED_PROVIDERS` + IdP no realm CRM).
- **Sem log de secret/OTP/senha.**
- **Meta/Facebook fora de escopo.**

## Matriz de provedores

| Provedor | Alias | Sprint | Status 7.0 | Prerequisito externo |
|----------|-------|--------|-------------|----------------------|
| Google | `google` | 7.1 | Preparado (não habilitado) | OAuth Client ID/Secret (Google Cloud Console) |
| Microsoft/Outlook | `microsoft` | 7.1 | Preparado (não habilitado) | App registration (Microsoft Entra) |
| Apple/iCloud | `apple` | 7.2 | Preparado (não habilitado) | Apple Developer Program (Sign in with Apple) |
| Telefone/OTP | `phone` | 7.3 | No catálogo (registro) | Provedor SMS + abstração de envio |

---

## 7.1 — Login & Cadastro com Google e Microsoft

- **Objetivo:** habilitar Google e Microsoft como primeiros Identity Providers reais.
- **Tarefas:**
  - Criar OAuth app Google (authorized redirect = `/realms/CRM/broker/google/endpoint`) e app
    Entra (redirect `/realms/CRM/broker/microsoft/endpoint`).
  - Configurar os IdPs no Keycloak realm CRM (client id/secret, scopes `openid email profile`,
    mapeamento de e-mail/nome) e habilitar `AUTH_GATEWAY_ENABLED_PROVIDERS=google,microsoft`.
  - **Deploy controlado:** backup → rebuild auth-service → `up -d` → regressão 6.6–6.9 +
    login real por e-mail/senha + login real Google/Microsoft.
  - **Account linking básico:** decidir e implementar o mapeamento de e-mail entre IdP e conta
    local (ver 7.4) e a política de e-mail duplicado (`duplicateEmailsAllowed` hoje `false`).
  - Cadastro via IdP: decisão de auto-provisionamento de usuário novo (broker flow) + criação
    da empresa/tenant associada.
- **Critérios de aceite:** login com conta Google e Microsoft terminam em `/dashboard` com
  `/auth/me` 200; e-mail+senha continua funcional; botões da tela de login ficam habilitados
  apenas para os provedores ativos.
- **Saída:** `sprints/7.1/REPORT.md`.

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

- Google Cloud Console (OAuth 2.0 Client ID) — **7.1**.
- Microsoft Entra (app registration) — **7.1**.
- Apple Developer Program (conta paga + Services ID/Key) — **7.2**.
- Provedor de SMS — **7.3**.
- Nenhuma credencial será inventada; a sprint 7.0 não consome nenhuma delas.
