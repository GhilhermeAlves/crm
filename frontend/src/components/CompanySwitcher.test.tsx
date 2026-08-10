import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { CompanySwitcher } from "./CompanySwitcher";
import { AuthProvider } from "@/features/auth/hooks/useAuth";
import type { CompanyOption } from "@/features/auth/types/auth.types";

const { pathnameState } = vi.hoisted(() => ({
  pathnameState: { value: "/dashboard" },
}));
const { meMock, myCompaniesMock, switchCompanyMock } = vi.hoisted(() => ({
  meMock: vi.fn(),
  myCompaniesMock: vi.fn(),
  switchCompanyMock: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => pathnameState.value,
}));
vi.mock("@/features/auth/services/auth.service", () => ({
  AuthService: { me: meMock, myCompanies: myCompaniesMock, switchCompany: switchCompanyMock },
}));
vi.mock("@/lib/gateway-auth", () => ({
  loginWithGateway: vi.fn(),
  logoutWithGateway: vi.fn(),
}));

const COMPANY_A = "company-a";
const COMPANY_B = "company-b";

function mockUser(companyId: string | null) {
  return {
    id: "user-1",
    email: "ana@example.com",
    name: "Ana Silva",
    companyId,
    isActive: true,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  };
}

function companies(activeId: string): CompanyOption[] {
  return [
    { companyId: COMPANY_A, name: "Empresa A", logo: null, active: activeId === COMPANY_A },
    { companyId: COMPANY_B, name: "Empresa B", logo: null, active: activeId === COMPANY_B },
  ];
}

function renderSwitcher() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <AuthProvider>
        <CompanySwitcher />
      </AuthProvider>
    </QueryClientProvider>,
  );
}

async function optionButton(id: string) {
  return (await screen.findByTestId(`company-option-${id}`)) as HTMLButtonElement;
}

describe("CompanySwitcher (Sprint 8.4)", () => {
  beforeEach(() => {
    pathnameState.value = "/dashboard";
    meMock.mockReset();
    myCompaniesMock.mockReset();
    switchCompanyMock.mockReset();
    switchCompanyMock.mockResolvedValue({
      companyId: COMPANY_B,
      name: "Empresa B",
      logo: null,
      active: true,
    });
  });

  it("shows the active company with a check and the others as selectable", async () => {
    meMock.mockResolvedValue(mockUser(COMPANY_A));
    myCompaniesMock.mockResolvedValue(companies(COMPANY_A));

    renderSwitcher();

    await screen.findByText("Empresa A");
    expect(screen.getByText("Empresa B")).toBeTruthy();
    expect(screen.getByTestId("active-company-check")).toBeTruthy();

    const activeOption = await optionButton(COMPANY_A);
    const otherOption = await optionButton(COMPANY_B);
    expect(activeOption.disabled).toBe(true);
    expect(otherOption.disabled).toBe(false);
  });

  it("switches company without logout and moves the active mark (me invalidated)", async () => {
    // A -> B after switch: both `me` and the company list refetch during invalidate.
    meMock
      .mockResolvedValueOnce(mockUser(COMPANY_A))
      .mockResolvedValue(mockUser(COMPANY_B));
    myCompaniesMock
      .mockResolvedValueOnce(companies(COMPANY_A))
      .mockResolvedValue(companies(COMPANY_B));

    renderSwitcher();

    const optionB = await optionButton(COMPANY_B);
    fireEvent.click(optionB);

    await waitFor(() => expect(switchCompanyMock).toHaveBeenCalledWith(COMPANY_B));
    // After invalidation, /me returns company B -> B becomes the active/disabled option.
    await waitFor(async () => {
      const activeOption = await optionButton(COMPANY_B);
      expect(activeOption.disabled).toBe(true);
    });
  });

  it("renders nothing for a company-less user (onboarding)", async () => {
    meMock.mockResolvedValue(mockUser(null));

    renderSwitcher();

    await waitFor(() => expect(myCompaniesMock).not.toHaveBeenCalled());
    expect(screen.queryByText("Empresas")).toBeNull();
  });
});