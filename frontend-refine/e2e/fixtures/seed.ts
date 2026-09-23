import { type Page } from "@playwright/test";

/**
 * Fixture de dados E2E (Task 4.3) — cria e limpa dados de negócio via API.
 *
 * Como autentica: o frontend usa um BFF relay (rewrite de /api/** no
 * next.config.js para DEV_GATEWAY_TARGET). `page.request` compartilha os
 * cookies do browser context (incluindo `crm_session`), então as chamadas aqui
 * passam pelo MESMO caminho que o app — sem precisar de storageState ou token.
 *
 * Empresa alvo: é a empresa DEFAULT do usuário E2E (e2e/seed/user.sql), com o
 * usuário Admin E2E (roles AGENT+ADMIN, `role:manage` etc).
 *
 * Idempotência: cada execução usa um `runId` único (emails/nomes com sufixo),
 * e a limpeza é feita em `afterAll`; para resíduos de execuções falhas, os
 * helpers `purgeE2EPipelines`/`purgeE2ERoles` removem por prefixo "E2E".
 */
export const E2E_COMPANY_ID = "00000000-0000-0000-0000-000000000001";

const API_ROOT = "http://localhost:3000/api/v1";
const COMPANY = `/companies/${E2E_COMPANY_ID}`;

export function newRunId(): string {
  return `r${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
}

export function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export interface SeedContact {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
}

export interface SeedLead {
  id: string;
  contactId: string;
  status: string;
}

export interface SeedStage {
  id: string;
  name: string;
  probability: number;
  order: number;
}

export interface SeedPipeline {
  id: string;
  name: string;
  stages: SeedStage[];
}

export interface SeedRole {
  id: string;
  name: string;
  isSystem: boolean;
}

export interface SeedContext {
  contacts: SeedContact[];
  pipeline: SeedPipeline | null;
}

async function api<T>(
  page: Page,
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE",
  resourcePath: string,
  body?: unknown,
): Promise<T> {
  const response = await page.request.fetch(`${API_ROOT}${resourcePath}`, {
    method,
    data: body === undefined ? undefined : JSON.stringify(body),
    headers: { "Content-Type": "application/json" },
  });
  const text = await response.text();
  if (!response.ok()) {
    throw new Error(`seed ${method} ${resourcePath} -> ${response.status()}: ${text}`);
  }
  return (text ? JSON.parse(text) : undefined) as T;
}

// ===========================================================================
// Seeds
// ===========================================================================

export async function seedContacts(
  page: Page,
  runId: string,
  count = 5,
  tag = "",
): Promise<SeedContact[]> {
  const label = tag ? `${runId}-${tag}` : runId;
  const contacts: SeedContact[] = [];
  for (let i = 1; i <= count; i++) {
    contacts.push(
      await api<SeedContact>(page, "POST", `${COMPANY}/contacts`, {
        firstName: "E2E",
        lastName: `Contato ${label} ${i}`,
        email: `e2e.${label}.c${i}@crm.local`,
        phone: `5511${String(900_000_000 + i)}`,
        notes: `Seed E2E ${label}`,
      }),
    );
  }
  return contacts;
}

const LEAD_SEED_STATUSES = ["NEW", "CONTACTED", "QUALIFIED"] as const;

export async function seedLeads(
  page: Page,
  runId: string,
  contacts: SeedContact[],
): Promise<SeedLead[]> {
  const leads: SeedLead[] = [];
  for (let i = 0; i < Math.min(contacts.length, LEAD_SEED_STATUSES.length); i++) {
    leads.push(
      await api<SeedLead>(page, "POST", `${COMPANY}/leads`, {
        contactId: contacts[i].id,
        status: LEAD_SEED_STATUSES[i],
        source: "MANUAL",
        score: 35 + i * 5,
      }),
    );
  }
  return leads;
}

export async function seedPipeline(page: Page, runId: string): Promise<SeedPipeline> {
  return api<SeedPipeline>(page, "POST", `${COMPANY}/pipelines`, {
    name: `E2E Pipeline ${runId}`,
    description: "Seed E2E",
  });
}

export async function seedOpportunity(
  page: Page,
  pipeline: Pick<SeedPipeline, "id">,
  contactId: string,
  title: string,
  value: number,
): Promise<void> {
  await api(page, "POST", `${COMPANY}/pipelines/${pipeline.id}/opportunities`, {
    title,
    value,
    contactId,
  });
}

// ===========================================================================
// Cleanup
// ===========================================================================

export async function cleanupLeadsByContacts(page: Page, contactIds: string[]): Promise<void> {
  const result = await api<{ content: { id: string; contactId: string }[] }>(
    page,
    "GET",
    `${COMPANY}/leads?page=0&pageSize=200`,
  );
  const ours = (result.content ?? []).filter((lead) => contactIds.includes(lead.contactId));
  for (const lead of ours) {
    await api(page, "DELETE", `${COMPANY}/leads/${lead.id}`);
  }
}

export async function cleanupBusiness(page: Page, ctx: SeedContext): Promise<void> {
  if (ctx.pipeline) {
    // Deleção do pipeline remove em cascata estágios/board/oportunidades (V017).
    await api(page, "DELETE", `${COMPANY}/pipelines/${ctx.pipeline.id}`);
  }
  await cleanupLeadsByContacts(page, ctx.contacts.map((c) => c.id));
  for (const contact of ctx.contacts) {
    // DELETE de contato é soft-delete (deleted_at); leads explicitamente antes.
    await api(page, "DELETE", `${COMPANY}/contacts/${contact.id}`);
  }
}

export async function purgeE2EPipelines(page: Page): Promise<void> {
  const pipelines = await api<SeedPipeline[]>(page, "GET", `${COMPANY}/pipelines`);
  for (const pipeline of pipelines.filter((p) => p.name.startsWith("E2E Pipeline"))) {
    await api(page, "DELETE", `${COMPANY}/pipelines/${pipeline.id}`);
  }
}

export async function listRoles(page: Page): Promise<SeedRole[]> {
  return api<SeedRole[]>(page, "GET", "/roles");
}

export async function deleteRole(page: Page, roleId: string): Promise<void> {
  await api(page, "DELETE", `/roles/${roleId}`);
}

export async function purgeE2ERoles(page: Page): Promise<void> {
  const roles = await listRoles(page);
  for (const role of roles.filter((r) => r.name.startsWith("E2E_"))) {
    await deleteRole(page, role.id);
  }
}