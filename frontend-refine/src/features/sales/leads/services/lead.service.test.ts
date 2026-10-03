import { describe, it, expect, vi, beforeEach } from "vitest";

const api = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));

vi.mock("@/lib/api", () => ({ default: api }));

import { LeadService } from "./lead.service";

const page = {
  content: [],
  page: 0,
  pageSize: 10,
  totalElements: 0,
  totalPages: 0,
};

describe("LeadService (Sprint 10)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("sends the search term as a query param when listing leads", async () => {
    api.get.mockResolvedValue({ data: page });

    await LeadService.list("company-1", { page: 0, pageSize: 10, search: "joão" });

    expect(api.get).toHaveBeenCalledWith("/companies/company-1/leads", {
      params: expect.objectContaining({ search: "joão" }),
    });
  });

  it("omits undefined params when listing leads", async () => {
    api.get.mockResolvedValue({ data: page });

    await LeadService.list("company-1", { page: 0, pageSize: 10 });

    expect(api.get).toHaveBeenCalledWith("/companies/company-1/leads", {
      params: expect.objectContaining({ page: 0, pageSize: 10 }),
    });
  });
});
