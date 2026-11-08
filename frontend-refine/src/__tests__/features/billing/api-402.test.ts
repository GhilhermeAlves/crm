import { afterEach, describe, expect, it, vi } from "vitest";
import api, { isBillingBlocked } from "@/lib/api";

describe("402 BILLING_BLOCKED", () => {
  const assign = vi.fn();

  afterEach(() => {
    assign.mockReset();
    vi.unstubAllGlobals();
  });

  function stubLocation(pathname: string) {
    vi.stubGlobal("location", { ...window.location, pathname, assign });
  }

  function blocked() {
    return { response: { status: 402, data: { code: "BILLING_BLOCKED" } }, config: {} };
  }

  // O interceptador de resposta registrado em lib/api.ts.
  const reject = (error: unknown) =>
    (
      api.interceptors.response as unknown as {
        handlers: { rejected: (e: unknown) => Promise<unknown> }[];
      }
    ).handlers[0].rejected(error);

  it("reconhece o erro de bloqueio", () => {
    expect(isBillingBlocked(blocked())).toBe(true);
    expect(isBillingBlocked({ response: { status: 402, data: {} } })).toBe(false);
    expect(isBillingBlocked({ response: { status: 403, data: { code: "BILLING_BLOCKED" } } })).toBe(
      false,
    );
  });

  it("manda para /pagamento-pendente", async () => {
    stubLocation("/crm");
    await expect(reject(blocked())).rejects.toBeTruthy();
    expect(assign).toHaveBeenCalledWith("/pagamento-pendente");
  });

  it("não redireciona se já está na página", async () => {
    stubLocation("/pagamento-pendente");
    await expect(reject(blocked())).rejects.toBeTruthy();
    expect(assign).not.toHaveBeenCalled();
  });
});
