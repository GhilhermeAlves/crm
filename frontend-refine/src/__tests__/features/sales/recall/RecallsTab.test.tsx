import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RecallsTab } from "@/features/sales/recall/components/RecallsTab";
import { useRecalls } from "@/features/sales/recall/hooks/useRecalls";

function renderTab() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <RecallsTab />
    </QueryClientProvider>,
  );
}

if (typeof Element !== "undefined") {
  Element.prototype.hasPointerCapture ??= () => false;
  Element.prototype.setPointerCapture ??= () => {};
  Element.prototype.releasePointerCapture ??= () => {};
  Element.prototype.scrollIntoView ??= () => {};
}

const h = vi.hoisted(() => ({
  recallMutate: vi.fn(),
  dismissSuggestionMutate: vi.fn(),
  createAppointmentMutate: vi.fn(),
  createRecall: {
    id: "recall-1",
    companyId: "company-1",
    contactId: "contact-1",
    phone: "11999998888",
    hostId: "host-1",
    tag: "Limpeza",
    dueDate: "2099-12-31",
    status: "PENDING" as const,
    appointmentId: null,
    createdBy: null,
    createdAt: "2026-10-01T00:00:00Z",
    updatedAt: "2026-10-01T00:00:00Z",
  },
  suggestion: {
    contactId: "contact-2",
    contactName: "João Pereira",
    mobile: "11988887777",
    phone: null,
    hostId: "host-1",
    lastAppointmentAt: "2026-07-10T10:00:00Z",
    recentProcedures: ["Restauração", "Profilaxia"],
  },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({
    user: { id: "user-1", companyId: "company-1" },
    permissions: [],
    roles: [],
  }),
}));

vi.mock("@/features/identity/members/hooks/useMembers", () => ({
  useMembers: () => ({
    data: [{ userId: "host-1", name: "Dra. Ana" }],
    isLoading: false,
  }),
}));

vi.mock("@/features/masterdata/contacts/hooks/useContacts", () => ({
  useContacts: () => ({
    data: [
      {
        id: "contact-1",
        firstName: "Maria",
        lastName: "Silva",
        mobile: "11999998888",
        phone: null,
      },
    ],
    isLoading: false,
  }),
}));

vi.mock("@/features/sales/scheduling/hooks/useScheduling", () => ({
  useAppointmentTypes: () => ({ data: [], isLoading: false }),
  useCreateAppointment: () => ({ mutate: h.createAppointmentMutate, isPending: false }),
}));

vi.mock("@/features/sales/scheduling/components/CreateAppointmentDialog", () => ({
  CreateAppointmentDialog: () => <div data-testid="create-appointment-dialog" />,
}));

vi.mock("@/features/sales/recall/hooks/useRecalls", () => ({
  useRecalls: vi.fn(() => ({
    data: {
      content: [h.createRecall],
      page: 0,
      pageSize: 100,
      totalElements: 1,
      totalPages: 1,
    },
    isLoading: false,
  })),
  useRecallSuggestions: vi.fn(() => ({ data: [h.suggestion], isLoading: false })),
  useCreateRecall: () => ({ mutate: h.recallMutate, isPending: false }),
  useDismissRecall: () => ({ mutate: h.recallMutate, isPending: false }),
  useScheduleRecall: () => ({ mutate: h.recallMutate, isPending: false }),
  useDismissRecallSuggestion: vi.fn(() => ({
    mutate: h.dismissSuggestionMutate,
    isPending: false,
  })),
}));

describe("RecallsTab (Fase B — Retornos)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders both the Programados and Sugeridos sections", () => {
    renderTab();

    expect(screen.getByText("Retornos")).toBeTruthy();
    expect(screen.getByText("Programados")).toBeTruthy();
    expect(screen.getByText("Sugeridos")).toBeTruthy();

    expect(screen.getByText("Maria Silva")).toBeTruthy();
    expect(screen.getByText("Limpeza")).toBeTruthy();
    expect(screen.getByText("João Pereira")).toBeTruthy();
    expect(screen.getAllByText("Dra. Ana").length).toBeGreaterThan(0);
  });

  it("changes the window filter and reloads recalls with the new window", async () => {
    renderTab();

    expect(useRecalls).toHaveBeenCalledWith(
      "company-1",
      expect.objectContaining({ window: "NEXT_7_DAYS" }),
    );

    fireEvent.click(screen.getByLabelText("Janela de vencimento"));
    fireEvent.click(await screen.findByRole("option", { name: "Vencidos" }));

    await waitFor(() => {
      expect(useRecalls).toHaveBeenCalledWith(
        "company-1",
        expect.objectContaining({ window: "OVERDUE" }),
      );
    });
  });

  it("asks for confirmation before discarding a suggestion", async () => {
    renderTab();

    fireEvent.pointerDown(screen.getByLabelText("Ações da sugestão"));
    fireEvent.click(await screen.findByText("Descartar sugestão"));

    expect(
      await screen.findByText(
        /Tem certeza que deseja descartar a sugestão de retorno para João Pereira/,
      ),
    ).toBeTruthy();
    expect(h.dismissSuggestionMutate).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Descartar" }));
    expect(h.dismissSuggestionMutate).toHaveBeenCalledWith("contact-2", expect.anything());
  });

  it("builds the WhatsApp link from the suggestion phone number", () => {
    renderTab();

    const link = screen.getByRole("link", { name: "Conversar no WhatsApp" });
    expect(link.getAttribute("href")).toBe("https://wa.me/5511988887777");
    expect(link.getAttribute("target")).toBe("_blank");
  });
});
