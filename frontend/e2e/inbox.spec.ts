import { test, expect, type BrowserContext, type Page } from "@playwright/test";
import { login } from "./fixtures/auth";

/**
 * Task 4.3 — Inbox (conversas do seed e2e/seed/inbox.sql).
 *
 * O seed SQL é aplicado pelo job `e2e` (após o healthcheck do backend) e cria
 * 2 conversas do provedor FAKE na empresa default:
 *   - 5511999990001: 2 mensagens INBOUND, unread_count = 2;
 *   - 5511999990002: 1 mensagem INBOUND, unread_count = 0.
 *
 * O envio usa FakeProvider (POST /conversations/{id}/messages) que responde na
 * hora com SENT, então a spec valida que a mensagem aparece no thread e o badge
 * de não-lidas zera ("Em dia") ao abrir a conversa.
 */
test.describe("Inbox — conversas do seed (Task 4.3)", () => {
  test.describe.configure({ timeout: 120_000 });

  const CONV1_PHONE = "5511999990001";
  const CONV2_PHONE = "5511999990002";

  let context: BrowserContext;
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext({ baseURL: "http://localhost:3000" });
    page = await context.newPage();
    await login(page);
  });

  test.afterAll(async () => {
    await context.close();
  });

  // Botão da conversa no ConversationList (Button com externalPhone).
  function conversation(phone: string) {
    return page.getByRole("button").filter({ hasText: phone });
  }

  test("lista as conversas do seed (telefone + contador de não-lidas)", async () => {
    await page.goto("/inbox");
    await expect(page.getByRole("heading", { name: "Inbox" })).toBeVisible();

    const conv1 = conversation(CONV1_PHONE);
    await expect(conv1).toBeVisible();
    await expect(conv1.getByText("2")).toBeVisible();

    await expect(conversation(CONV2_PHONE)).toBeVisible();
  });

  test("abre a conversa e carrega o thread (mensagens do seed)", async () => {
    await page.goto("/inbox");
    await conversation(CONV1_PHONE).click();

    // Mensagem mais antiga só existe no thread (a lista mostra a última como preview).
    await expect(
      page.getByText("Olá! Quero saber mais sobre o plano."),
    ).toBeVisible({ timeout: 15_000 });

    // Ao abrir, unread_count zera => thread exibe "Em dia".
    await expect(page.getByText("Em dia")).toBeVisible({ timeout: 15_000 });
  });

  test("envia mensagem e replica no thread (FakeProvider)", async () => {
    await page.goto("/inbox");
    await conversation(CONV1_PHONE).click();

    const body = `E2E mensagem ${Date.now()}`;
    const input = page.getByPlaceholder(/Digite sua mensagem/);
    await input.fill(body);
    await input.press("Enter");

    await expect(page.getByText(body)).toBeVisible({ timeout: 15_000 });
  });
});