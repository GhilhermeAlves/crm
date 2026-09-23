import { test, expect, type BrowserContext, type Page } from "@playwright/test";
import { login } from "./fixtures/auth";
import {
  cleanupBusiness,
  newRunId,
  purgeE2EPipelines,
  seedContacts,
  seedOpportunity,
  seedPipeline,
  type SeedContact,
  type SeedPipeline,
} from "./fixtures/seed";

/**
 * Task 4.3 — Pipeline / Negociações (seed E2E + fluxos pela UI).
 *
 * O kanban lista pipelines da empresa por created_at DESC, então o pipeline
 * criado pelo seed (mais recente) é sempre `activePipelines[0]` — e o board
 * mostra as 5 etapas padrão criadas junto (Prospecção → Fechamento).
 *
 * `purgeE2EPipelines` no beforeAll remove resíduos "E2E Pipeline" de runs
 * falhas, mantendo o board determinístico também em CI com retry.
 */
test.describe("Pipeline — seed E2E (Task 4.3)", () => {
  test.describe.configure({ timeout: 120_000 });

  let context: BrowserContext;
  let page: Page;
  let runId: string;
  let contact: SeedContact;
  let pipeline: SeedPipeline;
  let seedOppTitle: string;

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext({ baseURL: "http://localhost:3000" });
    page = await context.newPage();
    await login(page);
    runId = newRunId();

    await purgeE2EPipelines(page);
    pipeline = await seedPipeline(page, runId);
    contact = (await seedContacts(page, runId, 1))[0];
    seedOppTitle = `E2E Oportunidade ${runId}`;
    await seedOpportunity(page, pipeline, contact.id, seedOppTitle, 12_500);
  });

  test.afterAll(async () => {
    await cleanupBusiness(page, { contacts: [contact], pipeline });
    await context.close();
  });

  test("kanban renderiza as 5 etapas padrão e a oportunidade do seed", async () => {
    await page.goto("/pipeline");
    await expect(page.getByRole("heading", { name: "Negociações" })).toBeVisible();

    for (const stage of pipeline.stages) {
      await expect(page.getByText(stage.name, { exact: true }).first()).toBeVisible();
    }

    // Card do seed na 1ª etapa (iso: subtitle "Prospecção · 10%").
    await expect(page.getByText(seedOppTitle)).toBeVisible();
    await expect(page.getByText("Prospecção · 10%")).toBeVisible();
  });

  test("cria uma oportunidade pelo modal (toast + card novo)", async () => {
    await page.goto("/pipeline");
    const newTitle = `E2E Nova ${runId}`;

    await page.getByRole("button", { name: "Nova oportunidade" }).click();
    await page.getByLabel("Título *").fill(newTitle);
    await page.getByLabel("Valor (R$) *").fill("1200");
    await page.getByLabel("Contato (ID) *").fill(contact.id);
    await page.getByRole("button", { name: "Criar Oportunidade" }).click();

    await expect(page.getByText("Oportunidade criada com sucesso")).toBeVisible();
    await expect(page.getByText(newTitle)).toBeVisible({ timeout: 15_000 });
  });

  test("move a oportunidade do seed para a etapa seguinte (toast + subtítulo)", async () => {
    await page.goto("/pipeline");
    const card = page.locator("div.shadow-sm").filter({ hasText: seedOppTitle });
    await expect(card).toBeVisible();

    await card.getByRole("button", { name: "Avançar" }).click();

    await expect(page.getByText("Oportunidade movida")).toBeVisible();
    // 2ª etapa padrão: Qualificação · 20% (verified: DEFAULT_STAGES).
    await expect(card.getByText("Qualificação · 20%")).toBeVisible({ timeout: 15_000 });
  });
});