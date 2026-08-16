import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ContactTable } from "./ContactTable";
import type { Contact } from "../types/contact.types";

vi.mock("next/link", () => ({
  default: ({
    href,
    children,
  }: {
    href: string;
    children: React.ReactNode;
  }) => <a href={href}>{children}</a>,
}));

function makeContact(overrides: Partial<Contact> = {}): Contact {
  return {
    id: "contact-1",
    companyId: "company-1",
    firstName: "Ana",
    lastName: "Souza",
    email: "ana@e.com",
    phone: null,
    notes: null,
    createdAt: "2026-08-13T00:00:00",
    ...overrides,
  };
}

describe("ContactTable (Sprint 13)", () => {
  it("renders an empty state when there are no contacts", () => {
    render(<ContactTable contacts={[]} />);
    expect(screen.getByText("Nenhum contato cadastrado ainda.")).toBeTruthy();
  });

  it("renders contact rows with name and email", () => {
    render(<ContactTable contacts={[makeContact()]} />);
    expect(screen.getByText(/Ana/)).toBeTruthy();
    expect(screen.getByText(/ana@e.com/)).toBeTruthy();
  });

  it("links each row to the customer 360 page", () => {
    render(<ContactTable contacts={[makeContact()]} />);
    const link = screen.getByRole("link");
    expect(link.getAttribute("href")).toBe("/contacts/contact-1");
  });
});
