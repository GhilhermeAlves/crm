import { test, expect } from "@playwright/test";
import { login, E2E_ADMIN } from "./fixtures/auth";

/**
 * Task 4.2 — Login / Logout (E2E via Keycloak real no CI).
 *
 * Requer a stack completa (postgres, keycloak, backend, auth-service, redis,
 * rabbitmq) — subida pelo job `e2e` do GitHub Actions via `e2e/compose.ci.yml`.
 *
 * Roda SEMPRE com `workers: 1` (Playwright config) e sem storageState: cada
 * spec autentica via UI e encerra a própria sessão. O login usa os seletores
 * estáveis do tema padrão do Keycloak 26.x (IDs `#username`, `#password`,
 * `#kc-login`) — sem depender de tradução/role de botão do Keycloak.
 *
 * Post-logout: o RedirectUriValidator (auth-service) usa `defaultRedirect="/"`,
 * portanto o Keycloak redireciona para a landing pública (`/`), não para `/login`.
 */

// Timeout por-spec cobre JWKS lazy no 1º acesso + resolução de identidade.
test.describe("Login / Logout (Keycloak E2E)", () => {
  test.describe.configure({ timeout: 120_000 });

  test("login com credenciais válidas redireciona para o CRM autenticado", async ({
    page,
  }) => {
    await login(page);

    await expect(page).toHaveURL(/\/crm/, { timeout: 30_000 });
    // PageTitle "CRM" (h1) confirma que a página autenticada renderizou
    await expect(page.getByRole("heading", { name: "CRM", exact: true })).toBeVisible();
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
    await page.getByRole("button", { name: "Entrar com e-mail e senha" }).click();
    // Keycloak 26 renderiza o form no próprio /openid-connect/auth.
    await expect(page).toHaveURL(/realms\/CRM\//);
    await expect(page.locator("#username")).toBeVisible();

    await page.locator("#username").fill("nao-existe@crm.local");
    await page.locator("#password").fill("senha-incorreta");
    await page.locator("#kc-login").click();

    // Keycloak exibe erro e permanece na própria página (não redireciona ao CRM).
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

    // end_session_endpoint do Keycloak → post_logout_redirect_uri=/ (landing pública).
    // O redirect passa pelo keycloak e volta com 302, sem cookie.
    await expect(
      page.getByRole("heading", { name: "CRM SaaS Omnichannel" }),
    ).toBeVisible({ timeout: 30_000 });
    await expect(page.getByRole("link", { name: "Entrar", exact: true })).toBeVisible();

    // Rota protegida agora redireciona para /login (middleware, sem sessão).
    await page.goto("/crm");
    await expect(page).toHaveURL(/\/login/, { timeout: 15_000 });

    // cookie limpo
    const cookies = await page.context().cookies();
    expect(cookies.find((c) => c.name === "crm_session")).toBeUndefined();
  });
});