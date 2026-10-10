# Login unificado no Keycloak — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Landing → `/login` (só redireciona) → tela do Keycloak com o visual da landing → `/crm`; login por telefone/OTP removido do sistema.

**Architecture:** O `/login` do Next sai do grupo `(auth)` e vira uma página escura que chama `loginWithGateway(redirect)` ao montar. O tema Keycloak `crm-login` é reescrito (FTL + CSS + `vortex.js`). O OTP é apagado do frontend, do auth-service e do backend, e a migration `V090` limpa o banco.

**Tech Stack:** Next.js/React/Vitest/Playwright, Spring Boot/Flyway/JUnit, Keycloak 26.3 (FreeMarker).

**Spec:** `docs/superpowers/specs/2026-10-09-unified-keycloak-login-design.md`

## Global Constraints

- As migrations antigas (V024, V026) não são editadas (checksum); só a nova `V090__drop_phone_otp.sql`.
- `users.phone` permanece.
- Checkstyle: linhas com no máximo 200 caracteres.
- Os textos visíveis ao usuário ficam em pt-BR.
- Os seletores que o E2E usa no Keycloak se mantêm: `#username`, `#password`, `#kc-login`.

---

### Task 1: Backend — remover OTP/telefone + V090

**Files:**
- Delete: `backend/src/main/java/com/becommerce/crm/identity/{web/PhoneAuthController,application/port/input/PhoneAuthUseCase,application/service/PhoneAuthService,application/service/OtpService,domain/OtpCode,domain/repository/OtpCodeRepository,application/port/output/OtpSender,infrastructure/config/OtpConfig,infrastructure/rate/OtpRateLimiter,infrastructure/sms/*OtpSender,infrastructure/persistence/{OtpCode*,SpringDataOtpCodeRepository}}.java` e os testes `OtpServiceTest`, `OtpRateLimiterTest`, `PhoneAuthControllerTest`.
- Modify: `User.java`, `UserJpaEntity.java`, `UserMapper.java` (remover `phoneVerified`, `phoneVerifiedAt`, `markPhoneVerified`); `TenantContext.java` e `TenantAwareDataSource.java` (remover o GUC `app.current_identity_phone`); properties `otp.*` em `application*.yml`; a rota pública `/api/v1/auth/phone/**` no SecurityConfig, se existir.
- Create: `backend/src/main/resources/db/migration/V090__drop_phone_otp.sql`

- [ ] Apagar os arquivos, remover as referências e rodar `grep -rn "Otp\|PhoneAuth\|identity_phone\|phoneVerified" backend/src` até vir vazio (exceto o OTP de campanhas, que não tem relação).
- [ ] Criar a V090:
```sql
-- V090__drop_phone_otp.sql — remove o login por telefone/OTP (Sprint 7.3/7.4).
DROP POLICY IF EXISTS identity_phone_bootstrap_policy ON users;
DROP POLICY IF EXISTS identity_phone_link_policy ON users;
DROP TABLE IF EXISTS otp_codes;
ALTER TABLE users
    DROP COLUMN IF EXISTS phone_verified,
    DROP COLUMN IF EXISTS phone_verified_at;
```
- [ ] Rodar `mvn -q verify` em `backend/` (com checkstyle). Esperado: BUILD SUCCESS.
- [ ] Commit `refactor(identity): remove phone/OTP login and drop its tables`.

### Task 2: auth-service — remover o provedor `phone`

**Files:**
- Modify: `ConfiguredIdentityProviderCatalog.java` (o catálogo fica só com `google`), `GatewayOidcService.java:201-207` (sai o ramo `PHONE_IS_LOCAL_FLOW`), `OidcGatewayProperties.java` (sai `phoneEnabled`), `IdentityProviderCatalog.java` (javadoc), `application.yml:125-127`.
- Test: `ConfiguredIdentityProviderCatalogTest` (espera `List.of("google")` e sai `shouldMarkPhoneAvailableOnlyWhenPhoneEnabled`), `GatewayOidcServiceTest` (sai `shouldRejectPhoneAsNonOidcProviderEvenWhenEnabled`; o caso `"phone"` desconhecido passa a esperar o mesmo erro de provedor desconhecido).

- [ ] Ajustar os testes primeiro e rodar `mvn -q test` (falha).
- [ ] Remover o código e rodar `mvn -q verify` (verde).
- [ ] Commit `refactor(auth-service): drop the local phone provider`.

### Task 3: Frontend — `/login` vira redirecionamento e o OTP sai

**Files:**
- Move: `src/app/(auth)/login/page.tsx` → `src/app/login/page.tsx` (fora do `AuthLayout`).
- Delete: `PhoneLoginForm.tsx`, `services/phone-otp.service.ts`, `ProviderList.tsx`, `IdentityProviderButton.tsx`, `LoginForm.tsx`, `LoginFormCredentials.tsx`, `hooks/useIdentityProviders.ts`, `types/identity-provider.ts` e seus testes. Antes de apagar cada um, conferir com `grep` que não há outro consumidor.
- Modify: `src/lib/gateway-auth.ts` (sai `PHONE` de `IDENTITY_PROVIDERS`).
- Test: `src/__tests__/app/login/page.test.tsx`

- [ ] Teste:
```tsx
import { render, screen } from "@testing-library/react";
import { vi } from "vitest";
const loginWithGateway = vi.fn();
vi.mock("@/lib/gateway-auth", () => ({ loginWithGateway: (...a: unknown[]) => loginWithGateway(...a) }));
vi.mock("next/navigation", () => ({ useSearchParams: () => new URLSearchParams("redirect=/crm/agenda") }));
import LoginPage from "@/app/login/page";

it("redireciona ao Keycloak preservando o redirect", () => {
  render(<LoginPage />);
  expect(loginWithGateway).toHaveBeenCalledWith("/crm/agenda");
  expect(screen.getByText(/Redirecionando para o login seguro/)).toBeInTheDocument();
});
```
- [ ] Página: componente cliente com `useEffect(() => loginWithGateway(searchParams.get("redirect") ?? undefined), [])`, fundo `bg-black text-white`, `Loader2` e o texto "Redirecionando para o login seguro…", dentro de um `Suspense`.
- [ ] Remover os arquivos de OTP e de provedores, depois rodar `npm run lint && npx tsc --noEmit && npx vitest run` (verde).
- [ ] Commit `feat(frontend): /login redirects straight to Keycloak; remove phone login`.

### Task 4: Tema Keycloak com o visual da landing

**Files (em `infrastructure/keycloak/themes/crm-login/login/`):**
- Rewrite: `crm-template.ftl`, `login.ftl`, `resources/css/login.css`
- Create: `resources/js/vortex.js` (port 1:1 do `VortexBackground.tsx`: mesmas constantes `STRANDS=260`, `SEGMENTS=110`, `SPEED=0.06`, as mesmas curvas, estrelas e vinheta; IIFE que procura `#crm-vortex`)
- Modify: `theme.properties` (`scripts=js/vortex.js`)

- [ ] Template: `<canvas id="crm-vortex">`, moldura `.crm-frame`, header "CRM OMNICHANNEL", `<main>` centralizado; manter as macros `registrationLayout`, as mensagens `message`, `displayInfo` e `socialProvidersNode` do tema base.
- [ ] login.ftl: título "Entrar", e-mail (`#username`), senha (`#password`) com toggle, "Lembrar de mim" (se o realm permitir), "Esqueci a senha", botão `#kc-login` com o texto "Entrar no CRM", e o divisor "ou" com os provedores sociais.
- [ ] Validar subindo `docker compose up -d keycloak` com o tema montado, abrindo `http://localhost:8080/realms/CRM/account` → tela de login; conferir no desktop e em 375px.
- [ ] Commit `feat(keycloak): login theme matches the landing (vortex, frame, CTA)`.

### Task 5: E2E

**Files:** `frontend-refine/e2e/fixtures/auth.ts`, `frontend-refine/e2e/auth.spec.ts`, `frontend-refine/e2e/smoke.spec.ts`

- [ ] O helper `login`: `page.goto("/login")` → espera `/realms\/CRM\//` → preenche `#username`/`#password` → clica `#kc-login`.
- [ ] Teste de credenciais inválidas: sai o clique em "Entrar com e-mail e senha" e o resto continua igual.
- [ ] Smoke: landing → clicar no link "Entrar" → URL `/realms\/CRM\//` e heading "Entrar" visível.
- [ ] Commit `test(e2e): login goes straight to the Keycloak screen`.

### Task 6: PR

- [ ] Push da branch, abrir a PR para `crm-improvements-deploy-phase`, CI verde, merge.
