import { describe, expect, it } from "vitest";
import {
  invitationStatusMeta,
  isUsableInvitation,
} from "@/features/identity/invitations/lib/invitation-status";

describe("invitation-status (status efetivo)", () => {
  const cases = [
    ["PENDING", "Pendente", "secondary"],
    ["ACCEPTED", "Aceito", "default"],
    ["REVOKED", "Revogado", "destructive"],
    ["EXPIRED", "Expirado", "outline"],
  ] as const;

  it.each(cases)("rótulo de %s → %s", (status, label, variant) => {
    expect(invitationStatusMeta(status)).toEqual({ label, variant });
  });

  it("cai para o próprio status quando desconhecido", () => {
    const meta = invitationStatusMeta("UNKNOWN" as never);
    expect(meta).toEqual({ label: "UNKNOWN", variant: "secondary" });
  });

  it("permite regenerar (reenviar/copiar link) apenas em PENDING e EXPIRED", () => {
    expect(isUsableInvitation("PENDING")).toBe(true);
    expect(isUsableInvitation("EXPIRED")).toBe(true);
    expect(isUsableInvitation("REVOKED")).toBe(false);
    expect(isUsableInvitation("ACCEPTED")).toBe(false);
  });
});
