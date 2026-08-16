import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { useContacts, useCustomer360, useCreateContact } from "./useContacts";

const { listMock, c360Mock, createMock } = vi.hoisted(() => ({
  listMock: vi.fn(),
  c360Mock: vi.fn(),
  createMock: vi.fn(),
}));

vi.mock("../services/contact.service", () => ({
  ContactService: {
    list: listMock,
    findById: vi.fn(),
    customer360: c360Mock,
    create: createMock,
    update: vi.fn(),
    delete: vi.fn(),
  },
}));

const contact = {
  id: "contact-1",
  companyId: "company-1",
  firstName: "Ana",
  lastName: "Souza",
  email: "ana@e.com",
  phone: "11-99999",
  notes: null,
  createdAt: "2026-08-13T00:00:00",
};

const customer360 = {
  companyId: "company-1",
  contact: {
    id: "contact-1",
    fullName: "Ana Souza",
    email: "ana@e.com",
    phone: null,
    notes: null,
    initials: "AS",
    createdAt: "2026-08-13T00:00:00",
    lastInteractionAt: "2026-08-13T00:00:00",
    atRisk: false,
    riskMessage: null,
  },
  openOpportunities: 1,
  openValue: 5000,
  opportunities: [
    {
      id: "opp-1",
      title: "Negócio A",
      value: 5000,
      stageName: "Proposta",
      probability: 60,
      status: "OPEN",
      statusLabel: "Aberta",
      pipelineName: "Vendas",
      assignedTo: null,
      expectedCloseDate: null,
    },
  ],
  tasks: [],
  timeline: [],
  nextAction: {
    type: "NONE",
    title: "Tudo em dia",
    description: "",
    priority: 0,
  },
};

function renderWith<T>(render: () => T) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client }, children);
  return renderHook(render, { wrapper });
}

describe("useContacts (Sprint 13)", () => {
  beforeEach(() => {
    listMock.mockReset();
    c360Mock.mockReset();
    createMock.mockReset();
  });

  it("fetches the contact directory for the active company", async () => {
    listMock.mockResolvedValue([contact]);
    const { result } = renderWith(() => useContacts("company-1"));

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
  });

  it("loads the customer 360 for a contact", async () => {
    c360Mock.mockResolvedValue(customer360);
    const { result } = renderWith(() =>
      useCustomer360("company-1", "contact-1"),
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.contact.fullName).toBe("Ana Souza");
    expect(c360Mock).toHaveBeenCalledWith("company-1", "contact-1");
  });

  it("does not fetch the 360 without a valid contact id", () => {
    renderWith(() => useCustomer360("company-1", null));
    expect(c360Mock).not.toHaveBeenCalled();
  });

  it("create contact posts to the active company", async () => {
    createMock.mockResolvedValue(contact);
    const { result } = renderWith(() => useCreateContact("company-1"));

    result.current.mutate({
      firstName: "Ana",
      lastName: "Souza",
      email: "ana@e.com",
    });
    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith(
        "company-1",
        expect.objectContaining({ firstName: "Ana" }),
      ),
    );
  });
});
