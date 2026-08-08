# Identity Provider Roadmap (7.0 → 7.4)

> Roteiro da evolução de identidade do CRM sobre o Access Gateway (auth-service) + Keycloak
> Identity Brokering. **Decisão de escopo (2026-08-06): o único Identity Provider externo da
> Sprint 7 é o Google.** Microsoft/Microsoft Entra ID, Apple/Sign in with Apple/iCloud e
> qualquer outro IdP externo estão **FORA DO ESCOPO ATUAL** — não há sprint prevista para
> eles. A estrutura oficial da Sprint 7 é 7.0 → 7.4.

## Princípios invariantes

- **Não-regressão:** `/login` → Keycloak → `/auth/callback` → `/dashboard` nunca pode quebrar.
- **Sem autenticação paralela:** todo fluxo passa pelo Access Gateway; o browser nunca detém
  token de provedor.
- **Sem credencial inventada:** provedor só é habilitado com credencial real configurada no
  Keycloak (configuração = `AUTH_GATEWAY_ENABLED_PROVIDERS` + IdP no realm CRM).
- **Sem log de secret/OTP/senha.**
- **Meta/Facebook fora de escopo.**
- **Escopo de IdP externo limitado ao Google** — Microsoft e Apple ficam apenas como registro
  "preparado" no catálogo de código, sem sprint nem credencial; não habilitar.

## Matriz de provedores

| Provedor | Alias | Sprint | Status | Prerequisito externo |
|----------|-------|--------|-------------|----------------------|
| Google | `google` | 7.1 | ✅ **Habilitado e validado em produção** (E2E até `/dashboard`) | OAuth Client ID/Secret (Google Cloud Console) — ✅ consumido |
| Microsoft/Outlook | `microsoft` | — | ⛔ **FORA DO ESCOPO ATUAL** (registro no catálogo apenas; não habilitado) | App registration (Microsoft Entra) — não se aplica |
| Apple/iCloud | `apple` | — | ⛔ **FORA DO ESCOPO ATUAL** (registro no catálogo apenas; não habilitado) | Apple Developer Program (Sign in with Apple) — não se aplica |
| Telefone/OTP | `phone` | 7.3 | ✅ **Verificação OTP implementada e validada em produção** (endpoints `/api/v1/auth/phone/*`; portal OTP `OtpSender`; RLS por telefone) | Provedor SMS real (integração pendente — `DisabledOtpSender` em prod) |

---

## 7.0 — Identity Providers (Google)

- **Status:** ✅ Concluída (2026-08-05).
- **Objetivo:** fundação de identidade — catálogo de provedores, `kc_idp_hint` e nova tela
  de login (sem habilitar IdP real).
- **Detalhes técnicos:** ver `sprints/7.0/REPORT.md`.

## 7.1 — Login/Cadastro com Google

- **Status:** ✅ **CONCLUÍDA** (2026-08-06) — histórico preservado em `sprints/7.1/REPORT.md`.
- **Objetivo:** habilitar o primeiro (e único) Identity Provider externo real — Google.
- **Entregue:**
  - OAuth app Google com authorized redirect
    `/realms/CRM/broker/google/endpoint` (registrado no Cloud Console).
  - IdP `google` configurado no Keycloak realm CRM (client id/secret, `useJwksUrl`,
    `syncMode=IMPORT`, `trustEmail`, scopes `openid profile email`) + 3 mappers
    (email/first name/last name); username via fallback padrão do broker (e-mail).
  - Correção do `UsernameTemplateMapper` (Keycloak 26.3.5 exige `template`) — mapper
    "username" removido (ver "Problemas" no report da 7.1).
  - `AUTH_GATEWAY_ENABLED_PROVIDERS=google` habilitado apenas no serviço auth-service
    do compose de produção.
  - Deploy controlado na VPS (backup → sincronização do código 7.0 → rebuild → `up -d`
    → regressão 6.6–6.9 + cadeia curl até o sign-in do Google).
  - Testes de regressão; E2E Google validado em produção (login Google real →
    `/dashboard`); backend 245/245, frontend 49/49, lint/typecheck/build PASS.
  - Documentação + commit `f0b2524`.
- **Pendência funcional (para a 7.2):** o **auto-provisionamento** completo da identidade
  Google no banco CRM **ainda não está implementado**. O login Google está concluído
  (Keycloak cria/importa a identidade → OIDC resolvida), mas a conta CRM correspondente não
  é criada automaticamente: na validação da 7.1 foi necessário provisionar manualmente
  `users` + `user_roles` para o usuário do E2E. A 7.2 deve tratar o caso de uma identidade
  Google já autenticada que ainda não possui conta CRM correspondente.

## 7.2 — Account Linking

- **Status:** ⏳ Pendente.
- **Objetivo:** permitir a **vinculação segura** entre a **conta local CRM** e a
  **identidade Google** (`conta local CRM ↕ identidade Google`).
- **Tarefas:**
  - Resolver o caso da identidade Google já autenticada que ainda não possui conta CRM
    correspondente (pendência funcional herdada da 7.1) — vinculação/provisão segura.
  - **Não fazer vinculação automática apenas porque o e-mail é igual** — definir a
    estratégia segura de vinculação na implementação (fluxo de confirmação, prova de
    posse, auditoria).
  - Perfil do usuário consolidado no crm-backend (vínculo identidade ↔ usuário CRM).
  - Auditoria de link/deslink (audit log 6.x).
- **Critérios de aceite:** um usuário pode entrar por conta local e por identidade Google
  vinculada sem criar duplicidade; deslink seguro; nenhum link automático baseado apenas no
  e-mail.
- **Saída:** `sprints/7.2/REPORT.md`.

## 7.3 — Telefone / OTP

- **Status:** ✅ **CONCLUÍDA** (2026-08-08) — histórico preservado em `sprints/7.3/REPORT.md`.
- **Objetivo:** autenticação/verificação por **Telefone → OTP → Identidade CRM**.
- **Entregue:**
  - Porta `OtpSender` + adapters `ConsoleOtpSender` (dev/test) e `DisabledOtpSender`
    (produção — nunca loga o código, invariante 7.3).
  - `OtpService` (hash SHA-256+salt, TTL 5min, 3 tentativas, cooldown 60s, invalidação por
    uso/expiração/excesso) + bean explícito em `OtpConfig`.
  - Tabela `otp_codes` (V024) e colunas `phone`/`phone_verified` em `users`.
  - Endpoints públicos `/api/v1/auth/phone/{send-otp,verify-otp,can-resend}`.
  - **RLS por telefone (V026):** sob RLS FORCE, a conta só fica visível por telefone via
    GUC `app.current_identity_phone`, definido APÓS a validação do OTP (prova de posse) —
    padrão V025 aplicado ao telefone.
  - E2E em produção: verify-otp → `success:true`, `phone_verified=t`, OTP consumido.
  - Backend 105/105 tests PASS.
- **Pendência funcional:** provedor SMS real em produção (hoje `DisabledOtpSender` descarta
  o código); login completo por telefone (criar conta) fica para o backlog.

## 7.4 — Recuperação de conta e segurança da identidade

- **Status:** ⏳ Pendente.
- **Objetivo:** recuperação de conta e hardening da identidade.
- **Tarefas:**
  - Recuperação de conta utilizando os mecanismos definidos na Sprint 7.3 (OTP como
    verificação/fallback) e e-mail como fallback.
  - Proteção contra abuso: revogação de sessões por admin, logout global
    (`end_session_endpoint`), revisão de `bruteForceProtected`, verificação de e-mail e
    políticas de senha do realm CRM.
  - Hardening dos fluxos de identidade (MFA TOTP nativo do Keycloak como camada opcional —
    decisão de produto).
  - Auditoria dos fluxos de recuperação e de vinculação.
- **Critérios de aceite:** recuperação funcional sem criar backdoor; sessões revogáveis;
  fluxos de recuperação e vinculação auditados.
- **Saída:** `sprints/7.4/REPORT.md`.

---

## Fora do escopo atual

- **Microsoft / Microsoft Entra ID** — ⛔ fora do escopo da Sprint 7. O catálogo de código
  mantém o registro `microsoft` como "preparado", mas **não há sprint** prevista para
  habilitá-lo; nenhuma credencial Entra será requisitada.
- **Apple / Sign in with Apple / iCloud** — ⛔ fora do escopo da Sprint 7. O catálogo de
  código mantém o registro `apple` como "preparado", mas **não há sprint** prevista para
  habilitá-lo; nenhum Apple Developer Program será requisitado.
- **Meta/Facebook e qualquer outro IdP externo não explicitamente definido** — fora de escopo.
- Nota histórica: a versão anterior deste roadmap previa as sprints 7.2 (Apple), 7.5
  (Recuperação), 7.6 (UX/Perfil) e 7.7 (Final Security Review). A partir da decisão de
  2026-08-06 a estrutura oficial é 7.0 → 7.4; itens gerais (logo definitivo para
  `LoginBrand.logoSrc`, UX/perfil de identidade e revisão de segurança final) ficam como
  backlog pós-7.4, sem sprint numerada, e não tratam de Apple/Microsoft.

---

## Dependências externas

- Google Cloud Console (OAuth 2.0 Client ID) — **7.1** — ✅ consumido (Google habilitado).
- Provedor de SMS — **7.3** — 🔶 abstração de envio implementada; integração de provedor real pendente (em produção o `DisabledOtpSender` descarta o código).
- Microsoft Entra (app registration) — **fora do escopo atual** (não é dependência ativa).
- Apple Developer Program — **fora do escopo atual** (não é dependência ativa).
- Nenhuma credencial será inventada; a sprint 7.0 não consome nenhuma delas.
