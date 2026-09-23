import { test, expect, type BrowserContext, type Page } from "@playwright/test";
import { login } from "./fixtures/auth";
import {
  cleanupBusiness,
  escapeRegExp,
  newRunId,
  seedContacts,
  seedLeads,
  type SeedContact,
} from "./fixtures/seed";

/**
 * Task 4.3 — Leads (seed E2E via API + fluxo completo pela UI).
 *
 * Requer a stack E2E completa e um browser context autenticado via
 * `login()` (fixtures/auth.ts). O seed cria contatos e leads na empresa
 * DEFAULT do usuário E2E usando `page.request` (mesmo relay /api/** do app).
 *
 * O backend de leads CONSEGUE filtrar por status/origem/classificação, mas
 * NÃO implementa busca por texto (`search` é client-side no frontend). Por
 * isso as asserções de quantidade são ancoradas no `runId` único do seed
 * (presente nos nomes/e-mails dos contatos) — `locator(...).filter({ hasText })`
 * — e não no total de linhas da tabela.
 *
 * Sobre retries do CI: cada teste cria dados com identificador único por
 * tentativa, tornando a spec idempotente em reruns.
 */
test.describe("Leads — seed E2E (Task 4.3)", () => {
  test.describe.configure({ timeout: 120_000 });

  let context: BrowserContext;
  let page: Page;
  let runId: string;
  let contacts: SeedContact[] = [];

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext({ baseURL: "http://localhost:3000" });
    page = await context.newPage();
    await login(page);
    runId = newRunId();
    contacts = await seedContacts(page, runId, 5);
    await seedLeads(page, runId, contacts);
  });

  test.afterAll(async () => {
    await cleanupBusiness(page, { contacts, pipeline: null });
    await context.close();
  });

  // Linhas da tabela cujo texto contém o runId (identifica todos os nossos seeds).
  function seedRows() {
    return page.locator("tbody tr").filter({ hasText: runId });
  }

  test("lista renderiza os 3 leads do seed (contagem de linhas)", async () => {
    await page.goto("/leads");
    await expect(seedRows()).toHaveCount(3, { timeout: 15_000 });
  });

  test("cria um lead pelo formulário (toast + linha nova na lista)", async () => {
    const tag = `cria${Date.now().toString(36)}`;
    // Contato novo por tentativa => e-mails únicos (idempotente em retry do CI).
    const extra = (await seedContacts(page, runId, 1, tag))[0];
    contacts.push(extra);

    await page.goto("/leads/new");
    await page.locator("#contactId").click();
    await page
      .getByRole("option", { name: new RegExp(escapeRegExp(extra.email)) })
      .click();
    await page.getByRole("button", { name: "Criar Lead" }).click();

    await expect(page.getByText("Lead criado com sucesso")).toBeVisible();
    await expect(page).toHaveURL(/\/leads/);

    // O lead do contato criado acima aparece na tabela (linha única por tag).
    await expect(page.locator("tbody tr").filter({ hasText: tag })).toHaveCount(1, {
      timeout: 15_000,
    });
  });

  test("filtra por status Qualificado (filtro server-side)", async () => {
    await page.goto("/leads");
    await expect(seedRows()).toHaveCount(3, { timeout: 15_000 });

    await page.getByRole("button", { name: "Filtros" }).click();
    await page.getByRole("combobox", { name: "Status" }).click();
    await page.getByRole("option", { name: "Qualificado" }).click();

    // Apenas o lead QUALIFIED do seed passa no filtro (criado pela spec é NEW).
    await expect(seedRows()).toHaveCount(1, { timeout: 15_000 });
  });
});