# Sprint 7.3 — Telefone/OTP (autenticação e verificação por telefone)

**Data:** 2026-08-06/08 — **Ambiente:** local + VPS (produção `srv1348261.hstgr.cloud`) — **Status:** ✅ Concluída

## Identificação

- **Sprint:** 7.3
- **Nome:** Telefone/OTP — verificação e autenticação por telefone via código de uso único
- **Responsável:** AI Agent
- **Fase:** Segurança — Access Gateway / Identidade
- **Dependência:** Sprint 7.1 (Google IdP) + Sprints 6.x (Gateway/OIDC/sessão e RLS)

## Objetivo

Habilitar a **verificação de conta por telefone com OTP**: enviar o código por
canal, validar, marcar o telefone como verificado e localizar a conta pelo telefone,
preservando o **RLS FORCE** dos tenants (`users`) e a invariante **nunca logar o OTP em
produção**.

## Escopo

- **Porta de entrega (`OtpSender`):** `ConsoleOtpSender` (`@Profile("dev|test")`, loga o OTP
  mascarado — **apenas dev/test**) e `DisabledOtpSender` (`@Profile("prod")`, descarta o
  código sem logar — invariante 7.3).
- **`OtpService`:** geração numérica de 6 dígitos, hash SHA-256 + salt (armazenado como
  `salt:hash`), TTL 5 min, máx. 3 tentativas, cooldown de reenvio 60s, invalidação por
  uso/expiração/excesso de tentativas; bean explícito em `OtpConfig` (fora do scan de
  domínio).
- **Migração:** `V024__add_phone_otp_tables.sql` (tabela `otp_codes` + colunas `phone`/
  `phone_verified` em `users`) e `V026__identity_phone_bootstrap_rls.sql`.
- **Endpoints (públicos, pré-auth):**
  - `POST /api/v1/auth/phone/send-otp` `{phone}` — envia OTP (cooldown 60s).
  - `POST /api/v1/auth/phone/verify-otp` `{phone, otp}` — valida, marca `phone_verified`,
    retorna `userId/email/phoneVerified`.
  - `POST /api/v1/auth/phone/can-resend` `{phone}` — cooldown.
- **Fora de escopo:** canal SMS real em produção (provedor fica como recomendação), login
  completo por telefone, account linking visual (7.2).

## RLS por telefone — bug real descoberto e corrigido

`verify-otp` valida o OTP e então executa `userRepository.findByPhone(phoneE164)`. Esse
SELECT roda em request **anônimo** (sem JWT/company_id) sob RLS FORCE em `users`; sem
política extra, a linha era invisível:

- E2E em produção: `SET ROLE crm_app; RESET app.current_company_id` → **0 rows**; com
  `SET app.current_company_id=<tenant>` → **1 row**. O `verify-otp` retornava
  `USER_NOT_FOUND` mesmo com a conta existindo.
- **Correção (padrão V024/V025, nunca bypass de RLS):** migration `V026` com duas policies
  gateadas pelo GUC `app.current_identity_phone`:

```sql
CREATE POLICY identity_phone_bootstrap_policy ON users
  FOR SELECT USING (phone = NULLIF(current_setting('app.current_identity_phone', true), ''));
CREATE POLICY identity_phone_link_policy ON users
  FOR UPDATE USING (phone = NULLIF(current_setting('app.current_identity_phone', true), ''))
  WITH CHECK (phone = NULLIF(current_setting('app.current_identity_phone', true), ''));
```

- O GUC é definido pelo `TenantAwareDataSource` a partir de `TenantContext.setIdentityPhone`
  — chamado **após a validação do OTP (prova de posse)** no `PhoneAuthService.verifyOtp`, e
  `RESET` em `finally`. Nunca derivado de input do usuário sem a OTP validada.
- Segurança: a policy só opera na linha cujo telefone é exatamente o GUC informado; o
  chamador já conhece o telefone (não há enumeração adicional). `users` permanece FORCE.

## Evidência E2E em produção (VPS)

Usuário `e2e.otp@crm.local` (phone `+5511999999999`), OTP `123456` injetado como hash
válido em `otp_codes`:

1. `POST https://srv1348261.hstgr.cloud/api/v1/auth/phone/verify-otp` `{phone, otp:"123456"}` →
   **200**:
   ```json
   { "success": true, "userExists": true,
     "userId": "5236fcd1-d9e5-420a-a8f8-43c47c9678f1",
     "email": "e2e.otp@crm.local", "phoneVerified": true }
   ```
2. Persistência: `phone_verified = t` em `users`; OTP marcado como consumido
   (anti-replay).
3. `send-otp` → **200** e cooldown 60s validado em teste de reenvio.
4. Validação do GUC no banco (como `crm_app`): sem GUC 0 rows; com
   `SET app.current_identity_phone='+5511999999999'` → 1 row encontrada.

## Problemas e correções

1. **`findByPhone` retornava `USER_NOT_FOUND` por RLS FORCE em request anônimo** —
   corrigido com GUC + policy V026 (acima).
2. **`findLatestByPhone` quebrava com 2+ OTPs** para o mesmo telefone
   (`IncorrectResultSizeDataAccessException`) — corrigido com `PageRequest.of(0,1)` +
   `findFirst`.
3. **`OtpCode.create` forçava `id=randomUUID()` em conflito com lock otimista** — removido
   (id gerado pelo BD).
4. **Seu `password_hash` do fixture E2E inválido (`\\\`, 3 chars)** quebrava o mapper de
   domínio `Password` (exigia bcrypt ou >=8 chars) — substituído por hash bcrypt real
   ($2b$). Não é bug do domínio; foi dado de teste inconsistente.

## Testes locais

- **Backend:** `mvn test` → **105/105 testes PASS** (regressão completa).
- Novos testes 7.3: `OtpServiceTest` (3): entrega via `OtpSender`, validação
  (correto/incorreto), cooldown de reenvio.

## Pendências / recomendações

- **Provedor SMS real** no lugar do `DisabledOtpSender` em produção.
- **Auto-provisionamento / login completo por telefone** (criar conta por telefone).
- **Rate limiting fino** no `send-otp` (por IP/sessão) e medidas anti-abuso.

## Resultado

STATUS: **CONCLUÍDA**.

- Porta `OtpSender` (`ConsoleOtpSender`/`DisabledOtpSender`), `OtpService`, controller
  `/api/v1/auth/phone/*`, migrações V024/V026 aplicadas na VPS.
- RLS: `users` permanece FORCE; leitura por telefone só via GUC `app.current_identity_phone`
  definido após validação do OTP (prova de posse).
- E2E em produção: `verify-otp` → `success:true`, `phoneVerified:t` persistido, OTP
  consumido; RLS validado (0 sem GUC, 1 com GUC).
- Suítes locais: backend 105/105 PASS.

## Próxima sprint (roadmap)

- **7.4 — Recuperação de conta e segurança da identidade** (depende de 7.2 e 7.3).
- **7.2 — Account Linking** (vinculação local/Google) — concluída em 2026-08-08.