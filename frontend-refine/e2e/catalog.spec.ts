import { test, expect } from "@playwright/test";
import { login } from "./fixtures/auth";

/**
 * Catálogo de produtos/serviços (V078): cria um item pela UI, encontra pela busca
 * server-side e desativa. Nome único por tentativa (idempotente em retry do CI).
 */
test.describe("Catálogo", () => {
  test.describe.configure({ timeout: 120_000 });

  test("cria, busca e desativa um item", async ({ page }) => {
    const name = `E2E Plano ${Date.now().toString(36)}`;
    await login(page);

    await page.goto("/catalog");
    await expect(page.getByRole("heading", { name: "Catálogo", level: 1 })).toBeVisible();

    await page.getByRole("button", { name: "Novo item" }).click();
    await page.getByLabel("Nome").fill(name);
    await page.getByLabel("Categoria").fill("Planos");
    await page.getByLabel("Preço (R$)").fill("199,90");
    await page.getByRole("button", { name: "Salvar" }).click();
    await expect(page.getByText("Item criado")).toBeVisible();

    await page.getByLabel("Buscar no catálogo").fill(name);
    const row = page.locator("tbody tr").filter({ hasText: name });
    await expect(row).toHaveCount(1, { timeout: 15_000 });
    await expect(row).toContainText("R$ 199,90");
    await expect(row).toContainText("Ativo");

    await row.getByRole("button", { name: "Desativar" }).click();
    await expect(row).toContainText("Inativo", { timeout: 15_000 });
  });
});
