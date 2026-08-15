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

  it("upload envia FormData e NÃO fixa Content-Type manual (boundary preservado)", async () => {
    postMock.mockResolvedValue({ data: { id: "x" } });
    const file = new File(["conteudo"], "a.txt", { type: "text/plain" });

    await StorageService.upload(companyId, file);

    expect(postMock).toHaveBeenCalledTimes(1);
    const [url, body, config] = postMock.mock.calls[0];
    expect(url).toBe(`/companies/${companyId}/storage/upload`);
    expect(body).toBeInstanceOf(FormData);
    expect((body as FormData).get("file")).toBe(file);
    // Content-Type deve ser undefined para o browser montar o boundary do multipart.
    expect(config.headers["Content-Type"]).toBeUndefined();
  });

  it("download usa responseType blob e o endpoint correto", async () => {
    getMock.mockResolvedValue({ data: new Blob(["x"]) });
    const blob = await StorageService.download(companyId, "obj-1");
    expect(getMock).toHaveBeenCalledWith(`/companies/${companyId}/storage/obj-1`, {
      responseType: "blob",
    });
    expect(blob).toBeInstanceOf(Blob);
  });

  it("remove chama DELETE /companies/{id}/storage/{objectId}", async () => {
    deleteMock.mockResolvedValue({});
    await StorageService.remove(companyId, "obj-1");
    expect(deleteMock).toHaveBeenCalledWith(`/companies/${companyId}/storage/obj-1`);
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
