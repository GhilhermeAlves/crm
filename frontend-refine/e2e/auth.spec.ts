import { test, expect } from "@playwright/test";
import { login, E2E_ADMIN } from "./fixtures/auth";

/**
 * Task 4.2 — Login / Logout (E2E via formulário direto + auth-service backend).
 *
 * Requer a stack completa (postgres, keycloak, backend, auth-service, redis,
 * rabbitmq) — subida pelo job `e2e` do GitHub Actions via `e2e/compose.ci.yml`.
 *
 * Fluxo: /login redireciona direto para a tela de login do Keycloak (tema
 * crm-login) → callback do gateway → `/crm` com cookie `crm_session`.
 *
 * Roda SEMPRE com `workers: 1` (Playwright config) e sem storageState: cada
 * spec autentica via UI e encerra a própria sessão.
 *
 * Post-logout: o middleware redireciona para a landing pública (`/`).
 */

// Timeout por-spec cobre JWKS lazy no 1º acesso + resolução de identidade.
test.describe("Login / Logout (Keycloak E2E)", () => {
  test.describe.configure({ timeout: 120_000 });

  test("login com credenciais válidas redireciona para o CRM autenticado", async ({
    page,
  }) => {
    await login(page);

    await expect(page).toHaveURL(/\/crm/, { timeout: 30_000 });
    // Greeting da dashboard: "Bom dia/Boa tarde/Boa noite, Admin!"
    await expect(page.getByText(/Admin!/)).toBeVisible();
    // Crm_session existe
    const cookies = await page.context().cookies();
    expect(cookies.find((c) => c.name === "crm_session")).toBeTruthy();
  });

  test("login com credenciais inválidas mostra erro e não autentica", async ({
    page,
  }) => {
    await page.goto("/login");

    // Redirecionamento para Keycloak
    await expect(page).toHaveURL(/realms\/CRM\//);
    await expect(page.locator("#username")).toBeVisible();

    // Preenche com credenciais inválidas
    await page.locator("#username").fill("nao-existe@crm.local");
    await page.locator("#password").fill("senha-incorreta");
    await page.locator("#kc-login").click();

    // Keycloak exibe erro e permanece na página de login
    await expect(
      page.getByText(/Invalid username or password/i),
    ).toBeVisible({ timeout: 15_000 });
    await expect(page).toHaveURL(/realms\/CRM\/login-actions/);

    // crm_session NÃO foi criado
    const cookies = await page.context().cookies();
    expect(cookies.find((c) => c.name === "crm_session")).toBeUndefined();
  });

  test("logout encerra a sessão e bloqueia acesso protegido", async ({
    page,
  }) => {
    await login(page);
    await expect(page).toHaveURL(/\/crm/);

    // Abre o UserMenu (botão com o nome do usuário no header) → clica "Sair"
    await page.getByRole("button", { name: /Admin E2E/ }).click();
    await page.getByRole("menuitem", { name: "Sair" }).click();

    // Redirecionamento para landing pública após logout
    await expect(
      page.getByRole("heading", { name: /Atendimento, agenda e vendas/ }),
    ).toBeVisible({ timeout: 30_000 });
    await expect(page.getByRole("link", { name: "Entrar", exact: true })).toBeVisible();

    // Rota protegida redireciona para /login (middleware, sem sessão), que segue
    // direto para a tela de login do Keycloak.
    await page.goto("/crm");
    await expect(page).toHaveURL(/realms\/CRM\//, { timeout: 15_000 });

    // cookie limpo
    const cookies = await page.context().cookies();
    expect(cookies.find((c) => c.name === "crm_session")).toBeUndefined();
  });
});