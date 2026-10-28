import { describe, it, expect, vi } from "vitest";
import { render, screen, within, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PatientAppointments } from "@/features/clinical/patients/components/PatientAppointments";
import type { Appointment } from "@/features/sales/scheduling/types/scheduling.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const { appointmentsState, typesState, membersState, changeStatusMock } = vi.hoisted(() => ({
  appointmentsState: { value: [] as Appointment[] },
  typesState: {
    value: [] as { id: string; name: string; durationMinutes: number }[],
  },
  membersState: { value: [] as { userId: string; name: string }[] },
  changeStatusMock: vi.fn(),
}));

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "u1", companyId: "co1" } }),
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: () => true }),
}));

vi.mock("@/features/clinical/patients/hooks/usePatient", () => ({
  useContactAppointments: () => ({ data: appointmentsState.value, isLoading: false }),
}));

vi.mock("@/features/identity/members/hooks/useMembers", () => ({
  useMembers: () => ({ data: membersState.value }),
}));

vi.mock("@/features/sales/scheduling/hooks/useScheduling", () => ({
  useAppointmentTypes: () => ({ data: typesState.value }),
  useAgendaSettings: () => ({ data: undefined }),
  useChangeAppointmentStatus: () => ({ mutate: changeStatusMock, isPending: false }),
  useRescheduleAppointment: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUpdateAppointment: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeleteAppointment: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("@/features/sales/scheduling/hooks/useSubmitNewAppointment", () => ({
  useSubmitNewAppointment: () => ({ submit: vi.fn(), isPending: false }),
}));

vi.mock("@/features/sales/chair/hooks/useChairs", () => ({
  useChairs: () => ({ data: [] }),
}));

vi.mock("@/features/sales/tag/hooks/useTags", () => ({
  useTags: () => ({ data: [] }),
}));

vi.mock("@/features/sales/scheduling/components/CreateAppointmentDialog", () => ({
  CreateAppointmentDialog: () => null,
}));

const contact = {
  id: "c1",
  firstName: "Ana",
  lastName: "Silva",
} as Contact;

const now = Date.now();
const future = new Date(now + 86_400_000).toISOString();
const past = new Date(now - 86_400_000).toISOString();

function makeAppointment(overrides: Partial<Appointment>): Appointment {
  return {
    id: "a1",
    companyId: "co1",
    appointmentTypeId: null,
    hostId: "u1",
    contactId: "c1",
    opportunityId: null,
    title: "Consulta",
    startAt: future,
    endAt: future,
    status: "SCHEDULED",
    source: "INTERNAL",
    locationKind: null,
    locationDetail: null,
    meetingUrl: null,
    notes: null,
    cancelReason: null,
    publicToken: null,
    googleEventId: null,
    createdBy: "u1",
    createdAt: "",
    updatedAt: "",
    ...overrides,
  };
}

function renderComponent() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <PatientAppointments contact={contact} />
    </QueryClientProvider>,
  );
}

describe("PatientAppointments", () => {
  it("separa próximas consultas do histórico", () => {
    appointmentsState.value = [
      makeAppointment({ id: "future", appointmentTypeId: "t-future", status: "SCHEDULED" }),
      makeAppointment({
        id: "past",
        appointmentTypeId: "t-past",
        status: "COMPLETED",
        startAt: past,
        endAt: past,
      }),
    ];
    typesState.value = [
      { id: "t-future", name: "Avaliação", durationMinutes: 30 },
      { id: "t-past", name: "Retorno", durationMinutes: 30 },
    ];
    membersState.value = [{ userId: "u1", name: "Dra. Ana" }];

    renderComponent();

    const upcoming = screen.getByText("Próximas consultas").closest("section")!;
    const history = screen.getByText("Histórico de consultas").closest("section")!;

    expect(within(upcoming).getByText(/Avaliação/)).toBeTruthy();
    expect(within(history).getByText(/Retorno/)).toBeTruthy();
    expect(within(upcoming).queryByText(/Retorno/)).toBeNull();
  });

  it("mostra os botões de presença apenas em consultas passadas pendentes", () => {
    appointmentsState.value = [
      makeAppointment({ id: "future", status: "SCHEDULED" }),
      makeAppointment({ id: "past", status: "SCHEDULED", startAt: past, endAt: past }),
    ];
    typesState.value = [];
    membersState.value = [];

    renderComponent();

    expect(screen.getAllByText("Compareceu")).toHaveLength(1);
    expect(screen.getAllByText("Não compareceu")).toHaveLength(1);
  });

  it("chama a mutation de status com COMPLETED ao registrar presença", () => {
    appointmentsState.value = [
      makeAppointment({ id: "past", status: "SCHEDULED", startAt: past, endAt: past }),
    ];
    typesState.value = [];
    membersState.value = [];

    renderComponent();
    fireEvent.click(screen.getByText("Compareceu"));

    expect(changeStatusMock).toHaveBeenCalledWith(
      { id: "past", status: "COMPLETED" },
      expect.anything(),
    );
  });
});
