import { describe, it, expect, vi, beforeEach } from "vitest";
import { InvitationService } from "@/features/identity/invitations/services/invitation.service";

const { getMock, postMock } = vi.hoisted(() => ({ getMock: vi.fn(), postMock: vi.fn() }));

vi.mock("@/lib/api", () => ({
  default: { get: getMock, post: postMock, delete: vi.fn() },
}));

describe("InvitationService (fase 1 do convite)", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
  });

  it("preview chama GET /invitations/preview com o token", async () => {
    getMock.mockResolvedValue({ data: { companyName: "EmpresaX" } });

    const result = await InvitationService.preview("tok-1");

    expect(getMock).toHaveBeenCalledWith("/invitations/preview", { params: { token: "tok-1" } });
    expect(result).toEqual({ companyName: "EmpresaX" });
  });

  it("register envia apenas token, nome e senha", async () => {
    postMock.mockResolvedValue({ data: { status: "ACCEPTED" } });

    await InvitationService.register({ token: "tok-1", name: "Fulano", password: "Kc!Valid1Aa1" });

    expect(postMock).toHaveBeenCalledTimes(1);
    const [url, body] = postMock.mock.calls[0];
    expect(url).toBe("/invitations/register");
    expect(body).toEqual({ token: "tok-1", name: "Fulano", password: "Kc!Valid1Aa1" });
  });

  it("register descarta campos extras (e-mail, empresa, perfil) mesmo se vierem no objeto", async () => {
    postMock.mockResolvedValue({ data: {} });
    const tampered = {
      token: "tok-1",
      name: "Fulano",
      password: "Kc!Valid1Aa1",
      email: "atacante@evil.com",
      companyId: "c-1",
      role: "ADMIN",
    };

    await InvitationService.register(tampered);

    const body = postMock.mock.calls[0][1];
    expect(Object.keys(body).sort()).toEqual(["name", "password", "token"]);
  });
});
