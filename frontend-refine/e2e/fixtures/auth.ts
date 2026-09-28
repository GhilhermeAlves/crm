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
 * Login via UI (formulário direto de email/senha): /login → preenchimento de
 * credenciais → POST /auth/login (via gateway) → cookies `crm_session` →
 * redirecionamento automático para `/dashboard`.
 *
 * Novo fluxo (Sprint 7.0): sem navegação para Keycloak no browser; login
 * seguro feito server-side via gateway/auth-service.
 *
 * Encerra com a página autenticada em `http://localhost:3000/dashboard`.
 */
export async function login(
  page: Page,
  credentials: { email: string; password: string } = E2E_ADMIN,
): Promise<void> {
  await page.goto("/login");
  await expect(page).toHaveURL(/\/login/);

  // Preenche formulário direto de email/senha (novo design Sprint 7.0)
  await page.locator('input[type="email"]').fill(credentials.email);
  await page.locator('input[type="password"]').fill(credentials.password);
  await page.getByRole("button", { name: "Entrar com e-mail e senha" }).click();

  // Aguarda redirecionamento automático após login bem-sucedido
  // Timeout maior cobre a primeira chamada do JWKS + resolução de identidade
  await page.waitForURL(/\/dashboard/, { timeout: 30_000 });
}