import { defineConfig, devices } from "@playwright/test";

/**
 * Config de testes E2E (Playwright) do frontend Next.js.
 *
 * Escopo de orquestração:
 * - O Playwright GERENCIA SOMENTE o frontend (webServer abaixo: build + next
 *   start na porta 3000).
 * - A stack de apoio (postgres, redis, rabbitmq, keycloak, backend,
 *   auth-service/gateway) NÃO é subida pelo webServer. Ela é orquestrada pelo
 *   job `e2e` do GitHub Actions (ubuntu-latest, que tem Docker) em
 *   `.github/workflows/ci.yml`, usando exatamente as mesmas imagens/versões dos
 *   compose do repositório.
 * - Para o frontend alcançar o gateway local (sem nginx), o job de CI roda o
 *   build/start com `DEV_GATEWAY_TARGET=http://localhost:8082` (rewrites de
 *   /auth/** e /api/** do next.config.js apontando para o auth-service).
 * - Localmente, sem a stack, apenas o smoke de tooling roda (rotas públicas):
 *   `npm run e2e` (webServer sobe o frontend; `reuseExistingServer` permite
 *   apontar para um `next dev`/`next start` já em execução).
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  // JPA/Keycloak e o contrato VITE_MODE: um usuário por vez evita corrida de
  // OTP/2FA e races entre specs que compartilham sessões/seed.
  workers: 1,
  // Retry apenas em CI (2x); local = 0 para feedback rápido.
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [["html", { open: "never" }]] : "list",
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: {
    // Start do frontend gerenciado pelo Playwright. Em CI o job `e2e` já sobe
    // a stack completa antes deste comando; aqui apenas o app Next é servido.
    command: "npm run build && npm run start",
    url: "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});