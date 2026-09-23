import { test, expect } from "@playwright/test";

/**
 * Smoke de tooling (Task 4.1). Validar que o Playwright + webServer estão
 * funcionando. Usa apenas rotas públicas — não requer a stack de apoio
 * (backend/keycloak/pg/redis/rabbit), que entra em cena nas specs 4.2+.
 */

test("landing page pública renderiza e link de login existe", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "CRM SaaS Omnichannel" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Entrar", exact: true })).toBeVisible();
});

test("página de login renderiza o título Entrar", async ({ page }) => {
  await page.goto("/login");
  await expect(page).toHaveURL(/\/login/);
  await expect(page.getByRole("heading", { name: "Entrar" })).toBeVisible();
});