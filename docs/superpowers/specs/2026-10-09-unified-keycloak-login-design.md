# Login unificado no Keycloak — design

Data: 2026-10-09 · Branch: `feat/unified-keycloak-login`

## Objetivo

Uma única tela de login. Fluxo do usuário:

```
Landing (/)  --"Entrar"-->  /login (CRM, só redireciona)  -->  Keycloak (tema crm-login)  -->  /crm
```

A tela do Keycloak passa a ter a mesma aparência da landing (fundo preto, vortex animado,
moldura de linhas finas, header "CRM OMNICHANNEL"). O login por telefone/OTP é removido do
sistema inteiro.

## 1. Fluxo

- `/login` (`frontend-refine/src/app/(auth)/login/page.tsx`) deixa de renderizar formulário/lista
  de provedores. Ao montar, chama `loginWithGateway(redirect)` (fluxo OIDC + PKCE já existente,
  sem `kc_idp_hint`), preservando `?redirect=`. Mostra apenas um estado de carregamento
  ("Redirecionando para o login seguro…") com o visual escuro da landing, para não piscar outra tela.
- Links da landing continuam apontando para `ROUTES.LOGIN`.
- Logout continua voltando para a landing.
- Google: o botão aparece na própria tela do Keycloak (seção social do tema, IdP já configurado no realm).
  `ProviderList`, `IdentityProviderButton` e `useIdentityProviders` saem do frontend se não tiverem
  outro consumidor (verificar `link-account` antes de remover).

## 2. Tema Keycloak `crm-login`

Arquivos em `infrastructure/keycloak/themes/crm-login/login/`:

- `crm-template.ftl` — reescrito: `<body>` preto, `<canvas id="crm-vortex">` absoluto em tela cheia,
  moldura (`inset 12px / 24px` ≥ sm), header com "CRM OMNICHANNEL", conteúdo central.
- `login.ftl` — card central translúcido (borda `white/10`, fundo `black/40`, blur): título
  "Entrar", e-mail, senha (com toggle), "Esqueci a senha", botão primário branco arredondado em
  caixa alta (igual ao CTA da landing), divisor "ou" e botão social do Google. Mensagens de erro
  do Keycloak no topo do card.
- `resources/css/login.css` — reescrito com os tokens da landing (preto, branco, `white/75`
  para textos secundários, `rounded-full` nos botões, `tracking` largo no header).
- `resources/js/vortex.js` — port em JS puro do `VortexBackground.tsx` (canvas 2D, sem
  dependências), respeitando `prefers-reduced-motion` (desenha um quadro estático).
- `theme.properties` — adicionar `scripts=js/vortex.js`.
- As demais páginas do fluxo (`login-reset-password`, `login-update-password`, erros) herdam o
  template novo automaticamente.

## 3. Remoção do OTP por telefone

Frontend (`frontend-refine/src/features/identity/auth/`):
- remover `PhoneLoginForm.tsx`, `services/phone-otp.service.ts`, tipos/schemas/mutations de OTP,
  `IDENTITY_PROVIDERS.PHONE` em `lib/gateway-auth.ts`, e os testes correspondentes
  (`ProviderList.test.tsx` etc.).

auth-service:
- remover o provedor `phone` de `ConfiguredIdentityProviderCatalog`, `OidcGatewayProperties`,
  `GatewayOidcService` e `application.yml`; ajustar os testes que o citam.

Backend (`com.becommerce.crm.identity`):
- remover `PhoneAuthController`, `PhoneAuthUseCase`, `PhoneAuthService`, `OtpService`, `OtpCode`,
  `OtpCodeRepository(+Impl, Mapper, JpaEntity, SpringData)`, `OtpSender`, `ConsoleOtpSender`,
  `DisabledOtpSender`, `OtpConfig`, `OtpRateLimiter` e seus testes.
- `User` / `UserJpaEntity` / `UserMapper`: remover `phoneVerified` e `phoneVerifiedAt`.
- remover o GUC `app.current_identity_phone` do datasource, se existir.
- conferir referências de OTP em `AuthService`, `AuthUseCase` e `ForgotPasswordRequest` e
  removê-las se forem do fluxo de telefone.
- O OTP de outros contextos (campanhas etc.) que só casou na busca por texto não é tocado.

Banco — nova migration `V090__drop_phone_otp.sql` (migrations antigas intocadas, por checksum):
```sql
DROP POLICY IF EXISTS identity_phone_bootstrap_policy ON users;
DROP POLICY IF EXISTS identity_phone_link_policy ON users;
DROP TABLE IF EXISTS otp_codes;
ALTER TABLE users DROP COLUMN IF EXISTS phone_verified,
                  DROP COLUMN IF EXISTS phone_verified_at;
```
`users.phone` permanece (é dado de contato, não do OTP).

## 4. Testes

- E2E `frontend-refine/e2e/auth.spec.ts`: o helper `login` passa a ir direto para a tela do
  Keycloak, já com os seletores do tema novo (`#username`, `#password`, `#kc-login`).
  Novo smoke: landing → "Entrar" → tela do Keycloak com o título "Entrar".
- Unitário frontend: teste da página `/login` verificando que chama `loginWithGateway` com o `redirect`.
- Backend/auth-service: `mvn verify` e checkstyle verdes após as remoções; a migration é validada
  pelo Testcontainers/Flyway dos testes de integração.

## Fora de escopo

- Mudar o realm ou os IdPs do Keycloak (só tema).
- Telas pré-login fora do fluxo de login (convites, link-account) além do necessário para compilar.
