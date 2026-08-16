import { describe, it, expect, vi, beforeEach } from "vitest";
import { StorageService } from "./storage.service";
import { formatBytes } from "../types/storage.types";

const { getMock, postMock, deleteMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
  deleteMock: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  default: { get: getMock, post: postMock, delete: deleteMock },
}));

const companyId = "11111111-2222-3333-4444-555555555555";

describe("StorageService (hotfix arquivos)", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
    deleteMock.mockReset();
  });

  it("list chama GET /companies/{id}/storage", async () => {
    getMock.mockResolvedValue({ data: [] });
    await StorageService.list(companyId);
    expect(getMock).toHaveBeenCalledWith(`/companies/${companyId}/storage`);
  });

  it("upload envia FormData real com a part 'file' e sem Content-Type fixo (browser monta o boundary)", async () => {
    postMock.mockResolvedValue({ data: { id: "x" } });
    const file = new File(["conteudo"], "a.txt", { type: "text/plain" });

    await StorageService.upload(companyId, file);

    expect(postMock).toHaveBeenCalledTimes(1);
    const [url, body, config] = postMock.mock.calls[0];
    expect(url).toBe(`/companies/${companyId}/storage/upload`);
    // FormData real é enviado e contém a part "file".
    expect(body).toBeInstanceOf(FormData);
    expect((body as FormData).get("file")).toBe(file);
    // O Content-Type é removido (null) para o browser gerar
    // multipart/form-data; boundary=... — nunca application/json nem
    // application/x-www-form-urlencoded.
    const contentType = config.headers["Content-Type"];
    expect(contentType).toBeNull();
    if (typeof contentType === "string") {
      expect(contentType).not.toContain("application/json");
      expect(contentType).not.toContain("application/x-www-form-urlencoded");
    }
  });

  it("download usa responseType blob e o endpoint correto", async () => {
    getMock.mockResolvedValue({ data: new Blob(["x"]) });
    const blob = await StorageService.download(companyId, "obj-1");
    expect(getMock).toHaveBeenCalledWith(
      `/companies/${companyId}/storage/obj-1`,
      {
        responseType: "blob",
      },
    );
    expect(blob).toBeInstanceOf(Blob);
  });

  it("remove chama DELETE /companies/{id}/storage/{objectId}", async () => {
    deleteMock.mockResolvedValue({});
    await StorageService.remove(companyId, "obj-1");
    expect(deleteMock).toHaveBeenCalledWith(
      `/companies/${companyId}/storage/obj-1`,
    );
  });
});

describe("formatBytes", () => {
  it("formata bytes, KB, MB e GB", () => {
    expect(formatBytes(512)).toBe("512 B");
    expect(formatBytes(2048)).toBe("2.0 KB");
    expect(formatBytes(5 * 1024 * 1024)).toBe("5.0 MB");
    expect(formatBytes(3 * 1024 * 1024 * 1024)).toBe("3.0 GB");
  });
});
