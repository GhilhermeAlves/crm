import { describe, it, expect } from "vitest";
import {
  buildInvitationMessage,
  buildInvitationWhatsAppUrl,
} from "@/features/identity/invitations/lib/invitation-share";

describe("invitation-share", () => {
  it("monta a mensagem com o nome da empresa", () => {
    const message = buildInvitationMessage("https://crm.app/convite/tok", "Clínica Vida");

    expect(message).toContain("Clínica Vida");
    expect(message).toContain("https://crm.app/convite/tok");
  });

  it("usa 'o CRM' como fallback quando não há nome da empresa", () => {
    expect(buildInvitationMessage("https://crm.app/convite/tok")).toContain("o CRM");
  });

  it("gera link wa.me com a mensagem codificada", () => {
    const url = buildInvitationWhatsAppUrl("https://crm.app/convite/tok", "Clínica Vida");

    expect(url.startsWith("https://wa.me/?text=")).toBe(true);
    expect(url).toContain(encodeURIComponent("Clínica Vida"));
    expect(url).toContain(encodeURIComponent("https://crm.app/convite/tok"));
  });

  it("resolve links relativos contra a origem da página", () => {
    const url = buildInvitationWhatsAppUrl("/convite/tok", "Clínica Vida");

    expect(decodeURIComponent(url)).toContain("http://localhost:3000/convite/tok");
  });
});
