import { describe, expect, it } from "vitest";
import { resolveInviteUrl } from "@/features/identity/invitations/lib/invite-url";

describe("invite-url (resolução de URL devolvida pelo backend)", () => {
  it("mantém URL absoluta com mais de um segmento de path", () => {
    expect(resolveInviteUrl("https://app.exemplo.com/convidar/abc")).toBe(
      "https://app.exemplo.com/convidar/abc",
    );
  });

  it("prefixa a origem quando a URL é relativa (base-url não configurada)", () => {
    expect(resolveInviteUrl("/convite/tok123", "https://app.exemplo.com")).toBe(
      "https://app.exemplo.com/convite/tok123",
    );
  });

  it("remove barra final do origin antes de concatenar", () => {
    expect(resolveInviteUrl("/convite/x", "https://app.exemplo.com/")).toBe(
      "https://app.exemplo.com/convite/x",
    );
  });

  it("usa window.location.origin automaticamente quando origin não é passado", () => {
    const original = window.location.origin;
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, origin: "https://crm.exemplo.com" },
    });
    try {
      expect(resolveInviteUrl("/convite/y")).toBe("https://crm.exemplo.com/convite/y");
    } finally {
      Object.defineProperty(window, "location", {
        configurable: true,
        value: { ...window.location, origin: original },
      });
    }
  });

  it("não altera valores vazios ou não-iniciados por barra", () => {
    expect(resolveInviteUrl("")).toBe("");
    expect(resolveInviteUrl("convite-sem-barra")).toBe("convite-sem-barra");
  });
});
