import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ContactTable } from "@/features/masterdata/contacts/components/ContactTable";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}));

function makeContact(overrides: Partial<Contact> = {}): Contact {
  return {
    id: "contact-1",
    companyId: "company-1",
    firstName: "Ana",
    lastName: "Souza",
    email: "ana@e.com",
    phone: null,
    mobile: null,
    notes: null,
    birthDate: null,
    cpf: null,
    rg: null,
    rgIssuer: null,
    gender: null,
    maritalStatus: null,
    professionalStatus: null,
    zipCode: null,
    street: null,
    addressNumber: null,
    addressComplement: null,
    neighborhood: null,
    city: null,
    state: null,
    healthPlan: null,
    healthPlanNumber: null,
    recordNumber: null,
    referralSource: null,
    hasGuardian: false,
    guardianName: null,
    guardianCpf: null,
    guardianPhone: null,
    createdAt: "2026-08-13T00:00:00",
    ...overrides,
  };
}

describe("ContactTable (Sprint 13)", () => {
  it("renders an empty state when there are no contacts", () => {
    render(<ContactTable contacts={[]} />);
    expect(screen.getByText("Nenhum contato cadastrado ainda.")).toBeTruthy();
  });

  it("renders contact rows with name, cpf, mobile and record number", () => {
    render(
      <ContactTable
        contacts={[
          makeContact({ cpf: "11122233344", mobile: "11988887777", recordNumber: "PRT-42" }),
        ]}
      />,
    );
    expect(screen.getByText(/Ana/)).toBeTruthy();
    expect(screen.getByText("111.222.333-44")).toBeTruthy();
    expect(screen.getByText("(11) 98888-7777")).toBeTruthy();
    expect(screen.getByText("PRT-42")).toBeTruthy();
  });

  it("links each row to the customer 360 page", () => {
    render(<ContactTable contacts={[makeContact()]} />);
    const link = screen.getByRole("link");
    expect(link.getAttribute("href")).toBe("/contacts/contact-1");
  });
});
