import { test, expect } from "@playwright/test";

/**
 * Smoke: landing pública e a passagem landing → tela de login do Keycloak
 * (requer o Keycloak da stack E2E).
 */

test("landing page pública renderiza e link de login existe", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: /Atendimento, agenda e vendas/ })).toBeVisible();
  await expect(page.getByRole("link", { name: "Entrar", exact: true })).toBeVisible();
});

test("landing → Entrar leva direto à tela de login do Keycloak", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: "Entrar", exact: true }).click();
  await expect(page).toHaveURL(/realms\/CRM\//, { timeout: 15_000 });
  await expect(page.getByRole("heading", { name: "Entrar" })).toBeVisible();
  await expect(page.locator("#kc-login")).toBeVisible();
});