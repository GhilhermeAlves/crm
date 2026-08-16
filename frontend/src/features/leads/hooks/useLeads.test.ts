import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { useLeads, useCreateLead } from "./useLeads";

const { listMock, createMock } = vi.hoisted(() => ({
  listMock: vi.fn(),
  createMock: vi.fn(),
}));

vi.mock("../services/lead.service", () => ({
  LeadService: {
    list: listMock,
    create: createMock,
    findById: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}));

const page = {
  content: [
    {
      id: "lead-1",
      companyId: "company-1",
      contactId: "contact-1",
      status: "NEW",
      score: 50,
      classification: "WARM",
      source: "WHATSAPP",
      campaignId: null,
      assignedTo: null,
      notes: null,
      createdAt: "2026-08-13T00:00:00",
      updatedAt: "2026-08-13T00:00:00",
    },
  ],
  page: 0,
  pageSize: 10,
  totalElements: 1,
  totalPages: 1,
};

function makeClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

function renderHookWith<TResult>(render: () => TResult) {
  const client = makeClient();
  const wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client }, children);
  return renderHook<TResult, unknown>(render, { wrapper });
}

describe("useLeads (Sprint 10)", () => {
  beforeEach(() => {
    listMock.mockReset();
    createMock.mockReset();
  });

  it("fetches leads for the active company", async () => {
    listMock.mockResolvedValue(page);
    const { result } = renderHookWith(() => useLeads("company-1", { page: 0, pageSize: 10 }));

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(listMock).toHaveBeenCalledWith("company-1", expect.objectContaining({ page: 0 }));
    expect(result.current.data?.totalElements).toBe(1);
  });

  it("does not fetch when there is no active company", async () => {
    renderHookWith(() => useLeads(null));
    expect(listMock).not.toHaveBeenCalled();
  });

  it("create mutation posts to the active company", async () => {
    createMock.mockResolvedValue(page.content[0]);
    const { result } = renderHookWith(() => useCreateLead("company-1"));

    result.current.mutate({
      contactId: "contact-1",
      source: "WHATSAPP",
      status: "NEW",
      score: 50,
    });

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith(
        "company-1",
        expect.objectContaining({ source: "WHATSAPP" }),
      ),
    );
  });
});
