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
 * Login via UI (nunca via storageState — cada spec autentica e encerra a
 * própria sessão): /login → Gateway OIDC → página de login do Keycloak dev
 * (tema padrão) → cookies `crm_session`/`XSRF-TOKEN` → `/crm`.
 *
 * Encerra com a página autenticada em `http://localhost:3000/crm`.
 */
export async function login(
  page: Page,
  credentials: { email: string; password: string } = E2E_ADMIN,
): Promise<void> {
  await page.goto("/login");
  await expect(page).toHaveURL(/\/login/);
  await page.getByRole("button", { name: "Entrar com e-mail e senha" }).click();

  // Página de login do Keycloak dev (hostname `keycloak`, tema padrão 26.x).
  // O 26.x renderiza o form no próprio /openid-connect/auth (login-actions só
  // aparece após o POST), então esperamos o campo, não a URL.
  await expect(page).toHaveURL(/realms\/CRM\//);
  await expect(page.locator("#username")).toBeVisible();
  await page.locator("#username").fill(credentials.email);
  await page.locator("#password").fill(credentials.password);
  await page.locator("#kc-login").click();

  // Após o callback do gateway, a sessão existe e o destino (redirect=/crm) é
  // liberado. Timeout maior cobre a primeira chamada do JWKS + resolução.
  await page.waitForURL(/\/crm/, { timeout: 30_000 });
}