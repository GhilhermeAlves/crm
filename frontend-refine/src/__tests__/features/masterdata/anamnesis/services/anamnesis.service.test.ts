import { describe, it, expect, vi, beforeEach } from "vitest";
import { AnamnesisService } from "@/features/masterdata/anamnesis/services/anamnesis.service";

const { getMock, postMock, putMock, deleteMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
  putMock: vi.fn(),
  deleteMock: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  default: { get: getMock, post: postMock, put: putMock, delete: deleteMock },
}));

const COMPANY = "c-1";
const base = `/companies/${COMPANY}/anamnesis-models`;

describe("AnamnesisService", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
    putMock.mockReset();
    deleteMock.mockReset();
  });

  it("list busca os modelos da empresa", async () => {
    getMock.mockResolvedValue({ data: [] });

    const result = await AnamnesisService.list(COMPANY);

    expect(getMock).toHaveBeenCalledWith(base);
    expect(result).toEqual([]);
  });

  it("get busca um modelo específico", async () => {
    getMock.mockResolvedValue({ data: { id: "m-1" } });

    const result = await AnamnesisService.get(COMPANY, "m-1");

    expect(getMock).toHaveBeenCalledWith(`${base}/m-1`);
    expect(result).toEqual({ id: "m-1" });
  });

  it("create envia nome e descrição", async () => {
    postMock.mockResolvedValue({ data: { id: "m-1", name: "Novo" } });

    await AnamnesisService.create(COMPANY, { name: "Novo", description: "desc" });

    expect(postMock).toHaveBeenCalledWith(base, { name: "Novo", description: "desc" });
  });

  it("update faz PUT com a estrutura", async () => {
    putMock.mockResolvedValue({ data: { id: "m-1" } });
    const payload = { name: "M", sections: [] } as never;

    await AnamnesisService.update(COMPANY, "m-1", payload);

    expect(putMock).toHaveBeenCalledWith(`${base}/m-1`, payload);
  });

  it("setActive usa activate/deactivate conforme o estado", async () => {
    postMock.mockResolvedValue({ data: {} });

    await AnamnesisService.setActive(COMPANY, "m-1", true);
    expect(postMock).toHaveBeenCalledWith(`${base}/m-1/activate`);

    await AnamnesisService.setActive(COMPANY, "m-1", false);
    expect(postMock).toHaveBeenCalledWith(`${base}/m-1/deactivate`);
  });

  it("remove faz DELETE do modelo", async () => {
    deleteMock.mockResolvedValue({});

    await AnamnesisService.remove(COMPANY, "m-1");

    expect(deleteMock).toHaveBeenCalledWith(`${base}/m-1`);
  });
});
