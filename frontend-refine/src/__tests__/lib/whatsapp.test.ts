import { describe, it, expect } from "vitest";
import { whatsappUrl } from "@/lib/whatsapp";

describe("whatsappUrl", () => {
  it("põe o DDI 55 quando o número veio sem ele", () => {
    expect(whatsappUrl("(34) 99999-8888")).toBe("https://wa.me/5534999998888");
  });

  it("mantém número que já tem DDI", () => {
    expect(whatsappUrl("+55 34 99999-8888")).toBe("https://wa.me/5534999998888");
  });

  it("sem número utilizável devolve null", () => {
    expect(whatsappUrl("123")).toBeNull();
    expect(whatsappUrl(null)).toBeNull();
    expect(whatsappUrl(undefined)).toBeNull();
  });

  it("anexa a mensagem codificada", () => {
    expect(whatsappUrl("34999998888", "Olá, tudo bem?")).toBe(
      "https://wa.me/5534999998888?text=Ol%C3%A1%2C%20tudo%20bem%3F",
    );
  });
});
