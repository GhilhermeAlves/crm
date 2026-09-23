import { test, expect, type BrowserContext, type Page } from "@playwright/test";
import { login } from "./fixtures/auth";
import {
  deleteRole,
  listRoles,
  newRunId,
  purgeE2ERoles,
} from "./fixtures/seed";

/**
 * Task 4.3 — Perfis de acesso (roles & permissões, seed E2E via API).
 *
 * A empresa default tem os papéis canônicos (ADMIN, MANAGER, AGENT, VIEWER)
 * criados pelo RoleSeedService do backend. A spec cria um perfil custom
 * "E2E_*" pela UI (requer `role:manage`, presente no Admin E2E), atribui a
 * permissão `dashboard:view` e valida o contador do PermissionMatrix.
 *
 * O cleanup remove perfis "E2E_*" via API em beforeAll/afterAll (delete de
 * role não-sistema funciona por API — RoleService.deleteRole).
 */
test.describe("Perfis de acesso — seed E2E (Task 4.3)", () => {
  test.describe.configure({ timeout: 120_000 });

  let context: BrowserContext;
  let page: Page;
  let runId: string;
  const createdRoleIds: string[] = [];

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext({ baseURL: "http://localhost:3000" });
    page = await context.newPage();
    await login(page);
    runId = newRunId();
    await purgeE2ERoles(page);
  });

  test.afterAll(async () => {
    for (const id of createdRoleIds) {
      await deleteRole(page, id);
    }
    await purgeE2ERoles(page);
    await context.close();
  });

  test("lista os perfis canônicos da empresa", async () => {
    await page.goto("/settings/roles");
    await expect(page.getByRole("heading", { name: "Perfis de acesso" })).toBeVisible();

    // RoleBadge renderiza o nome (ADMIN/MANAGER ...). Pode aparecer também no
    // heading do perfil ativo; .first() cobre.
    await expect(page.getByText("ADMIN", { exact: true }).first()).toBeVisible();
    await expect(page.getByText("MANAGER", { exact: true }).first()).toBeVisible();
  });

  test("cria um perfil custom e atribui uma permissão", async () => {
    await page.goto("/settings/roles");

    // UpperCase (backend normaliza para UPPER_SNAKE e mantém).
    const roleName = `E2E_${runId.toUpperCase()}_VENDAS`;

    await page.getByRole("button", { name: "Criar Perfil" }).click();
    await page.getByLabel("Nome").fill(roleName);
    await page.getByLabel("Descrição (opcional)").fill(`Perfil E2E ${runId}`);
    await page.getByRole("button", { name: "Criar", exact: true }).click();

    await expect(page.getByText("Role criada com sucesso")).toBeVisible();
    // Após criar, a página seleciona o novo perfil (h2 com nome "humanizado").
    await expect(
      page.getByRole("heading", { name: roleName.replace(/_/g, " ") }),
    ).toBeVisible();

    // Captura o id para cleanup (a UI não o expõe).
    const roles = await listRoles(page);
    const created = roles.find((r) => r.name === roleName);
    if (created) {
      createdRoleIds.push(created.id);
    }

    // A role nova começa sem permissões; atribui dashboard:view no matrix.
    await page
      .locator("label")
      .filter({ hasText: "dashboard:view" })
      .getByRole("checkbox")
      .click();
    await expect(page.getByText("Permissão atribuída com sucesso")).toBeVisible();
    await expect(page.getByText("1 selecionada(s)")).toBeVisible({ timeout: 15_000 });
  });
});