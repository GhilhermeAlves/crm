import { test, expect } from "@playwright/test";
import { login } from "./fixtures/auth";

/**
 * Controle de estoque (V095): cadastra um produto com estoque mínimo no Catálogo,
 * abre /estoque, vê o badge de baixo estoque, registra uma entrada e confere o saldo.
 */
test.describe("Estoque", () => {
  test.describe.configure({ timeout: 120_000 });

  test("registra entrada e atualiza o saldo", async ({ page }) => {
    const name = `E2E Estoque ${Date.now().toString(36)}`;
    await login(page);

    await page.goto("/catalog");
    await expect(page.getByRole("heading", { name: "Catálogo", level: 1 })).toBeVisible();
    await page.getByRole("button", { name: "Novo item" }).click();
    await page.getByLabel("Nome").fill(name);
    await page.getByLabel("Categoria").click();
    await page.getByRole("option", { name: "Material de consumo" }).click();
    await page.getByLabel("Unidade").click();
    await page.getByRole("option", { name: "Caixa" }).click();
    await page.getByLabel("Estoque mínimo").fill("5");
    await page.getByRole("button", { name: "Salvar" }).click();
    await expect(page.getByText("Item criado")).toBeVisible();

    await page.goto("/estoque");
    await expect(page.getByRole("heading", { name: "Estoque", level: 1 })).toBeVisible();

    const row = page.locator("tbody tr").filter({ hasText: name });
    await expect(row).toHaveCount(1, { timeout: 15_000 });
    await expect(row).toContainText("Baixo estoque");

    await row.getByRole("button", { name: "Movimentar" }).click();
    await page.getByLabel("Tipo").click();
    await page.getByRole("option", { name: "Entrada" }).click();
    await page.getByLabel("Quantidade").fill("10");
    await page.getByRole("button", { name: "Registrar" }).click();
    await expect(page.getByText("Movimento registrado")).toBeVisible();

    await expect(row.getByTestId("inventory-quantity")).toHaveText("10", { timeout: 15_000 });
    await expect(row).not.toContainText("Baixo estoque", { timeout: 15_000 });
  });
});