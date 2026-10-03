import { type Page, expect } from "@playwright/test";
import users from "./users.json";

/**
 * Fixtures de autenticação E2E (Task 4.2).
 *
 * O usuário vive no realm dev do Keycloak (crm-realm-dev.json) e na API do CRM
 * (seed e2e/seed/user.sql). As credenciais são as do fixture `users.json`
 * (fonte única, versionada e dev-only); `E2E_ADMIN_EMAIL`/`E2E_ADMIN_PASSWORD`
 * são overrides OPCIONAIS para rodar localmente contra outro realm. O job `e2e`
 * do CI NÃO exporta essas vars — usa o fixture.
 */
export const E2E_ADMIN = {
  email: process.env.E2E_ADMIN_EMAIL ?? users.admin.email,
  password: process.env.E2E_ADMIN_PASSWORD ?? users.admin.password,
  name: users.admin.name,
};

/**
 * Login via UI (Sprint 7.0): /login → clique em "Entrar com e-mail e senha" →
 * redirecionamento para gateway (/auth/authorize) → Keycloak (formulário de
 * login padrão) → callback do gateway → cookies `crm_session` + `/dashboard`.
 *
 * O fluxo OIDC mantém os tokens no servidor (auth-service); browser tem apenas
 * cookie HttpOnly de sessão.
 *
 * Encerra com a página autenticada em `http://localhost:3000/dashboard`.
 */
export async function login(
  page: Page,
  credentials: { email: string; password: string } = E2E_ADMIN,
): Promise<void> {
  await page.goto("/login");
  await expect(page).toHaveURL(/\/login/);

  // Clica em "Entrar com e-mail e senha" → redireciona para gateway
  await page.getByRole("button", { name: "Entrar com e-mail e senha" }).click();

  // Aguarda redirecionamento para Keycloak (formulário de login padrão 26.x)
  // Keycloak renderiza o form em /openid-connect/auth (tema padrão)
  await expect(page).toHaveURL(/realms\/CRM\//);
  await expect(page.locator("#username")).toBeVisible();

  // Preenche formulário do Keycloak
  await page.locator("#username").fill(credentials.email);
  await page.locator("#password").fill(credentials.password);
  await page.locator("#kc-login").click();

  // Aguarda callback do gateway e redirecionamento para dashboard
  // Timeout maior cobre JWKS lazy + resolução de identidade
  await page.waitForURL(/\/dashboard/, { timeout: 30_000 });
}